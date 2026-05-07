package com.proyecto.cafetin.ui.personas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.proyecto.cafetin.CafetinApp
import com.proyecto.cafetin.data.model.Persona
import com.proyecto.cafetin.sync.SyncManager
import com.proyecto.cafetin.ui.UiUtils.iniciales
import com.proyecto.cafetin.ui.theme.*
import com.proyecto.cafetin.util.MoneyUtils.centavosAtexto
import com.proyecto.cafetin.util.fuzzyMatch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

private val EnviadoBg = Color(0xFFFFF3E0)
private val EnviadoFg = Color(0xFFE65100)
private val AFavorBg  = Color(0xFFE3F2FD)
private val AFavorFg  = Color(0xFF1565C0)

@Composable
fun PersonasScreen(
    onPersonaClick: (Int) -> Unit,
    onHistorialClick: () -> Unit,
) {
    val context    = LocalContext.current
    val repository = (context.applicationContext as CafetinApp).container.repository
    val vm: PersonasViewModel = viewModel(factory = PersonasViewModel.Factory(repository))

    val personas         by vm.personas.collectAsState()
    val saldoTotal       by vm.saldoTotal.collectAsState()
    val cobradoHoy       by vm.cobradoHoy.collectAsState()
    val totalAFavor      by vm.totalAFavor.collectAsState()
    val saldosPorPersona by vm.saldosPorPersona.collectAsState()
    val busqueda         by vm.busqueda.collectAsState()
    val orden            by vm.orden.collectAsState()
    val filtro           by vm.filtro.collectAsState()
    val error            by vm.error.collectAsState()

    var mostrarDialogo       by remember { mutableStateOf(false) }
    var personaAEliminar     by remember { mutableStateOf<Persona?>(null) }
    var personaQuitarEnviado by remember { mutableStateOf<Persona?>(null) }
    var mostrarOrden         by remember { mutableStateOf(false) }

    // Estado del sync manual
    var sincronizando by remember { mutableStateOf(false) }
    val scope         = rememberCoroutineScope()
    val syncManager   = remember { SyncManager(context) }

    val listState         = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearError()
        }
    }

    val mostrarFab by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 || !listState.isScrollInProgress
        }
    }

    val ahora    = System.currentTimeMillis()
    val fechaHoy = remember {
        SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es")).format(Date())
    }

    val personasFiltradas = remember(personas, busqueda, orden, filtro, saldosPorPersona, ahora) {
        personas
            .filter { p ->
                val nombreOk    = fuzzyMatch(p.nombre, busqueda)
                val saldo       = saldosPorPersona[p.id] ?: 0L
                val estaEnviado = p.enviadoHasta > ahora
                val estadoOk = when (filtro) {
                    FiltroPersonas.TODOS     -> true
                    FiltroPersonas.CON_DEUDA -> saldo > 0
                    FiltroPersonas.AL_DIA    -> saldo == 0L
                    FiltroPersonas.ENVIADO   -> estaEnviado
                    FiltroPersonas.A_FAVOR   -> saldo < 0
                }
                nombreOk && estadoOk
            }
            .let { lista ->
                when (orden) {
                    OrdenPersonas.NOMBRE         -> lista.sortedBy { it.nombre.lowercase() }
                    OrdenPersonas.MAYOR_DEUDA    -> lista.sortedByDescending { saldosPorPersona[it.id] ?: 0L }
                    OrdenPersonas.AL_DIA_PRIMERO -> lista.sortedBy { saldosPorPersona[it.id] ?: 0L }
                }
            }
    }

    Scaffold(
        containerColor   = Color(0xFFF4F2F8),
        snackbarHost     = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            AnimatedVisibility(
                visible = mostrarFab,
                enter   = slideInVertically(initialOffsetY = { it * 2 }),
                exit    = slideOutVertically(targetOffsetY = { it * 2 })
            ) {
                ExtendedFloatingActionButton(
                    onClick        = { mostrarDialogo = true },
                    icon           = { Icon(Icons.Default.Add, contentDescription = null) },
                    text           = { Text("Nueva persona") },
                    containerColor = PrimaryContainer,
                    contentColor   = OnPrimaryContainer
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header
            Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 0.dp) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Cafetín", fontSize = 24.sp, fontWeight = FontWeight.Medium)
                        Text(fechaHoy, fontSize = 12.sp, color = TextGray, modifier = Modifier.padding(top = 2.dp))
                    }

                    // Botón sync manual
                    IconButton(
                        onClick = {
                            scope.launch {
                                sincronizando = true
                                val result = syncManager.sincronizar()
                                sincronizando = false
                                val msg = if (result.isSuccess)
                                    result.getOrNull() ?: "Sincronizado"
                                else
                                    "✗ Error: ${result.exceptionOrNull()?.message}"
                                snackbarHostState.showSnackbar(msg)
                            }
                        },
                        enabled = !sincronizando
                    ) {
                        if (sincronizando) {
                            CircularProgressIndicator(
                                modifier  = Modifier.size(20.dp),
                                color     = PrimaryColor,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector        = Icons.Default.Upload,
                                contentDescription = "Sincronizar con web",
                                tint               = PrimaryColor
                            )
                        }
                    }

                    IconButton(onClick = onHistorialClick) {
                        Icon(
                            imageVector        = Icons.Default.DateRange,
                            contentDescription = "Ver historial",
                            tint               = PrimaryColor
                        )
                    }
                }
            }

            // Stats
            Surface(color = MaterialTheme.colorScheme.surface) {
                Row(
                    Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard("Pendiente",   saldoTotal.centavosAtexto(),  DebtRed,  Modifier.width(160.dp))
                    StatCard("A favor",     totalAFavor.centavosAtexto(), AFavorFg, Modifier.width(160.dp))
                    StatCard("Cobrado hoy", cobradoHoy.centavosAtexto(),  OkGreen,  Modifier.width(160.dp))
                }
            }

            // Búsqueda + Ordenar
            Surface(color = MaterialTheme.colorScheme.surface) {
                Column {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(28.dp))
                                .background(SurfaceGray)
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Search, contentDescription = null, tint = TextGray, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            TextField(
                                value         = busqueda,
                                onValueChange = vm::setBusqueda,
                                placeholder   = { Text("Buscar persona...", fontSize = 13.sp, color = TextGray) },
                                singleLine    = true,
                                colors        = TextFieldDefaults.colors(
                                    focusedContainerColor   = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor   = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                modifier  = Modifier.fillMaxWidth(),
                                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                            )
                        }

                        Box {
                            Surface(
                                shape    = RoundedCornerShape(28.dp),
                                color    = if (orden != OrdenPersonas.NOMBRE) PrimaryContainer else SurfaceGray,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(28.dp))
                                    .clickable { mostrarOrden = true }
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        "Ordenar",
                                        fontSize   = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color      = if (orden != OrdenPersonas.NOMBRE) OnPrimaryContainer else TextGray
                                    )
                                    Icon(
                                        Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint     = if (orden != OrdenPersonas.NOMBRE) OnPrimaryContainer else TextGray
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded         = mostrarOrden,
                                onDismissRequest = { mostrarOrden = false }
                            ) {
                                OrdenOpcion("Nombre A–Z",     OrdenPersonas.NOMBRE,         orden) { vm.setOrden(it); mostrarOrden = false }
                                OrdenOpcion("Mayor deuda",    OrdenPersonas.MAYOR_DEUDA,    orden) { vm.setOrden(it); mostrarOrden = false }
                                OrdenOpcion("Al día primero", OrdenPersonas.AL_DIA_PRIMERO, orden) { vm.setOrden(it); mostrarOrden = false }
                            }
                        }
                    }

                    // Chips de filtro
                    Row(
                        Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FiltroChip("Todos",     FiltroPersonas.TODOS,     filtro) { vm.setFiltro(it) }
                        FiltroChip("Con deuda", FiltroPersonas.CON_DEUDA, filtro) { vm.setFiltro(it) }
                        FiltroChip("Al día",    FiltroPersonas.AL_DIA,    filtro) { vm.setFiltro(it) }
                        FiltroChipColoreado(
                            label        = "A favor",
                            valor        = FiltroPersonas.A_FAVOR,
                            seleccionado = filtro,
                            activoBg     = AFavorFg,
                            inactivoFg   = AFavorFg,
                            onClick      = { vm.setFiltro(it) }
                        )
                        FiltroChipColoreado(
                            label        = "Enviado",
                            valor        = FiltroPersonas.ENVIADO,
                            seleccionado = filtro,
                            activoBg     = EnviadoFg,
                            inactivoFg   = EnviadoFg,
                            onClick      = { vm.setFiltro(it) }
                        )
                    }

                    Spacer(Modifier.height(4.dp))
                }
            }

            HorizontalDivider()

            if (busqueda.isNotBlank() || filtro != FiltroPersonas.TODOS || orden != OrdenPersonas.NOMBRE) {
                Text(
                    text = when {
                        personasFiltradas.isEmpty() -> "Sin resultados"
                        else -> "${personasFiltradas.size} persona${if (personasFiltradas.size != 1) "s" else ""}"
                    },
                    fontSize = 11.sp,
                    color    = TextGray,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            LazyColumn(
                state    = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                if (personasFiltradas.isEmpty()) {
                    item {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 56.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                when (filtro) {
                                    FiltroPersonas.CON_DEUDA -> "Nadie debe nada por ahora ✓"
                                    FiltroPersonas.AL_DIA    -> "Todos tienen deuda pendiente"
                                    FiltroPersonas.ENVIADO   -> "Ningún reporte enviado hoy aún"
                                    FiltroPersonas.A_FAVOR   -> "Nadie tiene saldo a favor"
                                    else                     -> "No se encontró \"$busqueda\""
                                },
                                fontSize = 14.sp,
                                color    = TextGray
                            )
                        }
                    }
                }
                items(personasFiltradas, key = { it.id }) { persona ->
                    val saldo       = saldosPorPersona[persona.id] ?: 0L
                    val estaEnviado = persona.enviadoHasta > ahora
                    PersonaRow(
                        persona         = persona,
                        saldo           = saldo,
                        estaEnviado     = estaEnviado,
                        onClick         = { onPersonaClick(persona.id) },
                        onDelete        = { personaAEliminar = persona },
                        onQuitarEnviado = { personaQuitarEnviado = persona }
                    )
                    HorizontalDivider(color = Color(0xFFE7E0EC))
                }
            }
        }
    }

    if (mostrarDialogo) {
        AgregarPersonaDialog(
            onDismiss = { mostrarDialogo = false },
            onConfirm = { nombre, desc ->
                vm.agregarPersona(nombre, desc)
                mostrarDialogo = false
            }
        )
    }

    personaAEliminar?.let { persona ->
        EliminarPersonaDialog(
            persona   = persona,
            onDismiss = { personaAEliminar = null },
            onConfirm = {
                vm.eliminarPersona(persona)
                personaAEliminar = null
            }
        )
    }

    personaQuitarEnviado?.let { persona ->
        AlertDialog(
            onDismissRequest = { personaQuitarEnviado = null },
            title = { Text("Quitar marcador") },
            text  = { Text("¿Querés quitar el estado \"Enviado\" de ${persona.nombre}?") },
            confirmButton = {
                TextButton(onClick = {
                    vm.quitarEnviado(persona)
                    personaQuitarEnviado = null
                }) { Text("Quitar") }
            },
            dismissButton = {
                TextButton(onClick = { personaQuitarEnviado = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun FiltroChip(
    label: String,
    valor: FiltroPersonas,
    seleccionado: FiltroPersonas,
    onClick: (FiltroPersonas) -> Unit
) {
    val activo = seleccionado == valor
    Surface(
        shape    = RoundedCornerShape(100.dp),
        color    = if (activo) PrimaryColor else SurfaceGray,
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .clickable { onClick(valor) }
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (activo) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color.White)
            }
            Text(
                label,
                fontSize   = 12.sp,
                fontWeight = if (activo) FontWeight.SemiBold else FontWeight.Normal,
                color      = if (activo) Color.White else TextGray
            )
        }
    }
}

@Composable
private fun FiltroChipColoreado(
    label: String,
    valor: FiltroPersonas,
    seleccionado: FiltroPersonas,
    activoBg: Color,
    inactivoFg: Color,
    onClick: (FiltroPersonas) -> Unit
) {
    val activo = seleccionado == valor
    Surface(
        shape    = RoundedCornerShape(100.dp),
        color    = if (activo) activoBg else SurfaceGray,
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .clickable { onClick(valor) }
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (activo) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color.White)
            }
            Text(
                label,
                fontSize   = 12.sp,
                fontWeight = if (activo) FontWeight.SemiBold else FontWeight.Normal,
                color      = if (activo) Color.White else inactivoFg
            )
        }
    }
}

