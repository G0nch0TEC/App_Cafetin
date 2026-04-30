package com.proyecto.cafetin.ui.detalle

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
import com.proyecto.cafetin.data.model.CatalogoProducto
import com.proyecto.cafetin.data.model.Movimiento
import com.proyecto.cafetin.data.model.TipoMovimiento
import com.proyecto.cafetin.ui.theme.*
import com.proyecto.cafetin.util.DateUtils
import com.proyecto.cafetin.util.MoneyUtils.centavosAtexto
import java.text.SimpleDateFormat
import java.util.*

// ── MovimientoRow ─────────────────────────────────────────────────────────────

@Composable
internal fun MovimientoRow(
    mov: Movimiento,
    prodOrigen: CatalogoProducto?,
    onDelete: () -> Unit,
    onAumentar: () -> Unit,
    onDisminuir: () -> Unit,
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

        if (!esPago && prodOrigen != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    shape    = CircleShape,
                    color    = Color(0xFFF0EBF8),
                    modifier = Modifier.size(28.dp).clip(CircleShape).clickable(onClick = onDisminuir)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("−", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextGray)
                    }
                }
                Text(montoTexto, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = montoColor, modifier = Modifier.widthIn(min = 54.dp), textAlign = TextAlign.Center)
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

// ── Diálogo: exportar PDF ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportarPdfDialog(
    desdeMs: Long, hastaMs: Long, generando: Boolean, error: String?,
    onSetDesde: (Long) -> Unit, onSetHasta: (Long) -> Unit,
    onExportar: () -> Unit, onDismiss: () -> Unit,
) {
    val sdf = remember { SimpleDateFormat("dd/MM/yyyy", Locale("es")) }
    var mostrarDesde by remember { mutableStateOf(false) }
    var mostrarHasta by remember { mutableStateOf(false) }
    val desdePickerState = rememberDatePickerState(initialSelectedDateMillis = desdeMs)
    val hastaPickerState = rememberDatePickerState(initialSelectedDateMillis = hastaMs - DateUtils.UN_DIA_MS)

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
                    Text("Hasta: ${sdf.format(Date(hastaMs - DateUtils.UN_DIA_MS))}", fontSize = 14.sp)
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
        confirmButton    = { TextButton(onClick = { desdePickerState.selectedDateMillis?.let { onSetDesde(it) }; mostrarDesde = false }) { Text("Aceptar") } },
        dismissButton    = { TextButton(onClick = { mostrarDesde = false }) { Text("Cancelar") } }
    ) { DatePicker(state = desdePickerState) }

    if (mostrarHasta) DatePickerDialog(
        onDismissRequest = { mostrarHasta = false },
        confirmButton    = { TextButton(onClick = { hastaPickerState.selectedDateMillis?.let { onSetHasta(it) }; mostrarHasta = false }) { Text("Aceptar") } },
        dismissButton    = { TextButton(onClick = { mostrarHasta = false }) { Text("Cancelar") } }
    ) { DatePicker(state = hastaPickerState) }
}

// ── Diálogo: editar cliente ───────────────────────────────────────────────────

@Composable
internal fun EditarPersonaDialog(
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
                    value          = nombre,
                    onValueChange  = { nombre = it; errorNombre = false },
                    label          = { Text("Nombre") },
                    singleLine     = true,
                    isError        = errorNombre,
                    supportingText = if (errorNombre) {{ Text("El nombre no puede estar vacío") }} else null,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier       = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value          = descripcion,
                    onValueChange  = { descripcion = it },
                    label          = { Text("Descripción (opcional)") },
                    placeholder    = { Text("Ej: 5to B, Profesora, etc.") },
                    singleLine     = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier       = Modifier.fillMaxWidth()
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
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

// ── Diálogo: producto manual ("Otro producto") ────────────────────────────────

@Composable
internal fun OtroProductoDialog(
    onDismiss: () -> Unit,
    onConfirm: (desc: String, monto: Double) -> Unit,
    onError:   (String) -> Unit,
) {
    var desc  by remember { mutableStateOf("") }
    var monto by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Otro producto") },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Ingresá descripción y monto del producto a fiarse.", fontSize = 13.sp, color = TextGray)
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
                        desc.isBlank()      -> onError("Ingresá una descripción")
                        m == null || m <= 0 -> onError("Ingresá un monto válido")
                        else                -> onConfirm(desc, m)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
            ) { Text("Anotar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

// ── Diálogo: pago a cuenta ────────────────────────────────────────────────────

@Composable
internal fun PagoACuentaDialog(
    nombrePersona: String,
    onDismiss: () -> Unit,
    onConfirm: (centavos: Long) -> Unit,
    onError:   (String) -> Unit,
) {
    var montoParcialInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { onDismiss(); montoParcialInput = "" },
        title = { Text("Pago a cuenta", textAlign = TextAlign.Center) },
        text  = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Ingresa el monto recibido de $nombrePersona",
                    textAlign = TextAlign.Center,
                    fontSize  = 14.sp,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value           = montoParcialInput,
                    onValueChange   = { montoParcialInput = it },
                    placeholder     = { Text("0.00") },
                    prefix          = { Text("S/ ") },
                    singleLine      = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier        = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val m = montoParcialInput.toDoubleOrNull()
                    if (m == null || m <= 0) { onError("Ingresa un monto válido"); return@Button }
                    montoParcialInput = ""
                    onConfirm((m * 100).toLong())
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B6D11))
            ) { Text("Pagar") }
        },
        dismissButton = {
            OutlinedButton(onClick = { onDismiss(); montoParcialInput = "" }) { Text("Cancelar") }
        }
    )
}
