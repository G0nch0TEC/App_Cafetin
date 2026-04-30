package com.proyecto.cafetin.repository

import com.proyecto.cafetin.data.model.CatalogoCategoria
import com.proyecto.cafetin.data.model.CatalogoProducto
import com.proyecto.cafetin.data.model.CategoriaConProductos
import kotlinx.coroutines.flow.Flow

interface ICatalogoRepository {
    fun getCategoriasFlow(): Flow<List<CatalogoCategoria>>
    fun getAllProductosFlow(): Flow<List<CatalogoProducto>>
    fun getProductosByCategoriaFlow(categoriaId: Int): Flow<List<CatalogoProducto>>
    suspend fun getCategoriaConProductos(): List<CategoriaConProductos>
    suspend fun insertCategoria(cat: CatalogoCategoria): Long
    suspend fun updateCategoria(cat: CatalogoCategoria)
    suspend fun deleteCategoria(cat: CatalogoCategoria)
    suspend fun insertProducto(prod: CatalogoProducto): Long
    suspend fun updateProducto(prod: CatalogoProducto)
    suspend fun deleteProducto(prod: CatalogoProducto)
}
