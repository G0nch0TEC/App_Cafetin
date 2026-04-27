package com.proyecto.cafetin.ui.detalle

import android.app.Application
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.proyecto.cafetin.CafetinApp
import com.proyecto.cafetin.data.ProductosCatalogo
import com.proyecto.cafetin.data.model.Movimiento
import com.proyecto.cafetin.data.model.Producto.ProductoRapido
import com.proyecto.cafetin.data.model.Producto.CategoriaProductos
import com.proyecto.cafetin.data.model.TipoMovimiento
import com.proyecto.cafetin.ui.theme.*
import com.proyecto.cafetin.util.MoneyUtils.centavosAtexto
import com.proyecto.cafetin.util.NotaUtils.notaBase
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

// ── Pantalla principal ────────────────────────────────────────────────────────
@Composable
fun DetalleScreen(
    personaId: Int,
    onBack: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as CafetinApp
    val vm: DetalleViewModel = viewModel(factory = DetalleViewModel.Factory(app, personaId))
    val context = LocalContext.current

    val persona     by vm.persona.collectAsState()
    val saldo       by vm.saldo.collectAsState()
    val movimientos by vm.movimientos.collectAsState()
    val exportState by vm.exportState.collectAsState()
    val desdeMs     by vm.desdeMs.collectAsState()
    val hastaMs     by vm.hastaMs.collectAsState()

    var modalConfirm       by remember { mutableStateOf<Pair<String, Long>?>(null) }
    var mostrarModalParcial by remember { mutableStateOf(false) }
    var mostrarModalManual by remember { mutableStateOf(false) }
    var mostrarDialogoEditar by remember { mutableStateOf(false) }
    var categoriaSeleccionada by remember { mutableStateOf<CategoriaProductos?>(null) }

    val snackHost  = remember { SnackbarHostState() }
    val scope      = rememberCoroutineScope()
    @OptIn(ExperimentalMaterial3Api::class)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        vm.snackEvents.collect { msg -> snackHost.showSnackbar(msg) }
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
                Surface(color = MaterialTheme.colorScheme.surface) {
                    Column {
                        SectionHeader("Anotar fiado rápido")
                        CategoriasGrid(
                            categorias = ProductosCatalogo.categorias,
                            onCategoriaClick = { cat -> categoriaSeleccionada = cat }
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(
                                onClick = { mostrarModalManual = true },
                                shape  = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryColor),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryColor),
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Otro producto", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }

            // ── Historial de hoy ───────────────────────────────────────────
            item {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    HorizontalDivider()
                    SectionHeader(if (movimientos.isEmpty()) "Sin movimientos hoy" else "Movimientos de hoy")
                }
            }

            items(movimientos, key = { it.id }) { mov ->
                val prodOrigen = if (mov.tipo == TipoMovimiento.FIADO)
                    ProductosCatalogo.categorias.flatMap { it.productos }
                        .firstOrNull { it.nombre == notaBase(mov.nota) }
                else null

                Surface(color = MaterialTheme.colorScheme.surface) {
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
                                onClick = { mostrarModalParcial = true },
                                border  = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3B6D11)),
                                colors  = ButtonDefaults.outlinedButtonColors(contentColor = OkGreen),
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

    // ── Diálogo editar cliente ─────────────────────────────────────────────
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

    // ── BottomSheet productos de categoría ────────────────────────────────
    @OptIn(ExperimentalMaterial3Api::class)
    categoriaSeleccionada?.let { cat ->
        ModalBottomSheet(
            onDismissRequest = { categoriaSeleccionada = null },
            sheetState       = sheetState,
            containerColor   = MaterialTheme.colorScheme.surface,
            shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(cat.emoji, fontSize = 24.sp)
                    Text(cat.nombre, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                }
                IconButton(onClick = { categoriaSeleccionada = null }) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = TextGray)
                }
            }
            HorizontalDivider(color = Color(0xFFE7E0EC))
            LazyVerticalGrid(
                columns        = GridCells.Fixed(2),
                modifier       = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
                verticalArrangement   = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(cat.productos) { prod ->
                    ProductoCardFlash(
                        prod    = prod,
                        onClick = { vm.acumularProducto(prod.nombre, prod.montoCentavos) }
                    )
                }
            }
        }
    }

    // ── Modal producto manual ──────────────────────────────────────────────
    if (mostrarModalManual) {
        OtroProductoDialog(
            onDismiss = { mostrarModalManual = false },
            onConfirm = { desc, monto ->
                vm.registrarFiado((monto * 100).toLong(), desc)
                mostrarModalManual = false
            },
            onError   = { msg -> vm.enviarError(msg) }
        )
    }

    // ── Modal pago a cuenta ───────────────────────────────────────────────────
    if (mostrarModalParcial) {
        var montoParcialInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { mostrarModalParcial = false; montoParcialInput = "" },
            title = { Text("Pago a cuenta", textAlign = TextAlign.Center) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Ingresa el monto recibido de ${persona?.nombre ?: ""}",
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value         = montoParcialInput,
                        onValueChange = { montoParcialInput = it },
                        placeholder   = { Text("0.00") },
                        prefix        = { Text("S/ ") },
                        singleLine    = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier      = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val m = montoParcialInput.toDoubleOrNull()
                        if (m == null || m <= 0) { vm.enviarError("Ingresa un monto válido"); return@Button }
                        mostrarModalParcial = false
                        montoParcialInput = ""
                        modalConfirm = "Pago a cuenta" to (m * 100).toLong()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B6D11))
                ) { Text("Pagar") }
            },
            dismissButton = {
                OutlinedButton(onClick = { mostrarModalParcial = false; montoParcialInput = "" }) { Text("Cancelar") }
            }
        )
    }

        // ── Modal confirmación pago ────────────────────────────────────────────
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
                    onClick = {
                        vm.registrarPago(centavos)
                        modalConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B6D11))
                ) { Text("Confirmar") }
            },
            dismissButton = {
                OutlinedButton(onClick = { modalConfirm = null }) { Text("Cancelar") }
            }
        )
    }

    // ── Diálogo exportar PDF ───────────────────────────────────────────────
    if (exportState.mostrando) {
        ExportarPdfDialog(
            desdeMs    = desdeMs,
            hastaMs    = hastaMs,
            generando  = exportState.generando,
            error      = exportState.error,
            onSetDesde = { vm.setDesde(it) },
            onSetHasta = { vm.setHasta(it) },
            onExportar = { vm.exportarPdf(context) },
            onDismiss  = { vm.cerrarDialogoExport() }
        )
    }
}

