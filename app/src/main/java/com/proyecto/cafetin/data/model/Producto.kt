package com.proyecto.cafetin.data.model

class Producto {
    // ── Modelos ───────────────────────────────────────────────────────────────────
    data class ProductoRapido(val nombre: String, val montoCentavos: Long) {
        val precioTexto get() = "S/${montoCentavos / 100}.${ "%02d".format(montoCentavos % 100) }"
    }

    data class CategoriaProductos(
        val nombre: String,
        val emoji: String,
        val productos: List<ProductoRapido>
    )
}