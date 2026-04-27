package com.proyecto.cafetin.data

import com.proyecto.cafetin.data.model.Producto.CategoriaProductos
import com.proyecto.cafetin.data.model.Producto.ProductoRapido

object ProductosCatalogo {
    val categorias: List<CategoriaProductos> = listOf(
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
}