// ── Grid de categorías ────────────────────────────────────────────────────────

@Composable
private fun CategoriasGrid(
    categorias: List<CategoriaProductos>,
    onCategoriaClick: (CategoriaProductos) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categorias.chunked(3).forEach { fila ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                fila.forEach { cat ->
                    Surface(
                        shape    = RoundedCornerShape(12.dp),
                        color    = Color(0xFFF5F0FF),
                        border   = androidx.compose.foundation.BorderStroke(1.dp, PrimaryContainer),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onCategoriaClick(cat) }
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(cat.emoji, fontSize = 22.sp, lineHeight = 24.sp)
                            Text(
                                cat.nombre,
                                fontSize   = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color      = Color(0xFF3C3489),
                                textAlign  = TextAlign.Center,
                                maxLines   = 1
                            )
                        }
                    }
                }
                // Relleno si la última fila tiene menos de 3
                repeat(3 - fila.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

// ── Card de producto con flash al tocar ───────────────────────────────────────

@Composable
private fun ProductoCardFlash(
    prod: ProductoRapido,
    onClick: () -> Unit,
) {
    var flashing by remember { mutableStateOf(false) }
    val bgColor by animateColorAsState(
        targetValue = if (flashing) PrimaryContainer else Color.White,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 300),
        finishedListener = { flashing = false },
        label = "flash"
    )

    OutlinedCard(
        onClick = {
            onClick()
            flashing = true
        },
        shape  = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = bgColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryContainer)
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                prod.nombre,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Medium,
                color      = Color(0xFF1C1B1F)
            )
            Text(
                prod.precioTexto,
                fontSize   = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color      = PrimaryColor,
                modifier   = Modifier.padding(top = 3.dp)
            )
        }
    }
}

