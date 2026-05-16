package com.proyecto.cafetin.ui.screen

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.integration.android.IntentIntegrator
import com.proyecto.cafetin.network.AuthApiService
import com.proyecto.cafetin.sync.SyncManager
import kotlinx.coroutines.launch

@Composable
fun ScanQrScreen(
    onVolver: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val deviceId = remember {
        (context.applicationContext as com.proyecto.cafetin.CafetinApp).container.deviceId
    }

    var estado by remember { mutableStateOf<EstadoScan>(EstadoScan.Espera) }

    val scanLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val token = result.data?.getStringExtra("SCAN_RESULT")
            if (token.isNullOrBlank()) {
                estado = EstadoScan.Error("No se pudo leer el código.")
                return@rememberLauncherForActivityResult
            }

            estado = EstadoScan.Cargando

            scope.launch {
                when (val res = AuthApiService.confirmarToken(token, context)) {
                    is AuthApiService.ResultadoAuth.Exito -> {
                        // ── Sync automático al confirmar QR ──────────────
                        try {
                            SyncManager(context, deviceId).sincronizar()
                        } catch (_: Exception) { /* sync falla silenciosamente */ }
                        // ─────────────────────────────────────────────────
                        estado = EstadoScan.Exito
                    }
                    is AuthApiService.ResultadoAuth.Error ->
                        estado = EstadoScan.Error(res.mensaje)
                }
            }
        } else {
            if (estado !is EstadoScan.Exito) {
                estado = EstadoScan.Espera
            }
        }
    }

    fun lanzarEscaner() {
        val integrator = IntentIntegrator(context as Activity).apply {
            setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            setPrompt("Apunta al código QR de la web")
            setBeepEnabled(true)
            setOrientationLocked(false)
        }
        scanLauncher.launch(integrator.createScanIntent())
    }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Acceso web") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (val s = estado) {

                EstadoScan.Espera -> {
                    Text("☕", fontSize = 56.sp)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text       = "Autorizar acceso web",
                        style      = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text      = "Escanea el código QR que aparece\nen la pantalla del dashboard.",
                        textAlign = TextAlign.Center,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(32.dp))
                    Button(
                        onClick  = ::lanzarEscaner,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Escanear QR")
                    }
                }

                EstadoScan.Cargando -> {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Autorizando y sincronizando...")
                }

                EstadoScan.Exito -> {
                    Text("✅", fontSize = 56.sp)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text       = "¡Acceso autorizado!",
                        style      = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text      = "La web ya tiene acceso al dashboard\ndurante 24 horas.",
                        textAlign = TextAlign.Center,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(32.dp))
                    OutlinedButton(
                        onClick  = onVolver,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Volver")
                    }
                }

                is EstadoScan.Error -> {
                    Text("❌", fontSize = 56.sp)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text       = "No se pudo autorizar",
                        style      = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text      = s.mensaje,
                        textAlign = TextAlign.Center,
                        color     = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(32.dp))
                    Button(
                        onClick  = { estado = EstadoScan.Espera },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Intentar de nuevo")
                    }
                }
            }
        }
    }
}

private sealed class EstadoScan {
    object Espera   : EstadoScan()
    object Cargando : EstadoScan()
    object Exito    : EstadoScan()
    data class Error(val mensaje: String) : EstadoScan()
}