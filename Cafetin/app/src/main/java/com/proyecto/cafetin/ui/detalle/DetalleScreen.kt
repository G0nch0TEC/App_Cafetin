package com.proyecto.cafetin.ui.detalle

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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
import com.proyecto.cafetin.data.model.Movimiento
import com.proyecto.cafetin.data.model.TipoMovimiento
import com.proyecto.cafetin.ui.personas.*
import java.text.SimpleDateFormat
import java.util.*

// ── Modelos ───────────────────────────────────────────────────────────────────

data class ProductoRapido(val nombre: String, val montoCentavos: Long) {
    val precioTexto get() = "S/${montoCentavos / 100}.${ "%02d".format(montoCentavos % 100) }"
}

data class CategoriaProductos(
    val nombre: String,
    val emoji: String,
    val productos: List<ProductoRapido>
)

val CATEGORIAS_PRODUCTOS = listOf(
    CategoriaProductos("Bebidas", "🥤", listOf(
        ProductoRapido("Refresco",   100), ProductoRapido("Agua",        100),
        ProductoRapido("Agua c/Gas", 150), ProductoRapido("Jugo Papaya",        300),
        ProductoRapido("Sporade",   300), ProductoRapido("Chicha",       200),
        ProductoRapido("Gaseosas frutales",       300), ProductoRapido("Guaranita",       200),
    )),
    CategoriaProductos("Comida", "🍽️", listOf(
        ProductoRapido("Comida",      800), ProductoRapido("Papa Rellena",  500),
        ProductoRapido("Empanada",       400), ProductoRapido("Comidita",       500),
    )),
    CategoriaProductos("Snacks", "🍿", listOf(
        ProductoRapido("Chifles",       100), ProductoRapido("Inka Chips",        250),
        ProductoRapido("Cheetos",          200), ProductoRapido("Canchita",       100),
        ProductoRapido("Trixito",  150), ProductoRapido("Cuates",  200),
        ProductoRapido("Papa Lays",       250), ProductoRapido("Cereal",       100),
        ProductoRapido("Mani",       100), ProductoRapido("Abas",       100),
        ProductoRapido("Marshmellow",       200),
    )),
    CategoriaProductos("Antojos", "😋", listOf(
        ProductoRapido("Champa", 150), ProductoRapido("Pizza", 500),
        ProductoRapido("Pionono",        150), ProductoRapido("Keke",        200),
        ProductoRapido("Pie Manzana",      500), ProductoRapido("Cupcake",     150),
        ProductoRapido("Alfajor",       100), ProductoRapido("Ensalada Fruta",       500),
    )),
    CategoriaProductos("Galletas", "🍪", listOf(
        ProductoRapido("Galleta",      100), ProductoRapido("Galleta",   200),
        ProductoRapido("Galleta",    150),
    )),
    CategoriaProductos("Dulces", "🍬", listOf(
        ProductoRapido("Fruna", 150), ProductoRapido("3x Hals",  100),
        ProductoRapido("Oca Loca",  100), ProductoRapido("3x Adams",  100),
        ProductoRapido("Chin Chin",     150), ProductoRapido("Menta",   100),
        ProductoRapido("Caramelo Acido",       50), ProductoRapido("Barrilete",       50),
    )),
    CategoriaProductos("Fríos", "🧊", listOf(
        ProductoRapido("Gelatina",         100), ProductoRapido("Chupete",        100),
        ProductoRapido("Yogurt",       300), ProductoRapido("Pulp",       250),
        ProductoRapido("Chocolatada",       250),
    )),
    CategoriaProductos("Panes", "🍞", listOf(
        ProductoRapido("Sandwiches",  400), ProductoRapido("Pan Pizza", 500),
        ProductoRapido("Triple", 500),
    )),
    CategoriaProductos("Utiles", "✏️", listOf(
        ProductoRapido("Corrector", 300), ProductoRapido("Tajador", 250),
        ProductoRapido("Borrador", 200), ProductoRapido("Lapiz", 200),
        ProductoRapido("Lapicero", 350),
    )),
)

// ── Pantalla principal ────────────────────────────────────────────────────────