// ── Modal producto manual ─────────────────────────────────────────────────────

@Composable
private fun OtroProductoDialog(
    onDismiss: () -> Unit,
    onConfirm: (desc: String, monto: Double) -> Unit,
    onError:   (String) -> Unit
) {
    var desc  by remember { mutableStateOf("") }
    var monto by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Otro producto") },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Ingresá descripción y monto del producto a fiarse.",
                    fontSize = 13.sp,
                    color    = TextGray
                )
                OutlinedTextField(
                    value           = desc,
                    onValueChange   = { desc = it },
                    label           = { Text("Descripción") },
                    placeholder     = { Text("Ej: Pan de molde") },
                    singleLine      = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier        = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value           = monto,
                    onValueChange   = { monto = it },
                    label           = { Text("Monto S/") },
                    placeholder     = { Text("0.00") },
                    singleLine      = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier        = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val m = monto.toDoubleOrNull()
                    when {
                        desc.isBlank()        -> onError("Ingresá una descripción")
                        m == null || m <= 0   -> onError("Ingresá un monto válido")
                        else                  -> onConfirm(desc, m)
                    }
                },
                colors  = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
            ) { Text("Anotar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// ── MovimientoRow ─────────────────────────────────────────────────────────────

@Composable
private fun MovimientoRow(
    mov: Movimiento,
    prodOrigen: ProductoRapido?,   // null si es manual o pago
    onDelete: () -> Unit,
    onAumentar: () -> Unit,
    onDisminuir: () -> Unit
) {
    val esPago     = mov.tipo == TipoMovimiento.PAGO
    val iconBg     = if (esPago) OkGreenBg else Color(0xFFF9DEDC)
    val iconFg     = if (esPago) OkGreen   else Color(0xFF410E0B)
    val montoTexto = if (esPago) "+${mov.monto.centavosAtexto()}" else "−${mov.monto.centavosAtexto()}"
    val montoColor = if (esPago) OkGreen   else DebtRed
    val sdf        = remember { SimpleDateFormat("d MMM · h:mm a", Locale("es")) }
    val fecha      = remember(mov.fecha) { sdf.format(Date(mov.fecha)) }

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(iconBg), contentAlignment = Alignment.Center) {
            Text(if (esPago) "↑" else "↓", color = iconFg, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        Column(Modifier.weight(1f)) {
            Text(mov.nota.ifBlank { if (esPago) "Pago" else "Fiado" }, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(fecha, fontSize = 10.sp, color = TextGray, modifier = Modifier.padding(top = 1.dp))
        }

        // Botones −/+ solo para fiados de categoría (tienen prodOrigen)
        if (!esPago && prodOrigen != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Botón −
                Surface(
                    shape    = CircleShape,
                    color    = Color(0xFFF0EBF8),
                    modifier = Modifier.size(28.dp).clip(CircleShape).clickable(onClick = onDisminuir)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("−", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextGray)
                    }
                }

                Text(
                    montoTexto,
                    fontSize = 12.sp, fontWeight = FontWeight.Medium, color = montoColor,
                    modifier = Modifier.widthIn(min = 54.dp), textAlign = TextAlign.Center
                )

                // Botón +
                Surface(
                    shape    = CircleShape,
                    color    = Color(0xFFEDE7FF),
                    modifier = Modifier.size(28.dp).clip(CircleShape).clickable(onClick = onAumentar)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PrimaryColor)
                    }
                }
            }
        } else {
            Text(montoTexto, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = montoColor)
        }

        IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = TextGray, modifier = Modifier.size(15.dp))
        }
    }
}

