package com.proyecto.cafetin.ui.catalogo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.proyecto.cafetin.CafetinApp
import com.proyecto.cafetin.data.model.CatalogoCategoria
import com.proyecto.cafetin.data.model.CatalogoProducto
import com.proyecto.cafetin.ui.theme.*
import androidx.compose.ui.platform.LocalContext

// ── Pantalla principal Catálogo ───────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogoScreen(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as CafetinApp
    val vm: CatalogoViewModel = viewModel(factory = CatalogoViewModel.Factory(app))

    val categorias            by vm.categorias.collectAsState()
    val categoriaSeleccionada by vm.categoriaSeleccionada.collectAsState()
    val productos             by vm.productosDeCategoriaActual.collectAsState()
    val snackHost             = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        vm.snackEvents.collect { msg -> snackHost.showSnackbar(msg) }
    }

    // Estados de diálogos
    var mostrarDialogoNuevaCategoria  by remember { mutableStateOf(false) }
    var categoriaAEditar              by remember { mutableStateOf<CatalogoCategoria?>(null) }
    var categoriaAEliminar            by remember { mutableStateOf<CatalogoCategoria?>(null) }
    var mostrarDialogoNuevoProducto   by remember { mutableStateOf(false) }
    var productoAEditar               by remember { mutableStateOf<CatalogoProducto?>(null) }
    var productoAEliminar             by remember { mutableStateOf<CatalogoProducto?>(null) }

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
                    if (categoriaSeleccionada != null) {
                        IconButton(onClick = { vm.seleccionarCategoria(null) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                        Text(
                            "${categoriaSeleccionada!!.emoji}  ${categoriaSeleccionada!!.nombre}",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                        Text(
                            "Administrar catálogo",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (categoriaSeleccionada != null) mostrarDialogoNuevoProducto = true
                    else mostrarDialogoNuevaCategoria = true
                },
                containerColor = PrimaryColor,
                contentColor   = Color.White,
                shape          = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar")
            }
        }
    ) { padding ->

        // ── Vista: Lista de categorías ─────────────────────────────────────
        AnimatedVisibility(
            visible = categoriaSeleccionada == null,
            enter = fadeIn(), exit = fadeOut()
        ) {
            if (categorias.isEmpty()) {
                EmptyState(
                    mensaje = "No hay categorías aún.\nToca + para crear la primera.",
                    modifier = Modifier.padding(padding)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(bottom = 96.dp, top = 8.dp)
                ) {
                    item {
                        Text(
                            "CATEGORÍAS (${categorias.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextGray,
                            letterSpacing = 0.08.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(categorias, key = { it.id }) { cat ->
                        CategoriaRow(
                            cat = cat,
                            onClick        = { vm.seleccionarCategoria(cat) },
                            onEdit         = { categoriaAEditar = cat },
                            onDelete       = { categoriaAEliminar = cat }
                        )
                        HorizontalDivider(color = Color(0xFFE7E0EC), modifier = Modifier.padding(start = 60.dp))
                    }
                }
            }
        }

        // ── Vista: Productos de la categoría seleccionada ──────────────────
        AnimatedVisibility(
            visible = categoriaSeleccionada != null,
            enter = fadeIn(), exit = fadeOut()
        ) {
            if (categoriaSeleccionada != null) {
                if (productos.isEmpty()) {
                    EmptyState(
                        mensaje = "Esta categoría no tiene productos.\nToca + para agregar uno.",
                        modifier = Modifier.padding(padding)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentPadding = PaddingValues(bottom = 96.dp, top = 8.dp)
                    ) {
                        item {
                            Text(
                                "PRODUCTOS (${productos.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextGray,
                                letterSpacing = 0.08.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        items(productos, key = { it.id }) { prod ->
                            ProductoRow(
                                prod     = prod,
                                onEdit   = { productoAEditar = prod },
                                onDelete = { productoAEliminar = prod }
                            )
                            HorizontalDivider(color = Color(0xFFE7E0EC), modifier = Modifier.padding(start = 16.dp))
                        }
                    }
                }
            }
        }
    }

    // ── Diálogos CRUD ─────────────────────────────────────────────────────────

    if (mostrarDialogoNuevaCategoria) {
        DialogCategoria(
            titulo    = "Nueva categoría",
            onDismiss = { mostrarDialogoNuevaCategoria = false },
            onConfirm = { nombre, emoji ->
                vm.agregarCategoria(nombre, emoji)
                mostrarDialogoNuevaCategoria = false
            }
        )
    }

    categoriaAEditar?.let { cat ->
        DialogCategoria(
            titulo          = "Editar categoría",
            nombreInicial   = cat.nombre,
            emojiInicial    = cat.emoji,
            onDismiss       = { categoriaAEditar = null },
            onConfirm       = { nombre, emoji ->
                vm.editarCategoria(cat, nombre, emoji)
                categoriaAEditar = null
            }
        )
    }

    categoriaAEliminar?.let { cat ->
        DialogConfirmarEliminar(
            mensaje   = "¿Eliminar \"${cat.nombre}\"? También se borrarán todos sus productos.",
            onDismiss = { categoriaAEliminar = null },
            onConfirm = {
                vm.eliminarCategoria(cat)
                categoriaAEliminar = null
            }
        )
    }

    val catActual = categoriaSeleccionada
    if (mostrarDialogoNuevoProducto && catActual != null) {
        DialogProducto(
            titulo    = "Nuevo producto",
            onDismiss = { mostrarDialogoNuevoProducto = false },
            onConfirm = { nombre, monto ->
                vm.agregarProducto(catActual.id, nombre, monto)
                mostrarDialogoNuevoProducto = false
            }
        )
    }

    productoAEditar?.let { prod ->
        DialogProducto(
            titulo          = "Editar producto",
            nombreInicial   = prod.nombre,
            montoInicial    = prod.montoCentavos,
            onDismiss       = { productoAEditar = null },
            onConfirm       = { nombre, monto ->
                vm.editarProducto(prod, nombre, monto)
                productoAEditar = null
            }
        )
    }

    productoAEliminar?.let { prod ->
        DialogConfirmarEliminar(
            mensaje   = "¿Eliminar \"${prod.nombre}\" del catálogo?",
            onDismiss = { productoAEliminar = null },
            onConfirm = {
                vm.eliminarProducto(prod)
                productoAEliminar = null
            }
        )
    }
}

// ── Fila de categoría ─────────────────────────────────────────────────────────
// Toque simple → entrar a productos | Toque largo → eliminar | Ícono lápiz → editar

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoriaRow(
    cat: CatalogoCategoria,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick    = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDelete()
                }
            )
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Emoji en círculo
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF5F0FF)),
                contentAlignment = Alignment.Center
            ) {
                Text(cat.emoji, fontSize = 20.sp)
            }

            Column(Modifier.weight(1f)) {
                Text(cat.nombre, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text("Mantén para eliminar", fontSize = 10.sp, color = TextGray)
            }

            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = TextGray, modifier = Modifier.size(17.dp))
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = TextGray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ── Fila de producto ──────────────────────────────────────────────────────────
// Toque simple → editar | Toque largo → eliminar

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProductoRow(
    prod: CatalogoProducto,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick    = onEdit,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDelete()
                }
            )
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(prod.nombre, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(prod.precioTexto, fontSize = 13.sp, color = PrimaryColor, fontWeight = FontWeight.SemiBold)
                Text("Toca para editar · Mantén para eliminar", fontSize = 10.sp, color = TextGray)
            }
        }
    }
}