@Composable
fun DetalleScreen(
    personaId: Int,
    onBack: () -> Unit
) {
    val app = LocalContext.current.applicationContext as Application
    val vm: DetalleViewModel = viewModel(factory = DetalleViewModel.Factory(app, personaId))
    val context = LocalContext.current

    val persona     by vm.persona.collectAsState()
    val saldo       by vm.saldo.collectAsState()
    val movimientos by vm.movimientos.collectAsState()
    val exportState by vm.exportState.collectAsState()
    val desdeMs     by vm.desdeMs.collectAsState()
    val hastaMs     by vm.hastaMs.collectAsState()

    var pagoInput    by remember { mutableStateOf("") }
    var manualDesc   by remember { mutableStateOf("") }
    var manualMonto  by remember { mutableStateOf("") }
    var modalConfirm by remember { mutableStateOf<Pair<String, Long>?>(null) }

    val snackHost = remember { SnackbarHostState() }

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
                        val chipBg  = if (saldo > 0) Color(0xFFF9DEDC) else OkGreenBg
                        val chipFg  = if (saldo > 0) Color(0xFF410E0B) else OkGreen
                        val chipTxt = if (saldo > 0) "Debe ${saldo.centavosATexto()}" else "Al día"
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(chipBg)
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) { Text(chipTxt, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = chipFg) }
                    }
                }
            }

            // ── Categorías expandibles ─────────────────────────────────────
            item {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    Column {
                        SectionHeader("Anotar fiado rápido")
                        CategoriasExpandibles(
                            categorias  = CATEGORIAS_PRODUCTOS,
                            movimientos = movimientos,
                            onProductoClick = { prod ->
                                // Toque → acumula directamente en BD (INSERT o UPDATE)
                                vm.acumularProducto(prod.nombre, prod.montoCentavos)
                            }
                        )
                        Spacer(Modifier.height(14.dp))
                    }
                }
            }

            // ── Otro producto (manual) ─────────────────────────────────────
            item {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    Column {
                        HorizontalDivider()
                        SectionHeader("Otro producto")
                        OtroProductoCard(
                            desc          = manualDesc,
                            monto         = manualMonto,
                            onDescChange  = { manualDesc = it },
                            onMontoChange = { manualMonto = it },
                            onAnotar = {
                                val m = manualMonto.toDoubleOrNull()
                                when {
                                    manualDesc.isBlank() || m == null || m <= 0 ->
                                        vm.enviarError("Completa descripción y monto")
                                    else -> {
                                        vm.registrarFiado((m * 100).toLong(), manualDesc)
                                        manualDesc  = ""
                                        manualMonto = ""
                                    }
                                }
                            }
                        )
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
                // Busca el precio unitario si el movimiento viene de una categoría
                val prodOrigen = if (mov.tipo == TipoMovimiento.FIADO)
                    CATEGORIAS_PRODUCTOS.flatMap { it.productos }
                        .firstOrNull { it.nombre == notaBaseDeNota(mov.nota) }
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
                            Column(Modifier.weight(1f)) {
                                Text("Monto S/", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = PrimaryColor)
                                Spacer(Modifier.height(3.dp))
                                OutlinedTextField(
                                    value         = pagoInput,
                                    onValueChange = { pagoInput = it },
                                    placeholder   = { Text("0.00", fontSize = 14.sp) },
                                    singleLine    = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier      = Modifier.height(52.dp),
                                    textStyle     = LocalTextStyle.current.copy(fontSize = 14.sp)
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    val m = pagoInput.toDoubleOrNull()
                                    if (m == null || m <= 0) { vm.enviarError("Ingresa un monto de pago"); return@OutlinedButton }
                                    modalConfirm = "Pago parcial" to minOf((m * 100).toLong(), saldo)
                                },
                                border  = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3B6D11)),
                                colors  = ButtonDefaults.outlinedButtonColors(contentColor = OkGreen),
                                modifier = Modifier.height(52.dp)
                            ) { Text("Parcial", fontSize = 13.sp, fontWeight = FontWeight.Medium) }

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
                                    if (saldo > 0) "Todo ${saldo.centavosATexto()}" else "Sin deuda",
                                    fontSize = 13.sp, fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
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
                    "¿Registrar pago de ${centavos.centavosATexto()} de ${persona?.nombre}?",
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.registrarPago(centavos)
                        pagoInput    = ""
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

// ── Categorías Expandibles ────────────────────────────────────────────────────