// ── Diálogo exportar PDF ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportarPdfDialog(
    desdeMs: Long, hastaMs: Long, generando: Boolean, error: String?,
    onSetDesde: (Long) -> Unit, onSetHasta: (Long) -> Unit,
    onExportar: () -> Unit, onDismiss: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("dd/MM/yyyy", Locale("es")) }
    var mostrarDesde by remember { mutableStateOf(false) }
    var mostrarHasta by remember { mutableStateOf(false) }
    val desdePickerState = rememberDatePickerState(initialSelectedDateMillis = desdeMs)
    val hastaPickerState = rememberDatePickerState(initialSelectedDateMillis = hastaMs)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Exportar PDF") },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Elige el período de movimientos a incluir en el reporte.", fontSize = 13.sp, color = TextGray)
                OutlinedButton(onClick = { mostrarDesde = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Desde: ${sdf.format(Date(desdeMs))}", fontSize = 14.sp)
                }
                OutlinedButton(onClick = { mostrarHasta = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Hasta: ${sdf.format(Date(hastaMs))}", fontSize = 14.sp)
                }
                if (error != null) Text(error, fontSize = 12.sp, color = DebtRed)
                if (generando) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text("Generando PDF...", fontSize = 13.sp, color = TextGray)
                }
            }
        },
        confirmButton = {
            Button(onClick = onExportar, enabled = !generando, colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)) {
                Text("Generar y compartir")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !generando) { Text("Cancelar") } }
    )

    if (mostrarDesde) DatePickerDialog(
        onDismissRequest = { mostrarDesde = false },
        confirmButton = { TextButton(onClick = { desdePickerState.selectedDateMillis?.let { onSetDesde(it) }; mostrarDesde = false }) { Text("Aceptar") } },
        dismissButton = { TextButton(onClick = { mostrarDesde = false }) { Text("Cancelar") } }
    ) { DatePicker(state = desdePickerState) }

    if (mostrarHasta) DatePickerDialog(
        onDismissRequest = { mostrarHasta = false },
        confirmButton = { TextButton(onClick = { hastaPickerState.selectedDateMillis?.let { onSetHasta(it) }; mostrarHasta = false }) { Text("Aceptar") } },
        dismissButton = { TextButton(onClick = { mostrarHasta = false }) { Text("Cancelar") } }
    ) { DatePicker(state = hastaPickerState) }
}

// ── Diálogo editar cliente ────────────────────────────────────────────────────

@Composable
private fun EditarPersonaDialog(
    nombreInicial: String,
    descripcionInicial: String,
    onDismiss: () -> Unit,
    onConfirm: (nombre: String, descripcion: String) -> Unit,
) {
    var nombre      by remember { mutableStateOf(nombreInicial) }
    var descripcion by remember { mutableStateOf(descripcionInicial) }
    var errorNombre by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar cliente") },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value         = nombre,
                    onValueChange = { nombre = it; errorNombre = false },
                    label         = { Text("Nombre") },
                    singleLine    = true,
                    isError       = errorNombre,
                    supportingText = if (errorNombre) {{ Text("El nombre no puede estar vacío") }} else null,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier      = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value         = descripcion,
                    onValueChange = { descripcion = it },
                    label         = { Text("Descripción (opcional)") },
                    placeholder   = { Text("Ej: 5to B, Profesora, etc.") },
                    singleLine    = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier      = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nombre.isBlank()) { errorNombre = true; return@Button }
                    onConfirm(nombre, descripcion)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// ── Composables de apoyo ──────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        title, fontSize = 12.sp, fontWeight = FontWeight.Medium,
        color = PrimaryColor, letterSpacing = 0.04.sp,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
    )
}
