package com.proyecto.cafetin.ui.catalogo

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.proyecto.cafetin.CafetinApp
import com.proyecto.cafetin.data.model.CatalogoCategoria
import com.proyecto.cafetin.data.model.CatalogoProducto
import com.proyecto.cafetin.repository.ICafetinRepository
import com.proyecto.cafetin.sync.SyncManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CatalogoViewModel(
    private val repository: ICafetinRepository,
    private val app: Application,
    private val deviceId: String
) : ViewModel() {

    private val syncManager = SyncManager(app.applicationContext, deviceId)

    val categorias: StateFlow<List<CatalogoCategoria>> =
        repository.getCategoriasFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _snackEvents = Channel<String>(Channel.BUFFERED)
    val snackEvents = _snackEvents.receiveAsFlow()

    private val _categoriaSeleccionada = MutableStateFlow<CatalogoCategoria?>(null)
    val categoriaSeleccionada: StateFlow<CatalogoCategoria?> = _categoriaSeleccionada.asStateFlow()

    val productosDeCategoriaActual: StateFlow<List<CatalogoProducto>> =
        _categoriaSeleccionada
            .flatMapLatest { cat ->
                if (cat == null) flowOf(emptyList())
                else repository.getProductosByCategoriaFlow(cat.id)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun seleccionarCategoria(cat: CatalogoCategoria?) {
        _categoriaSeleccionada.value = cat
    }

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

    fun agregarCategoria(nombre: String, emoji: String) {
        if (nombre.isBlank()) return
        viewModelScope.launch {
            val orden = categorias.value.size
            repository.insertCategoria(
                CatalogoCategoria(nombre = nombre.trim(), emoji = emoji.ifBlank { "📦" }, orden = orden)
            )
            _snackEvents.send("Categoría \"${nombre.trim()}\" creada")
            sincronizar()
        }
    }

    fun editarCategoria(cat: CatalogoCategoria, nuevoNombre: String, nuevoEmoji: String) {
        if (nuevoNombre.isBlank()) return
        viewModelScope.launch {
            repository.updateCategoria(cat.copy(nombre = nuevoNombre.trim(), emoji = nuevoEmoji.ifBlank { cat.emoji }))
            _snackEvents.send("Categoría actualizada")
            sincronizar()
        }
    }

    fun eliminarCategoria(cat: CatalogoCategoria) {
        viewModelScope.launch {
            repository.deleteCategoria(cat)
            if (_categoriaSeleccionada.value?.id == cat.id) _categoriaSeleccionada.value = null
            _snackEvents.send("Categoría \"${cat.nombre}\" eliminada")
            sincronizar()
        }
    }

    fun agregarProducto(categoriaId: Int, nombre: String, montoCentavos: Long) {
        if (nombre.isBlank() || montoCentavos <= 0) return
        viewModelScope.launch {
            val orden = productosDeCategoriaActual.value.size
            repository.insertProducto(
                CatalogoProducto(categoriaId = categoriaId, nombre = nombre.trim(), montoCentavos = montoCentavos, orden = orden)
            )
            _snackEvents.send("\"${nombre.trim()}\" agregado")
            sincronizar()
        }
    }

    fun editarProducto(prod: CatalogoProducto, nuevoNombre: String, nuevoMonto: Long) {
        if (nuevoNombre.isBlank() || nuevoMonto <= 0) return
        viewModelScope.launch {
            repository.updateProducto(prod.copy(nombre = nuevoNombre.trim(), montoCentavos = nuevoMonto))
            _snackEvents.send("Producto actualizado")
            sincronizar()
        }
    }

    fun eliminarProducto(prod: CatalogoProducto) {
        viewModelScope.launch {
            repository.deleteProducto(prod)
            _snackEvents.send("\"${prod.nombre}\" eliminado")
            sincronizar()
        }
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val container = (app as CafetinApp).container
            return CatalogoViewModel(container.repository, app, container.deviceId) as T
        }
    }
}