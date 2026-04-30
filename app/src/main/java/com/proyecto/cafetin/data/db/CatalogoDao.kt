package com.proyecto.cafetin.data.db

import androidx.room.*
import com.proyecto.cafetin.data.model.CatalogoCategoria
import com.proyecto.cafetin.data.model.CatalogoProducto
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogoDao {

    // ── Categorías ────────────────────────────────────────────────────────────

    @Query("SELECT * FROM catalogo_categorias ORDER BY orden ASC, id ASC")
    fun getAllCategorias(): Flow<List<CatalogoCategoria>>

    @Insert
    suspend fun insertCategoria(cat: CatalogoCategoria): Long

    @Update
    suspend fun updateCategoria(cat: CatalogoCategoria)

    @Delete
    suspend fun deleteCategoria(cat: CatalogoCategoria)

    @Query("SELECT COUNT(*) FROM catalogo_categorias")
    suspend fun contarCategorias(): Int

    // ── Productos ─────────────────────────────────────────────────────────────

    @Query("SELECT * FROM catalogo_productos WHERE categoriaId = :catId ORDER BY orden ASC, id ASC")
    fun getProductosByCategoria(catId: Int): Flow<List<CatalogoProducto>>

    @Query("SELECT * FROM catalogo_productos ORDER BY orden ASC, id ASC")
    fun getAllProductos(): Flow<List<CatalogoProducto>>

    @Insert
    suspend fun insertProducto(prod: CatalogoProducto): Long

    @Update
    suspend fun updateProducto(prod: CatalogoProducto)

    @Delete
    suspend fun deleteProducto(prod: CatalogoProducto)

    // ── Para la pantalla Detalle: categorias + todos sus productos ─────────────
    // Se obtienen por separado y se combinan en el repositorio/viewmodel
    @Query("SELECT * FROM catalogo_categorias ORDER BY orden ASC, id ASC")
    suspend fun getAllCategoriasSnapshot(): List<CatalogoCategoria>

    @Query("SELECT * FROM catalogo_productos ORDER BY categoriaId ASC, orden ASC, id ASC")
    suspend fun getAllProductosSnapshot(): List<CatalogoProducto>
}
