package com.proyecto.cafetin.ui.personas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyecto.cafetin.data.model.Persona
import com.proyecto.cafetin.repository.ICafetinRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class OrdenPersonas { NOMBRE, MAYOR_DEUDA, AL_DIA_PRIMERO }
enum class FiltroPersonas { TODOS, CON_DEUDA, AL_DIA }

class PersonasViewModel(private val repository: ICafetinRepository, val personaId: Int) : ViewModel() {

    val personas: StateFlow<List<Persona>> = repository.personas
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val saldoTotal: StateFlow<Long> = repository.saldoTotal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val cobradoHoy: StateFlow<Long> = repository.cobradoHoy()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    private val _saldosPorPersona = MutableStateFlow<Map<Int, Long>>(emptyMap())
    val saldosPorPersona: StateFlow<Map<Int, Long>> = _saldosPorPersona.asStateFlow()

    private val _busqueda = MutableStateFlow("")
    val busqueda: StateFlow<String> = _busqueda.asStateFlow()

    private val _orden = MutableStateFlow(OrdenPersonas.NOMBRE)
    val orden: StateFlow<OrdenPersonas> = _orden.asStateFlow()

    private val _filtro = MutableStateFlow(FiltroPersonas.TODOS)
    val filtro: StateFlow<FiltroPersonas> = _filtro.asStateFlow()

    // Mapa de Jobs activos por personaId — evita collectors duplicados y memory leaks
    private val saldoJobs = mutableMapOf<Int, Job>()

    init {
        viewModelScope.launch {
            personas.collect { lista ->
                val idsActuales = lista.map { it.id }.toSet()

                // Cancelar jobs de personas ya eliminadas
                val idsAEliminar = saldoJobs.keys.filter { it !in idsActuales }
                idsAEliminar.forEach { id ->
                    saldoJobs.remove(id)?.cancel()
                    _saldosPorPersona.value = _saldosPorPersona.value - id
                }

                // Iniciar collector solo para personas nuevas (sin job activo)
                lista.filter { it.id !in saldoJobs }.forEach { persona ->
                    saldoJobs[persona.id] = launch {
                        repository.saldoPorPersona(persona.id).collect { saldo ->
                            _saldosPorPersona.value = _saldosPorPersona.value + (persona.id to saldo)
                        }
                    }
                }
            }
        }
    }

    fun setBusqueda(q: String) { _busqueda.value = q }
    fun setOrden(o: OrdenPersonas) { _orden.value = o }
    fun setFiltro(f: FiltroPersonas) { _filtro.value = f }

    fun agregarPersona(nombre: String, descripcion: String) {
        if (nombre.isBlank()) return
        viewModelScope.launch {
            repository.insertPersona(Persona(nombre = nombre.trim(), descripcion = descripcion.trim()))
        }
    }

    fun eliminarPersona(persona: Persona) {
        viewModelScope.launch {
            repository.deletePersona(persona)
        }
    }
}
