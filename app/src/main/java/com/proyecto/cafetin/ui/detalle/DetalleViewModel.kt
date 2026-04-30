package com.proyecto.cafetin.ui.detalle

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.proyecto.cafetin.CafetinApp
import com.proyecto.cafetin.data.model.CategoriaConProductos
import com.proyecto.cafetin.data.model.Movimiento
import com.proyecto.cafetin.data.model.Persona
import com.proyecto.cafetin.domain.usecase.AcumularProductoUseCase
import com.proyecto.cafetin.domain.usecase.ExportarPdfUseCase
import com.proyecto.cafetin.repository.ICafetinRepository
import com.proyecto.cafetin.util.DateUtils.desdeDatePicker
import com.proyecto.cafetin.util.DateUtils.finDeDia
import com.proyecto.cafetin.util.DateUtils.finDeDiaHoy
import com.proyecto.cafetin.util.DateUtils.inicioDeDiaHoy
import com.proyecto.cafetin.util.MoneyUtils.centavosAtexto
import com.proyecto.cafetin.util.NotaUtils.cantidadDeNota
import com.proyecto.cafetin.util.NotaUtils.notaBase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DetalleViewModel(
    private val repository: ICafetinRepository,
    val personaId: Int,
    private val appContext: Context,
    private val acumularProductoUseCase: AcumularProductoUseCase = AcumularProductoUseCase(repository),
    private val exportarPdfUseCase: ExportarPdfUseCase = ExportarPdfUseCase(repository, appContext)
) : ViewModel() {

    private val _persona = MutableStateFlow<Persona?>(null)
    val persona: StateFlow<Persona?> = _persona.asStateFlow()

    val saldo: StateFlow<Long> = repository.saldoPorPersona(personaId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val movimientos: StateFlow<List<Movimiento>> = repository.movimientosPorPersonaHoy(personaId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Catálogo dinámico cargado desde la base de datos */
    private val _catalogoCategorias = MutableStateFlow<List<CategoriaConProductos>>(emptyList())
    val catalogoCategorias: StateFlow<List<CategoriaConProductos>> = _catalogoCategorias.asStateFlow()

    private val _snackEvents = Channel<String>(Channel.BUFFERED)
    val snackEvents = _snackEvents.receiveAsFlow()

    private val _eventos = Channel<DetalleEvent>(Channel.BUFFERED)
    val eventos = _eventos.receiveAsFlow()

    private val _exportState = MutableStateFlow(ExportState())
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    private val _desdeMs = MutableStateFlow(inicioDeDiaHoy())
    private val _hastaMs = MutableStateFlow(finDeDiaHoy())
    val desdeMs: StateFlow<Long> = _desdeMs.asStateFlow()
    val hastaMs: StateFlow<Long> = _hastaMs.asStateFlow()

    init {
        viewModelScope.launch {
            repository.personas.collect { lista ->
                _persona.value = lista.find { it.id == personaId }
            }
        }
        // Re-carga el catálogo cuando cambian categorías O productos
        viewModelScope.launch {
            combine(
                repository.getCategoriasFlow(),
                repository.getAllProductosFlow()
            ) { _, _ -> Unit }
                .collect {
                    _catalogoCategorias.value = repository.getCategoriaConProductos()
                }
        }
    }

    fun editarPersona(nombre: String, descripcion: String) {
        val actual = _persona.value ?: return
        viewModelScope.launch {
            try {
                repository.updatePersona(actual.copy(nombre = nombre.trim(), descripcion = descripcion.trim()))
                _snackEvents.send("Datos actualizados")
            } catch (e: android.database.sqlite.SQLiteConstraintException) {
                _snackEvents.send("Ya existe un cliente con ese nombre y descripción")
            }
        }
    }

    fun registrarFiado(montoCentavos: Long, nota: String) {
        viewModelScope.launch {
            repository.registrarFiado(personaId, montoCentavos, nota)
            _snackEvents.send("$nota anotado — ${montoCentavos.centavosAtexto()}")
        }
    }

    fun acumularProducto(notaBase: String, precioCentavos: Long) {
        viewModelScope.launch {
            val mensaje = acumularProductoUseCase(
                personaId      = personaId,
                movimientosHoy = movimientos.value,
                notaBase       = notaBase,
                precioCentavos = precioCentavos
            )
            _snackEvents.send(mensaje)
        }
    }

    fun reducirProducto(mov: Movimiento, precioCentavos: Long) {
        viewModelScope.launch {
            val cantidadActual = cantidadDeNota(mov.nota)
            if (cantidadActual <= 1) {
                repository.eliminarMovimiento(mov)
            } else {
                val nuevaCantidad = cantidadActual - 1
                val nuevoMonto    = precioCentavos * nuevaCantidad
                val notaBaseStr   = notaBase(mov.nota)
                val nuevaNota     = "$notaBaseStr x$nuevaCantidad"
                repository.editarMovimiento(mov.copy(monto = nuevoMonto, nota = nuevaNota))
            }
        }
    }

    fun registrarPago(montoCentavos: Long) {
        viewModelScope.launch {
            repository.registrarPago(personaId, montoCentavos, "Pago")
            _snackEvents.send("Pago registrado — ${montoCentavos.centavosAtexto()}")
        }
    }

    fun eliminarMovimiento(mov: Movimiento) {
        viewModelScope.launch { repository.eliminarMovimiento(mov) }
    }

    fun enviarError(msg: String) {
        viewModelScope.launch { _snackEvents.send(msg) }
    }

    fun abrirDialogoExport() {
        _desdeMs.value = inicioDeDiaHoy()
        _hastaMs.value = finDeDiaHoy()
        _exportState.value = ExportState(mostrando = true)
    }

    fun cerrarDialogoExport() {
        _exportState.value = ExportState(mostrando = false)
    }

    fun setDesde(ms: Long) {
        val local = desdeDatePicker(ms)
        _desdeMs.value = local
        if (local > _hastaMs.value) _hastaMs.value = finDeDia(local)
    }

    fun setHasta(ms: Long) {
        _hastaMs.value = finDeDia(desdeDatePicker(ms))
    }

    fun exportarPdf() {
        val persona = _persona.value ?: return
        _exportState.value = _exportState.value.copy(generando = true, error = null)

        viewModelScope.launch {
            when (val resultado = exportarPdfUseCase(persona, _desdeMs.value, _hastaMs.value)) {
                is ExportarPdfUseCase.Resultado.SinMovimientos ->
                    _exportState.value = _exportState.value.copy(
                        generando = false,
                        error     = "No hay movimientos en ese período"
                    )
                is ExportarPdfUseCase.Resultado.ErrorAlGenerar ->
                    _exportState.value = _exportState.value.copy(
                        generando = false,
                        error     = "Error al generar el PDF"
                    )
                is ExportarPdfUseCase.Resultado.Exito -> {
                    _exportState.value = ExportState(mostrando = false)
                    _eventos.send(DetalleEvent.CompartirPdf(resultado.uri))
                }
            }
        }
    }

    class Factory(
        private val app: android.app.Application,
        private val personaId: Int
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val repo = (app as CafetinApp).container.repository
            return DetalleViewModel(repo, personaId, app.applicationContext) as T
        }
    }
}