@Composable
private fun CategoriasExpandibles(
    categorias: List<CategoriaProductos>,
    movimientos: List<Movimiento>,
    onProductoClick: (ProductoRapido) -> Unit
) {
    var categoriaAbierta by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        categorias.forEach { categoria ->
            val estaAbierta = categoriaAbierta == categoria.nombre
            CategoriaCard(
                categoria       = categoria,
                expandida       = estaAbierta,
                movimientos     = movimientos,
                onToggle        = { categoriaAbierta = if (estaAbierta) null else categoria.nombre },
                onProductoClick = onProductoClick
            )
        }
    }
}

@Composable
private fun CategoriaCard(
    categoria: CategoriaProductos,
    expandida: Boolean,
    movimientos: List<Movimiento>,
    onToggle: () -> Unit,
    onProductoClick: (ProductoRapido) -> Unit
) {
    val rotacion by animateFloatAsState(targetValue = if (expandida) 180f else 0f, label = "rot")

    Surface(
        shape    = RoundedCornerShape(12.dp),
        color    = if (expandida) PrimaryContainer else Color(0xFFF5F5F5),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Cabecera
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(categoria.emoji, fontSize = 20.sp)
                    Text(
                        categoria.nombre,
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        color = if (expandida) OnPrimaryContainer else Color(0xFF1C1B1F)
                    )
                    Text(
                        "${categoria.productos.size} items", fontSize = 11.sp,
                        color = if (expandida) OnPrimaryContainer.copy(alpha = 0.6f) else TextGray
                    )
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expandida) "Colapsar" else "Expandir",
                    modifier = Modifier.size(20.dp).rotate(rotacion),
                    tint = if (expandida) OnPrimaryContainer else TextGray
                )
            }

            // Lista de productos
            AnimatedVisibility(visible = expandida, enter = expandVertically(), exit = shrinkVertically()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFAF7FF))
                        .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HorizontalDivider(color = PrimaryContainer, thickness = 1.dp)
                    Spacer(Modifier.height(2.dp))
                    categoria.productos.chunked(2).forEach { fila ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            fila.forEach { prod ->
                                ProductoCardConBadge(
                                    prod     = prod,
                                    onClick  = { onProductoClick(prod) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (fila.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductoCardConBadge(
    prod: ProductoRapido,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        onClick  = onClick,
        modifier = modifier,
        shape    = RoundedCornerShape(10.dp),
        colors   = CardDefaults.outlinedCardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(width = 1.dp, color = PrimaryContainer)
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text(prod.nombre, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1C1B1F))
                Text(
                    prod.precioTexto, fontSize = 12.sp, color = PrimaryColor,
                    modifier = Modifier.padding(top = 2.dp), fontWeight = FontWeight.SemiBold
                )
            }
    }
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
    val montoTexto = if (esPago) "+${mov.monto.centavosATexto()}" else "−${mov.monto.centavosATexto()}"
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
private fun ExportarPdfDialog(
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

// ── Composables de apoyo ──────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        title, fontSize = 12.sp, fontWeight = FontWeight.Medium,
        color = PrimaryColor, letterSpacing = 0.04.sp,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
    )
}

@Composable
private fun OtroProductoCard(
    desc: String, monto: String,
    onDescChange: (String) -> Unit, onMontoChange: (String) -> Unit,
    onAnotar: () -> Unit
) {
    Box(
        Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF5F5F5))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("+ Ingresar manualmente", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = PrimaryColor)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(2f)) {
                    Text("Descripción", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = PrimaryColor)
                    Spacer(Modifier.height(3.dp))
                    OutlinedTextField(
                        value = desc, onValueChange = onDescChange,
                        placeholder = { Text("Ej: Pan de molde", fontSize = 13.sp) },
                        singleLine  = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.height(52.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text("Monto S/", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = PrimaryColor)
                    Spacer(Modifier.height(3.dp))
                    OutlinedTextField(
                        value = monto, onValueChange = onMontoChange,
                        placeholder = { Text("0.00", fontSize = 13.sp) },
                        singleLine  = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.height(52.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                    )
                }
                Button(
                    onClick  = onAnotar,
                    modifier = Modifier.height(52.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
                ) { Text("Anotar", fontSize = 13.sp) }
            }
        }
    }
}

// ── Helpers de parsing de nota (espejo de los del ViewModel) ─────────────────

internal fun notaBaseDeNota(nota: String): String =
    Regex(""" x\d+$""").replace(nota, "").trim()

internal fun cantidadDeNota(nota: String): Int =
    Regex(""" x(\d+)$""").find(nota)?.groupValues?.get(1)?.toIntOrNull() ?: 1
