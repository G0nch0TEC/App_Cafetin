package com.proyecto.cafetin.ui.detalle

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.proyecto.cafetin.CafetinApp
import com.proyecto.cafetin.data.model.CategoriaConProductos
import com.proyecto.cafetin.data.model.TipoMovimiento
import com.proyecto.cafetin.ui.theme.*
import com.proyecto.cafetin.util.MoneyUtils.centavosAtexto
import com.proyecto.cafetin.util.NotaUtils.notaBase

@Composable
fun DetalleScreen(
    personaId: Int,
    onBack: () -> Unit,
    onGestionarCatalogo: () -> Unit = {},
) {
    val app = LocalContext.current.applicationContext as CafetinApp
    val vm: DetalleViewModel = viewModel(factory = DetalleViewModel.Factory(app, personaId))
    val context = LocalContext.current

    val persona      by vm.persona.collectAsState()
    val saldo        by vm.saldo.collectAsState()
    val movimientos  by vm.movimientos.collectAsState()
    val exportState  by vm.exportState.collectAsState()
    val desdeMs      by vm.desdeMs.collectAsState()
    val hastaMs      by vm.hastaMs.collectAsState()
    val catalogoCats by vm.catalogoCategorias.collectAsState()

    var modalConfirm          by remember { mutableStateOf<Pair<String, Long>?>(null) }
    var mostrarModalParcial   by remember { mutableStateOf(false) }
    var mostrarModalManual    by remember { mutableStateOf(false) }
    var mostrarDialogoEditar  by remember { mutableStateOf(false) }
    var categoriaSeleccionada by remember { mutableStateOf<CategoriaConProductos?>(null) }

    val snackHost  = remember { SnackbarHostState() }
    @OptIn(ExperimentalMaterial3Api::class)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        vm.snackEvents.collect { msg -> snackHost.showSnackbar(msg) }
    }

    LaunchedEffect(Unit) {
        vm.eventos.collect { evento ->
            when (evento) {
                is DetalleEvent.CompartirPdf -> {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type     = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, evento.uri)
                        putExtra(Intent.EXTRA_SUBJECT, "Reporte de ${vm.persona.value?.nombre}")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Compartir reporte"))
                }
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFFF4F2F8),
        snackbarHost   = { SnackbarHost(snackHost) },
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 0.dp) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(56.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                    Column(Modifier.weight(1f)) {
                        Text(persona?.nombre ?: "...", fontSize = 17.sp, fontWeight = FontWeight.Medium)
                        if (!persona?.descripcion.isNullOrBlank())
                            Text(persona!!.descripcion, fontSize = 12.sp, color = TextGray)
                    }
                    IconButton(onClick = { mostrarDialogoEditar = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar cliente", tint = TextGray, modifier = Modifier.size(19.dp))
                    }
                    IconButton(onClick = { vm.abrirDialogoExport() }) {
                        Icon(
                            painter = painterResource(android.R.drawable.ic_menu_share),
                            contentDescription = "Exportar PDF",
                            tint = PrimaryColor
                        )
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {

            // ── Chip saldo ─────────────────────────────────────────────────
            item {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                        val chipBg  = when { saldo > 0 -> Color(0xFFF9DEDC); saldo < 0 -> Color(0xFFE3F2FD); else -> OkGreenBg }
                        val chipFg  = when { saldo > 0 -> Color(0xFF410E0B); saldo < 0 -> Color(0xFF1565C0); else -> OkGreen }
                        val chipTxt = when { saldo > 0 -> "Debe ${saldo.centavosAtexto()}"; saldo < 0 -> "A favor ${(-saldo).centavosAtexto()}"; else -> "Al día" }
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(chipBg)
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) { Text(chipTxt, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = chipFg) }
                    }
                }
            }

            // ── Grid de categorías ─────────────────────────────────────────
            item {
                DetalleCatalogoSection(
                    catalogoCats        = catalogoCats,
                    onCategoriaClick    = { cat -> categoriaSeleccionada = cat },
                    onGestionarCatalogo = onGestionarCatalogo,
                    onOtroProducto      = { mostrarModalManual = true }
                )
            }

            // ── Historial de hoy ───────────────────────────────────────────
            item {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    HorizontalDivider()
                    Column {
                        DetalleSectionHeader(if (movimientos.isEmpty()) "Sin movimientos hoy" else "Movimientos de hoy")
                        val MAX_VISIBLE = 5
                        val ROW_HEIGHT  = 62.dp
                        val listHeight  = ROW_HEIGHT * MAX_VISIBLE

                        if (movimientos.isNotEmpty()) {
                            val listState = rememberLazyListState()
                            LazyColumn(
                                state    = listState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(listHeight),
                                userScrollEnabled = true
                            ) {
                                items(movimientos, key = { it.id }) { mov ->
                                    val prodOrigen = if (mov.tipo == TipoMovimiento.FIADO)
                                        catalogoCats.flatMap { it.productos }
                                            .firstOrNull { it.nombre == notaBase(mov.nota) }
                                    else null
                                    MovimientoRow(
                                        mov         = mov,
                                        prodOrigen  = prodOrigen,
                                        onDelete    = { vm.eliminarMovimiento(mov) },
                                        onAumentar  = {
                                            if (prodOrigen != null)
                                                vm.acumularProducto(prodOrigen.nombre, prodOrigen.montoCentavos)
                                            else
                                                vm.registrarFiado(mov.monto, mov.nota)
                                        },
                                        onDisminuir = {
                                            if (prodOrigen != null)
                                                vm.reducirProducto(mov, prodOrigen.montoCentavos)
                                        }
                                    )
                                    HorizontalDivider(color = Color(0xFFE7E0EC))
                                }
                            }
                        }
                    }
                }
            }

            // ── Registrar pago ─────────────────────────────────────────────
            item {
                Surface(color = Color(0xFFF5F5F5)) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Text(
                            "Registrar pago",
                            fontSize = 12.sp, fontWeight = FontWeight.Medium,
                            color = PrimaryColor, letterSpacing = 0.04.sp
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment     = Alignment.Bottom
                        ) {
                            OutlinedButton(
                                onClick  = { mostrarModalParcial = true },
                                border   = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3B6D11)),
                                colors   = ButtonDefaults.outlinedButtonColors(contentColor = OkGreen),
                                modifier = Modifier.height(52.dp).weight(1f)
                            ) { Text("Pago a cuenta", fontSize = 13.sp, fontWeight = FontWeight.Medium) }

                            Button(
                                onClick = {
                                    if (saldo <= 0) { vm.enviarError("No hay deuda que cobrar"); return@Button }
                                    modalConfirm = "Cobrar todo" to saldo
                                },
                                enabled  = saldo > 0,
                                colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B6D11)),
                                modifier = Modifier.height(52.dp)
                            ) {
                                Text(
                                    if (saldo > 0) "Todo ${saldo.centavosAtexto()}" else "Sin deuda",
                                    fontSize = 13.sp, fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Diálogos ───────────────────────────────────────────────────────────
    if (mostrarDialogoEditar) {
        EditarPersonaDialog(
            nombreInicial      = persona?.nombre ?: "",
            descripcionInicial = persona?.descripcion ?: "",
            onDismiss          = { mostrarDialogoEditar = false },
            onConfirm          = { nombre, descripcion ->
                vm.editarPersona(nombre, descripcion)
                mostrarDialogoEditar = false
            }
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    categoriaSeleccionada?.let { catConProds ->
        ProductosCategoriaSheet(
            catConProds  = catConProds,
            sheetState   = sheetState,
            onDismiss    = { categoriaSeleccionada = null },
            onProductoClick = { prod -> vm.acumularProducto(prod.nombre, prod.montoCentavos) }
        )
    }

    if (mostrarModalManual) {
        OtroProductoDialog(
            onDismiss = { mostrarModalManual = false },
            onConfirm = { desc, monto ->
                vm.registrarFiado((monto * 100).toLong(), desc)
                mostrarModalManual = false
            },
            onError = { msg -> vm.enviarError(msg) }
        )
    }

    if (mostrarModalParcial) {
        PagoACuentaDialog(
            nombrePersona = persona?.nombre ?: "",
            onDismiss     = { mostrarModalParcial = false },
            onConfirm     = { centavos ->
                mostrarModalParcial = false
                modalConfirm = "Pago a cuenta" to centavos
            },
            onError = { msg -> vm.enviarError(msg) }
        )
    }

    modalConfirm?.let { (label, centavos) ->
        AlertDialog(
            onDismissRequest = { modalConfirm = null },
            icon = {
                Box(
                    Modifier.size(48.dp).clip(CircleShape).background(OkGreenBg),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.Add, contentDescription = null, tint = OkGreen, modifier = Modifier.size(24.dp)) }
            },
            title = { Text(label, textAlign = TextAlign.Center) },
            text  = {
                Text(
                    "¿Registrar pago de ${centavos.centavosAtexto()} de ${persona?.nombre}?",
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = { vm.registrarPago(centavos); modalConfirm = null },
                    colors  = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B6D11))
                ) { Text("Confirmar") }
            },
            dismissButton = {
                OutlinedButton(onClick = { modalConfirm = null }) { Text("Cancelar") }
            }
        )
    }

    if (exportState.mostrando) {
        ExportarPdfDialog(
            desdeMs    = desdeMs,
            hastaMs    = hastaMs,
            generando  = exportState.generando,
            error      = exportState.error,
            onSetDesde = { vm.setDesde(it) },
            onSetHasta = { vm.setHasta(it) },
            onExportar = { vm.exportarPdf() },
            onDismiss  = { vm.cerrarDialogoExport() }
        )
    }
}

// ── Composable de apoyo (compartido entre archivos del package) ───────────────

@Composable
internal fun DetalleSectionHeader(title: String) {
    Text(
        title, fontSize = 12.sp, fontWeight = FontWeight.Medium,
        color = PrimaryColor, letterSpacing = 0.04.sp,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
    )
}
