package com.proyecto.cafetin.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.proyecto.cafetin.util.MoneyUtils.toPrecioTexto

@Entity(tableName = "catalogo_categorias")
data class CatalogoCategoria(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val emoji: String,
    val orden: Int = 0
)

@Entity(
    tableName = "catalogo_productos",
    foreignKeys = [
        ForeignKey(
            entity = CatalogoCategoria::class,
            parentColumns = ["id"],
            childColumns = ["categoriaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("categoriaId")]
)
data class CatalogoProducto(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val categoriaId: Int,
    val nombre: String,
    val montoCentavos: Long,
    val orden: Int = 0
) {
    val precioTexto get() = montoCentavos.toPrecioTexto()
}

/** Categoría con sus productos, construida en código (no @Relation de Room) */
data class CategoriaConProductos(
    val categoria: CatalogoCategoria,
    val productos: List<CatalogoProducto>
)
