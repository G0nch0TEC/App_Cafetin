package com.proyecto.cafetin.ui.historial

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.proyecto.cafetin.CafetinApp
import com.proyecto.cafetin.data.model.Movimiento
import com.proyecto.cafetin.data.model.TipoMovimiento
import com.proyecto.cafetin.ui.UiUtils.iniciales
import com.proyecto.cafetin.ui.theme.*
import com.proyecto.cafetin.util.MoneyUtils.centavosAtexto
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistorialScreen(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as CafetinApp
    val vm: HistorialViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                HistorialViewModel(app.container.repository) as T
        }
    )

    val diaActual  by vm.diaActual.collectAsState()
    val grupos     by vm.gruposFiltrados.collectAsState()
    val resumen    by vm.resumenDia.collectAsState()
    val esHoy      by vm.esHoy.collectAsState()
    val busqueda   by vm.busqueda.collectAsState()

    val sdfTitulo = remember { SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es")) }
    val sdfHora   = remember { SimpleDateFormat("h:mm a", Locale("es")) }

    val tituloDia = remember(diaActual, esHoy) {
        val base = sdfTitulo.format(Date(diaActual)).replaceFirstChar { it.uppercase() }
        if (esHoy) "Hoy · $base" else base
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
                            IconButton(onClick = vm::diaAnterior) {
                                Text("←", fontSize = 22.sp, color = PrimaryColor, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                tituloDia,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = vm::diaSiguiente, enabled = !esHoy) {
                                Text(
                                    "→", fontSize = 22.sp,
                                    color = if (esHoy) TextGray else PrimaryColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // ── Buscador ─────────────────────────────────────────
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
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
                        Spacer(Modifier.height(8.dp))
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
                                    "No hay fiados ni pagos registrados\npara este día."
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

// ── Tarjeta colapsable por persona ───────────────────────────────────────────

@Composable
private fun GrupoPersonaCard(
    grupo: GrupoPersona,
    sdfHora: SimpleDateFormat,
    onDelete: (Movimiento) -> Unit
) {
    var expandido by remember(grupo.persona.id) { mutableStateOf(false) }

    val netoPers   = grupo.totalFiado - grupo.totalCobrado
    val chipBg     = if (netoPers > 0) DebtRedBg  else OkGreenBg
    val chipFg     = if (netoPers > 0) DebtRed    else OkGreen
    val chipTxt    = if (netoPers > 0) netoPers.centavosAtexto() else "Al día"

    Surface(
        color  = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp)
    ) {
        Column {
            // Cabecera de grupo — toca para colapsar/expandir
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

                // Nombre + descripción
                Column(Modifier.weight(1f)) {
                    Text(grupo.persona.nombre, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    if (grupo.persona.descripcion.isNotBlank())
                        Text(grupo.persona.descripcion, fontSize = 12.sp, color = TextGray)
                }

                // Chip neto de la persona en el día
                Box(
                    Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(chipBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(chipTxt, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = chipFg)
                }

                // Ícono expandir/colapsar
                Icon(
                    if (expandido) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = TextGray,
                    modifier = Modifier.size(20.dp)
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

    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 68.dp, end = 8.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Ícono pequeño tipo movimiento
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

        IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = TextGray, modifier = Modifier.size(15.dp))
        }
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
