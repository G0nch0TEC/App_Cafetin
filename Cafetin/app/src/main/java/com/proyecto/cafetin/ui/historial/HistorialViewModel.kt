package com.proyecto.cafetin.ui.historial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto.cafetin.data.model.Movimiento
import com.proyecto.cafetin.data.model.Persona
import com.proyecto.cafetin.data.model.TipoMovimiento
import com.proyecto.cafetin.repository.ICafetinRepository
import com.proyecto.cafetin.util.DateUtils.inicioDeDiaHoy
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ResumenDia(
    val totalFiado: Long,
    val totalCobrado: Long,
    val neto: Long
)

/** Todos los movimientos de una persona en el día, con su resumen parcial */
data class GrupoPersona(
    val persona: Persona,
    val movimientos: List<Movimiento>,
    val totalFiado: Long,
    val totalCobrado: Long
)

@OptIn(ExperimentalCoroutinesApi::class)
class HistorialViewModel(private val repository: ICafetinRepository) : ViewModel() {

    // ── Día visible ───────────────────────────────────────────────────────────
    private val _diaActual = MutableStateFlow(inicioDeDiaHoy())
    val diaActual: StateFlow<Long> = _diaActual.asStateFlow()

    val esHoy: StateFlow<Boolean> = _diaActual
        .map { it == inicioDeDiaHoy() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    // ── Buscador ──────────────────────────────────────────────────────────────
    private val _busqueda = MutableStateFlow("")
    val busqueda: StateFlow<String> = _busqueda.asStateFlow()
    fun setBusqueda(q: String) { _busqueda.value = q }

    // ── Movimientos del día ───────────────────────────────────────────────────
    private val movimientosDia: StateFlow<List<Movimiento>> = _diaActual
        .flatMapLatest { repository.movimientosPorDia(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val personas: StateFlow<List<Persona>> = repository.personas
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Grupos por persona (con filtro de búsqueda aplicado) ─────────────────
    val gruposFiltrados: StateFlow<List<GrupoPersona>> = combine(
        movimientosDia, personas, _busqueda
    ) { movs, personas, query ->
        val personasMap = personas.associateBy { it.id }

        // Agrupar movimientos por personaId
        val agrupados = movs.groupBy { it.personaId }

        agrupados
            .mapNotNull { (personaId, movsDePers) ->
                val persona = personasMap[personaId] ?: return@mapNotNull null
                GrupoPersona(
                    persona      = persona,
                    movimientos  = movsDePers,
                    totalFiado   = movsDePers.filter { it.tipo == TipoMovimiento.FIADO }.sumOf { it.monto },
                    totalCobrado = movsDePers.filter { it.tipo == TipoMovimiento.PAGO  }.sumOf { it.monto }
                )
            }
            // Filtrar por búsqueda sobre el nombre de la persona
            .filter { query.isBlank() || it.persona.nombre.contains(query, ignoreCase = true) }
            // Ordenar: primero los que tienen deuda pendiente, luego al día
            .sortedByDescending { it.totalFiado - it.totalCobrado }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Resumen global del día ────────────────────────────────────────────────
    val resumenDia: StateFlow<ResumenDia> = movimientosDia.map { lista ->
        val fiado   = lista.filter { it.tipo == TipoMovimiento.FIADO }.sumOf { it.monto }
        val cobrado = lista.filter { it.tipo == TipoMovimiento.PAGO  }.sumOf { it.monto }
        ResumenDia(fiado, cobrado, fiado - cobrado)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ResumenDia(0, 0, 0))

    // ── Navegación ────────────────────────────────────────────────────────────
    fun diaAnterior() {
        _busqueda.value = ""   // limpiar búsqueda al cambiar de día
        _diaActual.value = _diaActual.value - 24 * 60 * 60 * 1000L
    }

    fun diaSiguiente() {
        val siguienteDia = _diaActual.value + 24 * 60 * 60 * 1000L
        if (siguienteDia <= inicioDeDiaHoy()) {
            _busqueda.value = ""
            _diaActual.value = siguienteDia
        }
    }

    fun irAHoy() {
        _busqueda.value = ""
        _diaActual.value = inicioDeDiaHoy()
    }

    fun eliminarMovimiento(movimiento: Movimiento) {
        viewModelScope.launch { repository.eliminarMovimiento(movimiento) }
    }
}
