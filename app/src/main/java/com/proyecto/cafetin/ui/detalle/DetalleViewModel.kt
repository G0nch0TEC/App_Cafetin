package com.proyecto.cafetin.ui.detalle

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.proyecto.cafetin.CafetinApp
import com.proyecto.cafetin.data.model.Movimiento
import com.proyecto.cafetin.data.model.Persona
import com.proyecto.cafetin.domain.usecase.AcumularProductoUseCase
import com.proyecto.cafetin.repository.ICafetinRepository
import com.proyecto.cafetin.util.DateUtils
import com.proyecto.cafetin.util.DateUtils.finDeDia
import com.proyecto.cafetin.util.DateUtils.finDeDiaHoy
import com.proyecto.cafetin.util.DateUtils.inicioDeDiaHoy
import com.proyecto.cafetin.util.MoneyUtils.centavosAtexto
import com.proyecto.cafetin.util.NotaUtils.cantidadDeNota
import com.proyecto.cafetin.util.NotaUtils.notaBase
import com.proyecto.cafetin.util.PdfExporter
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/** Estado del diálogo de exportación */
data class ExportState(
    val mostrando: Boolean = false,
    val generando: Boolean = false,
    val error: String?     = null
)

class DetalleViewModel(
    private val repository: ICafetinRepository,
    val personaId: Int,
    private val acumularProductoUseCase: AcumularProductoUseCase = AcumularProductoUseCase(repository)
) : ViewModel() {

    private val _persona = MutableStateFlow<Persona?>(null)
    val persona: StateFlow<Persona?> = _persona.asStateFlow()

    val saldo: StateFlow<Long> = repository.saldoPorPersona(personaId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val movimientos: StateFlow<List<Movimiento>> = repository.movimientosPorPersonaHoy(personaId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _snackEvents = Channel<String>(Channel.BUFFERED)
    val snackEvents = _snackEvents.receiveAsFlow()

    // ── Estado exportación PDF ────────────────────────────────────────────────
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

    /**
     * Delega en [AcumularProductoUseCase] toda la lógica de INSERT/UPDATE.
     * El UseCase retorna el mensaje de confirmación listo para el Snackbar.
     */
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

    /**
     * Reduce en 1 la cantidad de un movimiento acumulado.
     * Si llega a 0 lo elimina directamente.
     */
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

    // ── Exportación PDF ───────────────────────────────────────────────────────

    fun abrirDialogoExport() {
        _desdeMs.value = inicioDeDiaHoy()
        _hastaMs.value = finDeDiaHoy()
        _exportState.value = ExportState(mostrando = true)
    }

    fun cerrarDialogoExport() {
        _exportState.value = ExportState(mostrando = false)
    }

    fun setDesde(ms: Long) {
        // El DatePicker devuelve medianoche UTC; corregimos a medianoche local
        val local = DateUtils.inicioDeDia(ms + java.util.TimeZone.getDefault().getOffset(ms))
        _desdeMs.value = local
        if (local > _hastaMs.value) _hastaMs.value = DateUtils.finDeDia(local)
    }

    fun setHasta(ms: Long) {
        val local = DateUtils.inicioDeDia(ms + java.util.TimeZone.getDefault().getOffset(ms))
        _hastaMs.value = DateUtils.finDeDia(local)
    }

    fun exportarPdf(context: Context) {
        val persona = _persona.value ?: return
        _exportState.value = _exportState.value.copy(generando = true, error = null)

        viewModelScope.launch {
            val movs = repository.movimientosPorPersonaEnRango(
                personaId, _desdeMs.value, _hastaMs.value
            )

            if (movs.isEmpty()) {
                _exportState.value = _exportState.value.copy(
                    generando = false, error = "No hay movimientos en ese período"
                )
                return@launch
            }

            val file = PdfExporter.generar(
                context            = context,
                nombrePersona      = persona.nombre,
                descripcionPersona = persona.descripcion,
                movimientos        = movs,
                desde              = _desdeMs.value,
                hasta              = _hastaMs.value
            )

            if (file == null) {
                _exportState.value = _exportState.value.copy(
                    generando = false, error = "Error al generar el PDF"
                )
                return@launch
            }

            // ── Marcar estado "Enviado" hasta el inicio del día siguiente ────
            // El estado expira automáticamente: la UI compara enviadoHasta con
            // el timestamp actual, así no hace falta ningún trabajo programado.
            val finDelDiaActual = finDeDia(System.currentTimeMillis())
            repository.marcarEnviado(personaId, finDelDiaActual)

            _exportState.value = ExportState(mostrando = false)

            val uri = PdfExporter.uriParaCompartir(context, file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type     = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Reporte de ${persona.nombre}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Compartir reporte"))
        }
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    class Factory(
        private val app: android.app.Application,
        private val personaId: Int
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val repo = (app as CafetinApp).container.repository
            return DetalleViewModel(repo, personaId) as T
        }
    }
}