// ── Diálogo: crear/editar categoría ──────────────────────────────────────────

@Composable
private fun DialogCategoria(
    titulo: String,
    nombreInicial: String = "",
    emojiInicial: String  = "",
    onDismiss: () -> Unit,
    onConfirm: (nombre: String, emoji: String) -> Unit
) {
    var nombre by remember { mutableStateOf(nombreInicial) }
    var emoji  by remember { mutableStateOf(emojiInicial) }
    var error  by remember { mutableStateOf(false) }

    // Emojis sugeridos para elegir rápido
    val emojisSugeridos = listOf("🥤","🍽️","🍿","😋","🍪","🍬","🧊","🍞","✏️","🎒","🥗","🍕","☕","🎮","🧁","🌮")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titulo) },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value           = nombre,
                    onValueChange   = { nombre = it; error = false },
                    label           = { Text("Nombre") },
                    isError         = error,
                    supportingText  = if (error) {{ Text("El nombre no puede estar vacío") }} else null,
                    singleLine      = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier        = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value           = emoji,
                    onValueChange   = { if (it.length <= 2) emoji = it },
                    label           = { Text("Emoji") },
                    placeholder     = { Text("📦") },
                    singleLine      = true,
                    modifier        = Modifier.fillMaxWidth()
                )
                Text("Sugeridos:", fontSize = 11.sp, color = TextGray)
                // Grid de emojis sugeridos
                val filas = emojisSugeridos.chunked(8)
                filas.forEach { fila ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        fila.forEach { e ->
                            Box(
                                Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (emoji == e) PrimaryContainer else Color(0xFFF5F0FF))
                                    .clickable { emoji = e },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(e, fontSize = 18.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nombre.isBlank()) { error = true; return@Button }
                    onConfirm(nombre, emoji)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// ── Diálogo: crear/editar producto ───────────────────────────────────────────

@Composable
private fun DialogProducto(
    titulo: String,
    nombreInicial: String = "",
    montoInicial: Long    = 0L,
    onDismiss: () -> Unit,
    onConfirm: (nombre: String, montoCentavos: Long) -> Unit
) {
    var nombre      by remember { mutableStateOf(nombreInicial) }
    var montoTexto  by remember { mutableStateOf(if (montoInicial > 0) "%.2f".format(java.util.Locale.US, montoInicial / 100.0) else "") }
    var errorNombre by remember { mutableStateOf(false) }
    var errorMonto  by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titulo) },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value           = nombre,
                    onValueChange   = { nombre = it; errorNombre = false },
                    label           = { Text("Nombre del producto") },
                    placeholder     = { Text("Ej: Jugo de mango") },
                    isError         = errorNombre,
                    supportingText  = if (errorNombre) {{ Text("Ingresa un nombre") }} else null,
                    singleLine      = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier        = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value           = montoTexto,
                    onValueChange   = { montoTexto = it; errorMonto = false },
                    label           = { Text("Precio S/") },
                    placeholder     = { Text("0.00") },
                    prefix          = { Text("S/ ") },
                    isError         = errorMonto,
                    supportingText  = if (errorMonto) {{ Text("Ingresa un precio válido") }} else null,
                    singleLine      = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier        = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val monto = montoTexto.replace(',', '.').toDoubleOrNull()
                    if (nombre.isBlank()) { errorNombre = true; return@Button }
                    if (monto == null || monto <= 0) { errorMonto = true; return@Button }
                    onConfirm(nombre, (monto * 100).toLong())
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// ── Diálogo: confirmar eliminar ───────────────────────────────────────────────

@Composable
private fun DialogConfirmarEliminar(
    mensaje: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFFCEBEB)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = DebtRed, modifier = Modifier.size(24.dp))
            }
        },
        title = { Text("Eliminar", textAlign = TextAlign.Center) },
        text  = { Text(mensaje, textAlign = TextAlign.Center, fontSize = 14.sp) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors  = ButtonDefaults.buttonColors(containerColor = DebtRed)
            ) { Text("Eliminar") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// ── Estado vacío ──────────────────────────────────────────────────────────────

@Composable
private fun EmptyState(mensaje: String, modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📦", fontSize = 48.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                mensaje,
                fontSize   = 14.sp,
                color      = TextGray,
                textAlign  = TextAlign.Center,
                lineHeight = 22.sp
            )
        }
    }
}
