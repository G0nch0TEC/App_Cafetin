package com.proyecto.cafetin.ui.backup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.proyecto.cafetin.backup.BackupManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class BackupEstado {
    object Idle : BackupEstado()
    object Cargando : BackupEstado()
    data class Exito(val mensaje: String) : BackupEstado()
    data class Error(val mensaje: String) : BackupEstado()
}

class BackupViewModel(private val backupManager: BackupManager) : ViewModel() {

    private val _estado = MutableStateFlow<BackupEstado>(BackupEstado.Idle)
    val estado: StateFlow<BackupEstado> = _estado

    fun exportar(uri: Uri) {
        viewModelScope.launch {
            _estado.value = BackupEstado.Cargando
            val error = backupManager.exportar(uri)
            _estado.value = if (error == null) {
                BackupEstado.Exito("✅ Respaldo exportado correctamente")
            } else {
                BackupEstado.Error(error)
            }
        }
    }

    fun importar(uri: Uri) {
        viewModelScope.launch {
            _estado.value = BackupEstado.Cargando
            val error = backupManager.importar(uri)
            _estado.value = if (error == null) {
                BackupEstado.Exito("✅ Datos importados correctamente")
            } else {
                BackupEstado.Error(error)
            }
        }
    }

    fun limpiarEstado() {
        _estado.value = BackupEstado.Idle
    }

    fun nombreArchivoSugerido() = backupManager.nombreArchivoSugerido()

    class Factory(private val backupManager: BackupManager) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            BackupViewModel(backupManager) as T
    }
}
