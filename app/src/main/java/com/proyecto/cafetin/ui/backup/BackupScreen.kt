package com.proyecto.cafetin.ui.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.proyecto.cafetin.CafetinApp
import com.proyecto.cafetin.backup.BackupManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as CafetinApp

    val backupManager = remember {
        BackupManager(context, app.container.repository)
    }

    val vm: BackupViewModel = viewModel(
        factory = BackupViewModel.Factory(backupManager)
    )

    val estado by vm.estado.collectAsState()

    // ── Launcher para EXPORTAR: el usuario elige dónde guardar ──────────────
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(BackupManager.MIME_TYPE)
    ) { uri: Uri? ->
        uri?.let { vm.exportar(it) }
    }

    // ── Launcher para IMPORTAR: el usuario elige el archivo ─────────────────
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { vm.importar(it) }
    }

    // ── Diálogo de confirmación para importar ────────────────────────────────
    var mostrarConfirmImport by remember { mutableStateOf(false) }
    var uriPendienteImport by remember { mutableStateOf<Uri?>(null) }

    if (mostrarConfirmImport) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmImport = false },
            title = { Text("⚠️ Reemplazar datos") },
            text = {
                Text(
                    "Esta acción reemplazará TODOS los datos actuales (personas, movimientos y catálogo) " +
                    "con los del archivo de respaldo.\n\nEsta acción no se puede deshacer. ¿Continuar?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        mostrarConfirmImport = false
                        uriPendienteImport?.let { vm.importar(it) }
                    }
                ) { Text("Importar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmImport = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // ── Snackbar de resultado ────────────────────────────────────────────────
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(estado) {
        when (val s = estado) {
            is BackupEstado.Exito -> {
                snackbarHostState.showSnackbar(s.mensaje)
                vm.limpiarEstado()
            }
            is BackupEstado.Error -> {
                snackbarHostState.showSnackbar(s.mensaje)
                vm.limpiarEstado()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Respaldo de datos") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Descripción
            Text(
                text = "Guarda una copia de todos tus datos o restáuralos desde un respaldo anterior. " +
                       "Útil si cambias de celular o quieres hacer una copia de seguridad.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tarjeta EXPORTAR
            BackupCard(
                titulo = "Exportar datos",
                descripcion = "Guarda todos tus personas, movimientos y catálogo en un archivo JSON que podrás guardar donde quieras.",
                icono = Icons.Default.Upload,
                botonTexto = "Exportar",
                habilitado = estado !is BackupEstado.Cargando,
                onClick = {
                    exportLauncher.launch(vm.nombreArchivoSugerido())
                }
            )

            // Tarjeta IMPORTAR
            BackupCard(
                titulo = "Importar datos",
                descripcion = "Restaura los datos desde un archivo de respaldo. Reemplazará todos los datos actuales.",
                icono = Icons.Default.Download,
                botonTexto = "Importar",
                habilitado = estado !is BackupEstado.Cargando,
                esDestructivo = true,
                onClick = {
                    // Primero pedimos el archivo, luego confirmamos
                    importLauncher.launch(arrayOf(BackupManager.MIME_TYPE, "text/plain", "*/*"))
                }
            )

            // Loading
            if (estado is BackupEstado.Cargando) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Nota al pie
            Text(
                text = "💡 Tip: después de importar, la app se actualiza automáticamente con los datos restaurados.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun BackupCard(
    titulo: String,
    descripcion: String,
    icono: ImageVector,
    botonTexto: String,
    habilitado: Boolean,
    esDestructivo: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icono,
                    contentDescription = null,
                    tint = if (esDestructivo)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = titulo,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }

            Text(
                text = descripcion,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )

            Button(
                onClick = onClick,
                enabled = habilitado,
                modifier = Modifier.align(Alignment.End),
                colors = if (esDestructivo) ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ) else ButtonDefaults.buttonColors()
            ) {
                Text(botonTexto)
            }
        }
    }
}
