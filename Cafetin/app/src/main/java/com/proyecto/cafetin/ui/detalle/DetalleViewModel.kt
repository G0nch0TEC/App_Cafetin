package com.proyecto.cafetin.ui.detalle

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.proyecto.cafetin.data.db.AppDatabase
import com.proyecto.cafetin.data.model.Movimiento
import com.proyecto.cafetin.data.model.Persona
import com.proyecto.cafetin.data.model.TipoMovimiento
import com.proyecto.cafetin.repository.CafetinRepository
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

class DetalleViewModel(app: Application, val personaId: Int) : AndroidViewModel(app) {

    private val repo = CafetinRepository(AppDatabase.getInstance(app))

    private val _persona = MutableStateFlow<Persona?>(null)
    val persona: StateFlow<Persona?> = _persona.asStateFlow()

    val saldo: StateFlow<Long> = repo.saldoPorPersona(personaId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val movimientos: StateFlow<List<Movimiento>> = repo.movimientosPorPersonaHoy(personaId)
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
            repo.personas.collect { lista ->
                _persona.value = lista.find { it.id == personaId }
            }
        }
    }

    fun registrarFiado(montoCentavos: Long, nota: String) {
        viewModelScope.launch {
            repo.registrarFiado(personaId, montoCentavos, nota)
            _snackEvents.send("$nota anotado — ${montoCentavos.centavosAtexto()}")
        }
    }

    /**
     * Acumula un producto de categoría en el historial de hoy:
     * - Si ya existe un movimiento de FIADO con la misma nota base (sin el "xN"),
     *   hace UPDATE aumentando el monto en [precioCentavos] y actualiza la nota a "Nombre x2", etc.
     * - Si no existe, hace INSERT normal.
     *
     * [notaBase]      = nombre limpio del producto, ej: "Refresco"
     * [precioCentavos] = precio unitario del producto
     */
    fun acumularProducto(notaBase: String, precioCentavos: Long) {
        viewModelScope.launch {
            // Busca en la lista actual de hoy si ya existe un fiado con esa nota base
            val existente = movimientos.value.firstOrNull { mov ->
                mov.tipo == TipoMovimiento.FIADO &&
                        notaBase(mov.nota) == notaBase
            }

            if (existente != null) {
                // Calcula la nueva cantidad y monto
                val cantidadActual = cantidadDeNota(existente.nota)
                val nuevaCantidad  = cantidadActual + 1
                val nuevoMonto     = precioCentavos * nuevaCantidad
                val nuevaNota      = "$notaBase x$nuevaCantidad"

                repo.editarMovimiento(existente.copy(monto = nuevoMonto, nota = nuevaNota))
                _snackEvents.send("$nuevaNota — ${nuevoMonto.centavosAtexto()}")
            } else {
                // Primer toque: INSERT con "Nombre x1"
                repo.registrarFiado(personaId, precioCentavos, "$notaBase x1")
                _snackEvents.send("$notaBase x1 — ${precioCentavos.centavosAtexto()}")
            }
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
                // Quedaba 1 → eliminar el movimiento
                repo.eliminarMovimiento(mov)
            } else {
                val nuevaCantidad = cantidadActual - 1
                val nuevoMonto    = precioCentavos * nuevaCantidad
                val notaBase      = notaBase(mov.nota)
                val nuevaNota     = "$notaBase x$nuevaCantidad"
                repo.editarMovimiento(mov.copy(monto = nuevoMonto, nota = nuevaNota))
            }
        }
    }

    fun registrarPago(montoCentavos: Long) {
        viewModelScope.launch {
            repo.registrarPago(personaId, montoCentavos)
            _snackEvents.send("Pago registrado — ${montoCentavos.centavosAtexto()}")
        }
    }

    fun eliminarMovimiento(mov: Movimiento) {
        viewModelScope.launch { repo.eliminarMovimiento(mov) }
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
        _desdeMs.value = ms
        if (ms > _hastaMs.value) _hastaMs.value = ms + 24 * 60 * 60 * 1000L - 1
    }

    fun setHasta(ms: Long) {
        _hastaMs.value = ms + 24 * 60 * 60 * 1000L - 1
    }

    fun exportarPdf(context: Context) {
        val persona = _persona.value ?: return
        _exportState.value = _exportState.value.copy(generando = true, error = null)

        viewModelScope.launch {
            val movs = repo.movimientosPorPersonaEnRango(
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

    // ── Helpers ───────────────────────────────────────────────────────────────
    class Factory(private val app: Application, private val personaId: Int) :
        ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            DetalleViewModel(app, personaId) as T
    }
}
