package com.proyecto.cafetin.ui.personas

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.proyecto.cafetin.CafetinApp
import com.proyecto.cafetin.data.model.Persona
import com.proyecto.cafetin.repository.ICafetinRepository
import com.proyecto.cafetin.sync.SyncManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class OrdenPersonas { NOMBRE, MAYOR_DEUDA, AL_DIA_PRIMERO }
enum class FiltroPersonas { TODOS, CON_DEUDA, AL_DIA, ENVIADO, A_FAVOR }

class PersonasViewModel(
    private val repository: ICafetinRepository,
    private val app: Application,
    private val deviceId: String
) : ViewModel() {

    private val syncManager = SyncManager(app.applicationContext, deviceId)

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

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun clearError() { _error.value = null }
    fun setBusqueda(q: String) { _busqueda.value = q }
    fun setOrden(o: OrdenPersonas) { _orden.value = o }
    fun setFiltro(f: FiltroPersonas) { _filtro.value = f }

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
            kotlinx.coroutines.CoroutineScope(SupervisorJob() + kotlinx.coroutines.Dispatchers.IO).launch {
                try { syncManager.sincronizar() } catch (_: Exception) {}
            }
        }
    }

    fun agregarPersona(nombre: String, descripcion: String) {
        if (nombre.isBlank()) return
        viewModelScope.launch {
            try {
                repository.insertPersona(
                    Persona(nombre = nombre.trim(), descripcion = descripcion.trim())
                )
                sincronizar()
            } catch (e: android.database.sqlite.SQLiteConstraintException) {
                _error.value = "Ya existe una persona con ese nombre"
            } catch (e: Exception) {
                _error.value = "Error al guardar: ${e.message}"
            }
        }
    }

    fun eliminarPersona(persona: Persona) {
        viewModelScope.launch {
            try {
                repository.deletePersona(persona)
                sincronizar()
            } catch (e: Exception) {
                _error.value = "Error al eliminar: ${e.message}"
            }
        }
    }

    fun quitarEnviado(persona: Persona) {
        viewModelScope.launch {
            repository.marcarEnviado(persona.id, 0L)
        }
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val container = (app as CafetinApp).container
            return PersonasViewModel(container.repository, app, container.deviceId) as T
        }
    }
}