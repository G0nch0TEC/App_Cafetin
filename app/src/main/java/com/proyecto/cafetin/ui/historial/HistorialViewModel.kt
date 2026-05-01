package com.proyecto.cafetin.ui.historial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.proyecto.cafetin.data.model.Movimiento
import com.proyecto.cafetin.data.model.Persona
import com.proyecto.cafetin.data.model.TipoMovimiento
import com.proyecto.cafetin.repository.ICafetinRepository
import com.proyecto.cafetin.util.DateUtils
import com.proyecto.cafetin.util.DateUtils.inicioDeDiaHoy
import com.proyecto.cafetin.util.fuzzyMatch
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
    val totalCobrado: Long,
    /** Saldo histórico acumulado real de la persona (todos los tiempos) */
    val saldoReal: Long = 0L
)

enum class FiltroTipo { TODOS, FIADOS, PAGOS }

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

    // ── Filtro por tipo ───────────────────────────────────────────────────────
    private val _filtroTipo = MutableStateFlow(FiltroTipo.TODOS)
    val filtroTipo: StateFlow<FiltroTipo> = _filtroTipo.asStateFlow()
    fun setFiltroTipo(f: FiltroTipo) { _filtroTipo.value = f }

    // ── Movimientos del día ───────────────────────────────────────────────────
    private val movimientosDia: StateFlow<List<Movimiento>> = _diaActual
        .flatMapLatest { repository.movimientosPorDia(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val personas: StateFlow<List<Persona>> = repository.personas
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Saldos reales de todas las personas (flow reactivo)
    @OptIn(ExperimentalCoroutinesApi::class)
    private val saldosReales: StateFlow<Map<Int, Long>> = personas
        .flatMapLatest { lista ->
            if (lista.isEmpty()) flowOf(emptyMap())
            else combine(
                lista.map { p -> repository.saldoPorPersona(p.id).map { p.id to it } }
            ) { pares -> pares.toMap() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    // ── Grupos por persona (con filtro de búsqueda y tipo aplicado) ──────────
    val gruposFiltrados: StateFlow<List<GrupoPersona>> = combine(
        movimientosDia, personas, _busqueda, _filtroTipo, saldosReales
    ) { movs, personas, query, filtroTipo, saldos ->
        val personasMap = personas.associateBy { it.id }

        // Aplicar filtro de tipo antes de agrupar
        val movsFiltrados = when (filtroTipo) {
            FiltroTipo.TODOS   -> movs
            FiltroTipo.FIADOS  -> movs.filter { it.tipo == TipoMovimiento.FIADO }
            FiltroTipo.PAGOS   -> movs.filter { it.tipo == TipoMovimiento.PAGO  }
        }

        movsFiltrados
            .groupBy { it.personaId }
            .mapNotNull { (personaId, movsDePers) ->
                val persona = personasMap[personaId] ?: return@mapNotNull null
                GrupoPersona(
                    persona      = persona,
                    movimientos  = movsDePers,
                    totalFiado   = movsDePers.filter { it.tipo == TipoMovimiento.FIADO }.sumOf { it.monto },
                    totalCobrado = movsDePers.filter { it.tipo == TipoMovimiento.PAGO  }.sumOf { it.monto },
                    saldoReal    = saldos[personaId] ?: 0L
                )
            }
            .filter { query.isBlank() || fuzzyMatch(it.persona.nombre, query) }
            .sortedByDescending { it.totalFiado - it.totalCobrado }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Resumen global del día (sobre movimientos sin filtro de tipo) ─────────
    val resumenDia: StateFlow<ResumenDia> = movimientosDia.map { lista ->
        val fiado   = lista.filter { it.tipo == TipoMovimiento.FIADO }.sumOf { it.monto }
        val cobrado = lista.filter { it.tipo == TipoMovimiento.PAGO  }.sumOf { it.monto }
        ResumenDia(fiado, cobrado, fiado - cobrado)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ResumenDia(0, 0, 0))

    // ── Navegación ────────────────────────────────────────────────────────────
    fun diaAnterior() {
        _diaActual.value = _diaActual.value - DateUtils.UN_DIA_MS
    }

    fun diaSiguiente() {
        val siguienteDia = _diaActual.value + DateUtils.UN_DIA_MS
        if (siguienteDia <= inicioDeDiaHoy()) {
            _diaActual.value = siguienteDia
        }
    }

    fun irAHoy() {
        _diaActual.value = inicioDeDiaHoy()
    }

    /** Salta directamente a un día elegido desde el DatePicker (timestamp UTC medianoche del picker) */
    fun irADia(utcMidnightMs: Long) {
        val localMs = DateUtils.desdeDatePicker(utcMidnightMs)
        // No permitir fechas futuras
        if (localMs <= inicioDeDiaHoy()) {
            _diaActual.value = localMs
        }
    }

    fun eliminarMovimiento(movimiento: Movimiento) {
        viewModelScope.launch { repository.eliminarMovimiento(movimiento) }
    }

    // ── Factory ───────────────────────────────────────────────────────────────
    class Factory(private val repository: ICafetinRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HistorialViewModel(repository) as T
    }
}
