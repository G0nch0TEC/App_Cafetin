package com.proyecto.cafetin.ui.personas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.proyecto.cafetin.data.model.Persona
import com.proyecto.cafetin.repository.ICafetinRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class OrdenPersonas { NOMBRE, MAYOR_DEUDA, AL_DIA_PRIMERO }
enum class FiltroPersonas { TODOS, CON_DEUDA, AL_DIA, ENVIADO, A_FAVOR }

class PersonasViewModel(private val repository: ICafetinRepository) : ViewModel() {

    val personas: StateFlow<List<Persona>> = repository.personas
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val saldoTotal: StateFlow<Long> = repository.saldoTotal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val cobradoHoy: StateFlow<Long> = repository.cobradoHoy()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val totalAFavor: StateFlow<Long> = repository.totalAFavor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    @OptIn(ExperimentalCoroutinesApi::class)
    val saldosPorPersona: StateFlow<Map<Int, Long>> = repository.personas
        .flatMapLatest { lista ->
            if (lista.isEmpty()) flowOf(emptyMap())
            else combine(
                lista.map { p -> repository.saldoPorPersona(p.id).map { p.id to it } }
            ) { pares -> pares.toMap() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private val _busqueda = MutableStateFlow("")
    val busqueda: StateFlow<String> = _busqueda.asStateFlow()

    private val _orden = MutableStateFlow(OrdenPersonas.NOMBRE)
    val orden: StateFlow<OrdenPersonas> = _orden.asStateFlow()

    private val _filtro = MutableStateFlow(FiltroPersonas.TODOS)
    val filtro: StateFlow<FiltroPersonas> = _filtro.asStateFlow()

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

    // ✅ Factory para inyectar el repositorio manualmente
    class Factory(private val repository: ICafetinRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PersonasViewModel(repository) as T
    }
}
