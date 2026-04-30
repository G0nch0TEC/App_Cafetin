package com.proyecto.cafetin.ui.detalle

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proyecto.cafetin.data.model.CatalogoProducto
import com.proyecto.cafetin.data.model.CategoriaConProductos
import com.proyecto.cafetin.ui.theme.*

// ── Sección completa del catálogo (header + grid + botón "Otro producto") ─────

@Composable
internal fun DetalleCatalogoSection(
    catalogoCats: List<CategoriaConProductos>,
    onCategoriaClick: (CategoriaConProductos) -> Unit,
    onGestionarCatalogo: () -> Unit,
    onOtroProducto: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 4.dp),
                verticalAlignment         = Alignment.CenterVertically,
                horizontalArrangement     = Arrangement.SpaceBetween
            ) {
                Text(
                    "Anotar fiado rápido",
                    fontSize      = 12.sp,
                    fontWeight    = FontWeight.Medium,
                    color         = PrimaryColor,
                    letterSpacing = 0.04.sp,
                    modifier      = Modifier.padding(vertical = 14.dp)
                )
                IconButton(
                    onClick  = onGestionarCatalogo,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Gestionar catálogo",
                        tint               = TextGray,
                        modifier           = Modifier.size(18.dp)
                    )
                }
            }

            if (catalogoCats.isEmpty()) {
                Box(
                    Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "El catálogo está vacío.\nAgrega categorías y productos.",
                        fontSize  = 13.sp,
                        color     = TextGray,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                CategoriasGrid(
                    categorias       = catalogoCats,
                    onCategoriaClick = onCategoriaClick
                )
            }

            Box(
                modifier         = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                OutlinedButton(
                    onClick        = onOtroProducto,
                    shape          = RoundedCornerShape(10.dp),
                    border         = BorderStroke(1.dp, PrimaryColor),
                    colors         = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryColor),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Otro producto", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(Modifier.height(2.dp))
        }
    }
}

// ── Grid de categorías ────────────────────────────────────────────────────────

@Composable
private fun CategoriasGrid(
    categorias: List<CategoriaConProductos>,
    onCategoriaClick: (CategoriaConProductos) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categorias.chunked(3).forEach { fila ->
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                fila.forEach { catConProds ->
                    Surface(
                        shape    = RoundedCornerShape(12.dp),
                        color    = Color(0xFFF5F0FF),
                        border   = BorderStroke(1.dp, PrimaryContainer),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onCategoriaClick(catConProds) }
                    ) {
                        Column(
                            modifier            = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(catConProds.categoria.emoji, fontSize = 22.sp, lineHeight = 24.sp)
                            Text(
                                catConProds.categoria.nombre,
                                fontSize   = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color      = Color(0xFF3C3489),
                                textAlign  = TextAlign.Center,
                                maxLines   = 1
                            )
                        }
                    }
                }
                repeat(3 - fila.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

// ── Bottom sheet de productos de una categoría ────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProductosCategoriaSheet(
    catConProds: CategoriaConProductos,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onProductoClick: (CatalogoProducto) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = MaterialTheme.colorScheme.surface,
        shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(catConProds.categoria.emoji, fontSize = 24.sp)
                Text(catConProds.categoria.nombre, fontSize = 17.sp, fontWeight = FontWeight.Medium)
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = TextGray)
            }
        }
        HorizontalDivider(color = Color(0xFFE7E0EC))

        if (catConProds.productos.isEmpty()) {
            Box(
                Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Esta categoría no tiene productos aún.",
                    fontSize  = 13.sp,
                    color     = TextGray,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyVerticalGrid(
                columns               = GridCells.Fixed(2),
                modifier              = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                contentPadding        = PaddingValues(top = 12.dp, bottom = 32.dp),
                verticalArrangement   = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(catConProds.productos) { prod ->
                    ProductoCardFlash(
                        prod    = prod,
                        onClick = { onProductoClick(prod) }
                    )
                }
            }
        }
    }
}

// ── Card de producto con flash ────────────────────────────────────────────────

@Composable
internal fun ProductoCardFlash(
    prod: CatalogoProducto,
    onClick: () -> Unit,
) {
    var flashing by remember { mutableStateOf(false) }
    val bgColor by animateColorAsState(
        targetValue    = if (flashing) PrimaryContainer else Color.White,
        animationSpec  = androidx.compose.animation.core.tween(durationMillis = 300),
        finishedListener = { flashing = false },
        label          = "flash"
    )

    OutlinedCard(
        onClick = { onClick(); flashing = true },
        shape   = RoundedCornerShape(12.dp),
        colors  = CardDefaults.outlinedCardColors(containerColor = bgColor),
        border  = BorderStroke(1.dp, PrimaryContainer)
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(prod.nombre, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1C1B1F))
            Text(prod.precioTexto, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = PrimaryColor, modifier = Modifier.padding(top = 3.dp))
        }
    }
}
