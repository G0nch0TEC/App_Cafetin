package com.proyecto.cafetin.data.model

import com.proyecto.cafetin.util.MoneyUtils.toPrecioTexto

/**
 * Modelos en memoria usados únicamente por ProductosCatalogo para pre-poblar
 * la base de datos en la migración 2→3. Con el catálogo ya en Room, estas clases
 * son candidatas a eliminarse en cuanto ProductosCatalogo deje de ser necesario.
 */
class Producto {
    data class ProductoRapido(val nombre: String, val montoCentavos: Long) {
        val precioTexto get() = montoCentavos.toPrecioTexto()
    }

    data class CategoriaProductos(
        val nombre: String,
        val emoji: String,
        val productos: List<ProductoRapido>
    )
}