@Composable
private fun OrdenOpcion(
    label: String,
    valor: OrdenPersonas,
    seleccionado: OrdenPersonas,
    onClick: (OrdenPersonas) -> Unit
) {
    DropdownMenuItem(
        text = {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(label, fontSize = 13.sp)
                if (seleccionado == valor) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp), tint = PrimaryColor)
                }
            }
        },
        onClick = { onClick(valor) }
    )
}

@Composable
private fun StatCard(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceGray)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column {
            Text(label, fontSize = 11.sp, color = TextGray)
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Medium, color = valueColor)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PersonaRow(
    persona: Persona,
    saldo: Long,
    estaEnviado: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onQuitarEnviado: () -> Unit = {}
) {
    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onDelete)
            .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(PrimaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(iniciales(persona.nombre), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = OnPrimaryContainer)
        }

        Column(Modifier.weight(1f)) {
            Text(persona.nombre, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            if (persona.descripcion.isNotBlank())
                Text(persona.descripcion, fontSize = 12.sp, color = TextGray, modifier = Modifier.padding(top = 1.dp))
        }

        if (estaEnviado) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(EnviadoBg)
                    .clickable { onQuitarEnviado() }
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text("Enviado", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = EnviadoFg)
            }
        }

        when {
            saldo > 0 -> Box(
                Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(DebtRedBg)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) { Text(saldo.centavosAtexto(), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = DebtRed) }

            saldo < 0 -> Box(
                Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(AFavorBg)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) { Text("A favor ${(-saldo).centavosAtexto()}", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = AFavorFg) }

            else -> Box(
                Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(OkGreenBg)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) { Text("Al día", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = OkGreen) }
        }
    }
}

@Composable
private fun AgregarPersonaDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var nombre      by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva persona") },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value           = nombre,
                    onValueChange   = { nombre = it },
                    label           = { Text("Nombre") },
                    singleLine      = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier        = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value           = descripcion,
                    onValueChange   = { descripcion = it },
                    label           = { Text("Descripción (opcional)") },
                    singleLine      = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier        = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (nombre.isNotBlank()) onConfirm(nombre, descripcion) },
                enabled = nombre.isNotBlank()
            ) { Text("Agregar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun EliminarPersonaDialog(
    persona: Persona,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eliminar persona") },
        text  = {
            Text(
                "¿Estás seguro de que quieres eliminar a \"${persona.nombre}\"? " +
                "Se eliminará también todo su historial de movimientos. Esta acción no se puede deshacer."
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors  = ButtonDefaults.buttonColors(containerColor = DebtRed)
            ) { Text("Eliminar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
