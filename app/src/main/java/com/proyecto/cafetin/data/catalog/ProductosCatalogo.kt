package com.proyecto.cafetin.data.catalog

import com.proyecto.cafetin.data.model.Producto

object ProductosCatalogo {
    val categorias: List<Producto.CategoriaProductos> = listOf(
        Producto.CategoriaProductos(
            "Bebidas", "🥤", listOf(
                Producto.ProductoRapido("Refresco", 100),
                Producto.ProductoRapido("Agua", 100),
                Producto.ProductoRapido("Agua c/Gas", 150),
                Producto.ProductoRapido("Jugo Papaya", 300),
                Producto.ProductoRapido("Jugo Fresa S", 600),
                Producto.ProductoRapido("Jugo Fresa L", 400),
                Producto.ProductoRapido("Sporade", 300),
                Producto.ProductoRapido("Chicha", 200),
                Producto.ProductoRapido("Gaseosas frutales", 300),
                Producto.ProductoRapido("Guaranita", 200),
            )
        ),
        Producto.CategoriaProductos(
            "Comida", "🍽️", listOf(
                Producto.ProductoRapido("Comida", 800),
                Producto.ProductoRapido("Papa Rellena", 500),
                Producto.ProductoRapido("Empanada", 400),
                Producto.ProductoRapido("Comidita", 500),
                Producto.ProductoRapido("Pizza", 500),
            )
        ),
        Producto.CategoriaProductos(
            "Snacks", "🍿", listOf(
                Producto.ProductoRapido("Chifles", 100), Producto.ProductoRapido("Inka Chips", 250),
                Producto.ProductoRapido("Cheetos", 200), Producto.ProductoRapido("Canchita", 100),
                Producto.ProductoRapido("Trixito", 150), Producto.ProductoRapido("Cuates", 200),
                Producto.ProductoRapido("Papa Lays", 250), Producto.ProductoRapido("Cereal", 100),
                Producto.ProductoRapido("Mani", 100), Producto.ProductoRapido("Abas", 100),
                Producto.ProductoRapido("Marshmellow", 200), Producto.ProductoRapido("Dorito", 250),
            )
        ),
        Producto.CategoriaProductos(
            "Antojos", "😋", listOf(
                Producto.ProductoRapido("Champa", 150),
                Producto.ProductoRapido("Pionono", 150),
                Producto.ProductoRapido("Keke", 200),
                Producto.ProductoRapido("Cupcake", 150),
                Producto.ProductoRapido("Alfajor", 100),
                Producto.ProductoRapido("Ensalada Fruta", 500),
            )
        ),
        Producto.CategoriaProductos(
            "Galletas", "🍪", listOf(
                Producto.ProductoRapido("Galleta chica", 100), Producto.ProductoRapido("Galleta grande", 200),
                Producto.ProductoRapido("Galleta mediana", 150), Producto.ProductoRapido("Cancun", 150),
                Producto.ProductoRapido("Chocman", 200),
            )
        ),
        Producto.CategoriaProductos(
            "Dulces", "🍬", listOf(
                Producto.ProductoRapido("Fruna", 150),
                Producto.ProductoRapido("3x Hals", 100),
                Producto.ProductoRapido("Oca Loca", 100),
                Producto.ProductoRapido("3x Adams", 100),
                Producto.ProductoRapido("Chin Chin", 150),
                Producto.ProductoRapido("Menta", 100),
                Producto.ProductoRapido("Caramelo Acido", 50),
                Producto.ProductoRapido("Barrilete", 50),
            )
        ),
        Producto.CategoriaProductos(
            "Fríos", "🧊", listOf(
                Producto.ProductoRapido("Gelatina", 100), Producto.ProductoRapido("Chupete", 100),
                Producto.ProductoRapido("Yogurt", 300), Producto.ProductoRapido("Pulp", 250),
                Producto.ProductoRapido("Chocolatada", 250),
            )
        ),
        Producto.CategoriaProductos(
            "Sandwiches", "🍞", listOf(
                Producto.ProductoRapido("Sandwich", 400),
                Producto.ProductoRapido("Pan Pizza", 500),
                Producto.ProductoRapido("Triple", 500),
            )
        ),
        Producto.CategoriaProductos(
            "Utiles", "✏️", listOf(
                Producto.ProductoRapido("Corrector", 300), Producto.ProductoRapido("Tajador", 250),
                Producto.ProductoRapido("Borrador", 200), Producto.ProductoRapido("Lapiz", 200),
                Producto.ProductoRapido("Lapicero", 300), Producto.ProductoRapido("Toalla Higienica", 150),
            )
        ),
    )
}