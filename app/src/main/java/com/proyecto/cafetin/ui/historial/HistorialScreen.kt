package com.proyecto.cafetin.ui.historial

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.proyecto.cafetin.CafetinApp
import com.proyecto.cafetin.data.model.Movimiento
import com.proyecto.cafetin.data.model.TipoMovimiento
import com.proyecto.cafetin.ui.UiUtils.iniciales
import com.proyecto.cafetin.ui.theme.*
import com.proyecto.cafetin.util.MoneyUtils.centavosAtexto
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialScreen(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as CafetinApp
    val vm: HistorialViewModel = viewModel(
        factory = HistorialViewModel.Factory(app.container.repository)
    )

    val diaActual  by vm.diaActual.collectAsState()
    val grupos     by vm.gruposFiltrados.collectAsState()
    val resumen    by vm.resumenDia.collectAsState()
    val esHoy      by vm.esHoy.collectAsState()
    val busqueda   by vm.busqueda.collectAsState()
    val filtroTipo by vm.filtroTipo.collectAsState()

    val sdfTitulo = remember { SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es")) }
    val sdfAnio   = remember { SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", Locale("es")) }
    val sdfHora   = remember { SimpleDateFormat("h:mm a", Locale("es")) }

    // Mostrar año en el título solo si no es el año actual
    val anioActual = remember { Calendar.getInstance().get(Calendar.YEAR) }
    val anioDelDia = remember(diaActual) {
        Calendar.getInstance().apply { timeInMillis = diaActual }.get(Calendar.YEAR)
    }

    val tituloDia = remember(diaActual, esHoy) {
        val fmt  = if (anioDelDia != anioActual) sdfAnio else sdfTitulo
        val base = fmt.format(Date(diaActual)).replaceFirstChar { it.uppercase() }
        if (esHoy) "Hoy · $base" else base
    }

    // DatePicker para saltar a un día concreto
    var mostrarDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = diaActual,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                // No permitir fechas futuras (comparar en UTC medianoche)
                val hoyUtcMidnight = run {
                    val cal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
                    cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0);      cal.set(Calendar.MILLISECOND, 0)
                    cal.timeInMillis
                }
                return utcTimeMillis <= hoyUtcMidnight
            }
        }
    )

    if (mostrarDatePicker) {
        DatePickerDialog(
            onDismissRequest = { mostrarDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { vm.irADia(it) }
                    mostrarDatePicker = false
                }) { Text("Ir a este día") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        containerColor = Color(0xFFF4F2F8),
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
                    Text(
                        "Historial",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    if (!esHoy) {
                        TextButton(onClick = vm::irAHoy) {
                            Text("Hoy", color = PrimaryColor, fontSize = 14.sp)
                        }
                    }
                    // Botón calendario para saltar a cualquier día
                    IconButton(onClick = { mostrarDatePicker = true }) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = "Ir a fecha",
                            tint = PrimaryColor
                        )
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {

            // ── Navegación de día ────────────────────────────────────────────
            item {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    Column {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(onClick = vm::diaAnterior) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Día anterior",
                                    tint = PrimaryColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Anterior", color = PrimaryColor, fontSize = 13.sp)
                            }

                            Text(
                                tituloDia,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )

                            if (esHoy) {
                                Text(
                                    "Hoy",
                                    fontSize = 13.sp,
                                    color = TextGray,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )
                            } else {
                                TextButton(onClick = vm::diaSiguiente) {
                                    Text("Siguiente", color = PrimaryColor, fontSize = 13.sp)
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Día siguiente",
                                        tint = PrimaryColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // ── Buscador ─────────────────────────────────────────
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .clip(RoundedCornerShape(28.dp))
                                .background(SurfaceGray)
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = TextGray,
                                modifier = Modifier.size(18.dp)
                            )
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

                        // ── Chips de filtro por tipo ──────────────────────────
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FiltroTipoChip("Todos",  FiltroTipo.TODOS,  filtroTipo) { vm.setFiltroTipo(it) }
                            FiltroTipoChip("Fiados", FiltroTipo.FIADOS, filtroTipo) { vm.setFiltroTipo(it) }
                            FiltroTipoChip("Pagos",  FiltroTipo.PAGOS,  filtroTipo) { vm.setFiltroTipo(it) }
                        }
                    }
                }
            }

            // ── Resumen del día ──────────────────────────────────────────────
            item {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    Column {
                        HorizontalDivider()
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ResumenCard("Fiado del día",  resumen.totalFiado.centavosAtexto(),   DebtRed, Modifier.weight(1f))
                            ResumenCard("Cobrado",        resumen.totalCobrado.centavosAtexto(), OkGreen, Modifier.weight(1f))
                        }
                        if (resumen.totalFiado > 0 || resumen.totalCobrado > 0) {
                            val netoColor = if (resumen.neto > 0) DebtRed else OkGreen
                            val netoLabel = if (resumen.neto > 0) "Pendiente del día" else "Día saldado ✓"
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .padding(bottom = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(netoLabel, fontSize = 12.sp, color = TextGray)
                                Text(resumen.neto.centavosAtexto(), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = netoColor)
                            }
                        }
                    }
                }
            }

            // ── Encabezado sección personas ──────────────────────────────────
            item {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    HorizontalDivider()
                    Text(
                        if (grupos.isEmpty()) {
                            if (busqueda.isBlank()) "Sin movimientos este día"
                            else "Sin resultados para \"$busqueda\""
                        } else {
                            "${grupos.size} persona${if (grupos.size != 1) "s" else ""} con movimientos"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = PrimaryColor,
                        letterSpacing = 0.04.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                    )
                }
            }

            // ── Estado vacío ─────────────────────────────────────────────────
            if (grupos.isEmpty()) {
                item {
                    Surface(color = MaterialTheme.colorScheme.surface) {
                        Box(
                            Modifier.fillMaxWidth().padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (busqueda.isBlank())
                                    "No hay movimientos registrados\npara este día."
                                else
                                    "No se encontró ninguna persona\ncon ese nombre.",
                                fontSize = 14.sp,
                                color = TextGray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // ── Grupos por persona ───────────────────────────────────────────
            items(grupos, key = { it.persona.id }) { grupo ->
                GrupoPersonaCard(
                    grupo   = grupo,
                    sdfHora = sdfHora,
                    onDelete = { mov -> vm.eliminarMovimiento(mov) }
                )
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

// ── Chip de filtro por tipo ───────────────────────────────────────────────────

@Composable
private fun FiltroTipoChip(
    label: String,
    valor: FiltroTipo,
    seleccionado: FiltroTipo,
    onClick: (FiltroTipo) -> Unit
) {
    val activo = seleccionado == valor
    val bg     = when {
        activo && valor == FiltroTipo.FIADOS -> Color(0xFFF9DEDC)
        activo && valor == FiltroTipo.PAGOS  -> Color(0xFFD7F3E3)
        activo                               -> PrimaryColor
        else                                 -> SurfaceGray
    }
    val fg = when {
        activo && valor == FiltroTipo.FIADOS -> DebtRed
        activo && valor == FiltroTipo.PAGOS  -> OkGreen
        activo                               -> Color.White
        else                                 -> TextGray
    }
    Box(
        Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(bg)
            .clickable { onClick(valor) }
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = fg)
    }
}

// ── Tarjeta colapsable por persona ───────────────────────────────────────────

@Composable
private fun GrupoPersonaCard(
    grupo: GrupoPersona,
    sdfHora: SimpleDateFormat,
    onDelete: (Movimiento) -> Unit
) {
    var expandido by remember(grupo.persona.id) { mutableStateOf(false) }

    val netoDia = grupo.totalFiado - grupo.totalCobrado

    // Chip del día: muestra lo que pasó HOY con esta persona
    val chipDiaBg  = when {
        netoDia > 0 -> Color(0xFFF9DEDC)
        netoDia < 0 -> Color(0xFFD7F3E3)
        else        -> Color(0xFFE8F5E9)
    }
    val chipDiaFg  = when {
        netoDia > 0 -> DebtRed
        netoDia < 0 -> OkGreen
        else        -> OkGreen
    }
    val chipDiaTxt = when {
        netoDia > 0 -> "+${netoDia.centavosAtexto()}"
        netoDia < 0 -> "Pagó ${(-netoDia).centavosAtexto()}"
        else        -> "Saldado"
    }

    // Saldo total real (histórico acumulado)
    val saldoReal = grupo.saldoReal
    val saldoRealTxt = when {
        saldoReal > 0 -> "Debe en total: ${saldoReal.centavosAtexto()}"
        saldoReal < 0 -> "A favor: ${(-saldoReal).centavosAtexto()}"
        else          -> "Sin deuda total"
    }
    val saldoRealColor = when {
        saldoReal > 0 -> DebtRed
        saldoReal < 0 -> OkGreen
        else          -> TextGray
    }

    Surface(
        color  = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Cabecera de grupo
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { expandido = !expandido }
                    .padding(horizontal = 16.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Avatar con iniciales
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(PrimaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        iniciales(grupo.persona.nombre),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = OnPrimaryContainer
                    )
                }

                // Nombre + descripción + saldo total real
                Column(Modifier.weight(1f)) {
                    Text(grupo.persona.nombre, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    if (grupo.persona.descripcion.isNotBlank())
                        Text(grupo.persona.descripcion, fontSize = 12.sp, color = TextGray)
                    // Saldo real siempre visible — el contexto que faltaba
                    Text(saldoRealTxt, fontSize = 11.sp, color = saldoRealColor)
                }

                // Chip de lo que pasó en el día
                Box(
                    Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(chipDiaBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(chipDiaTxt, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = chipDiaFg)
                }

                Icon(
                    if (expandido) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expandido) "Ocultar movimientos" else "Ver movimientos",
                    tint = PrimaryColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Movimientos colapsables
            AnimatedVisibility(
                visible = expandido,
                enter   = expandVertically(),
                exit    = shrinkVertically()
            ) {
                Column {
                    grupo.movimientos.forEach { mov ->
                        HorizontalDivider(color = Color(0xFFE7E0EC))
                        MovimientoFilaCompacta(
                            mov      = mov,
                            horaFmt  = remember(mov.fecha) { sdfHora.format(Date(mov.fecha)) },
                            onDelete = { onDelete(mov) }
                        )
                    }
                }
            }

            HorizontalDivider()
        }
    }
}

// ── Fila de movimiento con long-press para eliminar ──────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MovimientoFilaCompacta(
    mov: Movimiento,
    horaFmt: String,
    onDelete: () -> Unit
) {
    val esPago     = mov.tipo == TipoMovimiento.PAGO
    val montoTexto = if (esPago) "+${mov.monto.centavosAtexto()}" else "−${mov.monto.centavosAtexto()}"
    val montoColor = if (esPago) OkGreen else DebtRed
    val iconBg     = if (esPago) OkGreenBg else Color(0xFFF9DEDC)
    val iconFg     = if (esPago) OkGreen   else Color(0xFF410E0B)

    var mostrarConfirmacion by remember { mutableStateOf(false) }

    if (mostrarConfirmacion) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacion = false },
            title   = { Text("¿Eliminar movimiento?") },
            text    = { Text("Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    mostrarConfirmacion = false
                    onDelete()
                }) {
                    Text("Eliminar", color = DebtRed, fontWeight = FontWeight.Medium)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacion = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick     = {},
                onLongClick = { mostrarConfirmacion = true }
            )
            .padding(start = 68.dp, end = 16.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            Modifier.size(28.dp).clip(CircleShape).background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Text(if (esPago) "↑" else "↓", color = iconFg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }

        Column(Modifier.weight(1f)) {
            Text(
                mov.nota.ifBlank { if (esPago) "Pago" else "Fiado" },
                fontSize = 13.sp
            )
            Text(horaFmt, fontSize = 11.sp, color = TextGray)
        }

        Text(montoTexto, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = montoColor)
    }
}

// ── Composables auxiliares ───────────────────────────────────────────────────

@Composable
private fun ResumenCard(label: String, valor: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceGray)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column {
            Text(label, fontSize = 11.sp, color = TextGray)
            Spacer(Modifier.height(4.dp))
            Text(valor, fontSize = 20.sp, fontWeight = FontWeight.Medium, color = color)
        }
    }
}
