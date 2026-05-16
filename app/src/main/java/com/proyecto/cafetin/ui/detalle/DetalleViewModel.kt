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
import com.proyecto.cafetin.sync.SyncManager
import com.proyecto.cafetin.util.DateUtils.desdeDatePicker
import com.proyecto.cafetin.util.DateUtils.finDeDia
import com.proyecto.cafetin.util.DateUtils.finDeDiaHoy
import com.proyecto.cafetin.util.DateUtils.inicioDeDiaHoy
import com.proyecto.cafetin.util.MoneyUtils.centavosAtexto
import com.proyecto.cafetin.util.NotaUtils.cantidadDeNota
import com.proyecto.cafetin.util.NotaUtils.notaBase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DetalleViewModel(
    private val repository: ICafetinRepository,
    val personaId: Int,
    private val appContext: Context,
    private val deviceId: String,
    private val acumularProductoUseCase: AcumularProductoUseCase = AcumularProductoUseCase(repository),
    private val exportarPdfUseCase: ExportarPdfUseCase = ExportarPdfUseCase(repository, appContext)
) : ViewModel() {

    private val syncManager = SyncManager(appContext, deviceId)

    private val _persona = MutableStateFlow<Persona?>(null)
    val persona: StateFlow<Persona?> = _persona.asStateFlow()

    val saldo: StateFlow<Long> = repository.saldoPorPersona(personaId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val movimientos: StateFlow<List<Movimiento>> = repository.movimientosPorPersonaHoy(personaId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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

    /** Sincroniza en segundo plano sin bloquear la UI */
    private var syncJob: Job? = null
    private var pendingSync = false

    private fun sincronizar() {
        pendingSync = true
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            delay(3_000)
            pendingSync = false
            try { syncManager.sincronizar() } catch (_: Exception) {}
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (pendingSync) {
            // viewModelScope ya está cancelado — usamos un scope desacoplado para
            // que el sync llegue al servidor aunque el usuario haya navegado.
            kotlinx.coroutines.CoroutineScope(SupervisorJob() + kotlinx.coroutines.Dispatchers.IO).launch {
                try { syncManager.sincronizar() } catch (_: Exception) {}
            }
        }
    }

    fun editarPersona(nombre: String, descripcion: String) {
        val actual = _persona.value ?: return
        viewModelScope.launch {
            try {
                repository.updatePersona(actual.copy(nombre = nombre.trim(), descripcion = descripcion.trim()))
                _snackEvents.send("Datos actualizados")
                sincronizar()
            } catch (e: android.database.sqlite.SQLiteConstraintException) {
                _snackEvents.send("Ya existe un cliente con ese nombre y descripción")
            }
        }
    }

    fun registrarFiado(montoCentavos: Long, nota: String) {
        viewModelScope.launch {
            repository.registrarFiado(personaId, montoCentavos, nota)
            _snackEvents.send("$nota anotado — ${montoCentavos.centavosAtexto()}")
            sincronizar()
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
            sincronizar()
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
            sincronizar()
        }
    }

    fun registrarPago(montoCentavos: Long) {
        viewModelScope.launch {
            repository.registrarPago(personaId, montoCentavos, "Pago")
            _snackEvents.send("Pago registrado — ${montoCentavos.centavosAtexto()}")
            sincronizar()
        }
    }

    fun eliminarMovimiento(mov: Movimiento) {
        viewModelScope.launch {
            repository.eliminarMovimiento(mov)
            sincronizar()
        }
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

    fun confirmarEnviado() {
        val persona = _persona.value ?: return
        viewModelScope.launch {
            repository.marcarEnviado(persona.id, finDeDiaHoy())
        }
    }

    fun quitarEnviado() {
        val persona = _persona.value ?: return
        viewModelScope.launch {
            repository.marcarEnviado(persona.id, 0L)
        }
    }

    class Factory(
        private val app: android.app.Application,
        private val personaId: Int
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val container = (app as CafetinApp).container
            return DetalleViewModel(container.repository, personaId, app.applicationContext, container.deviceId) as T
        }
    }
}