package com.proyecto.cafetin.repository

import android.content.Context
import com.proyecto.cafetin.data.db.AppDatabase
import com.proyecto.cafetin.data.model.*
import com.proyecto.cafetin.data.model.TipoMovimiento
import com.proyecto.cafetin.util.DateUtils
import com.proyecto.cafetin.util.DateUtils.inicioDeDia
import kotlinx.coroutines.flow.Flow

class CafetinRepository(db: AppDatabase, context: Context) : ICafetinRepository {

    private val personaDao    = db.personaDao()
    private val movimientoDao = db.movimientoDao()
    private val catalogoDao   = db.catalogoDao()

    // ── Personas ──────────────────────────────────────────────────────────────
    override val personas = personaDao.getAll()
    override suspend fun insertPersona(persona: Persona)  { personaDao.insert(persona) }
    override suspend fun updatePersona(persona: Persona)  { personaDao.update(persona) }
    override suspend fun deletePersona(persona: Persona)  { personaDao.delete(persona) }
    override suspend fun marcarEnviado(personaId: Int, hastaMs: Long) {
        personaDao.marcarEnviado(personaId, hastaMs)
    }

    // ── Movimientos ───────────────────────────────────────────────────────────
    override fun movimientosPorPersonaHoy(personaId: Int): Flow<List<Movimiento>> =
        movimientoDao.getByPersonaHoy(personaId, inicioDeDia(System.currentTimeMillis()))

    override suspend fun movimientosPorPersonaEnRango(
        personaId: Int, desde: Long, hasta: Long
    ): List<Movimiento> = movimientoDao.getByPersonaEnRango(personaId, desde, hasta)

    override fun movimientosPorDia(fechaMs: Long): Flow<List<Movimiento>> {
        val desde = inicioDeDia(fechaMs)
        val hasta = desde + DateUtils.UN_DIA_MS
        return movimientoDao.getByRangoFecha(desde, hasta)
    }

    override fun saldoPorPersona(personaId: Int): Flow<Long> = movimientoDao.getSaldoByPersona(personaId)
    override suspend fun saldoPorPersonaUnaVez(personaId: Int): Long = movimientoDao.getSaldoByPersonaUnaVez(personaId)
    override val saldoTotal: Flow<Long> = movimientoDao.getSaldoTotal()
    override val totalAFavor: Flow<Long> = movimientoDao.getTotalAFavor()

    override fun cobradoHoy(): Flow<Long> =
        movimientoDao.getCobradoDesde(inicioDeDia(System.currentTimeMillis()))

    override suspend fun registrarFiado(personaId: Int, montoCentavos: Long, nota: String) {
        movimientoDao.insert(
            Movimiento(personaId = personaId, tipo = TipoMovimiento.FIADO, monto = montoCentavos, nota = nota)
        )
    }

    override suspend fun registrarPago(personaId: Int, montoCentavos: Long, nota: String) {
        movimientoDao.insert(
            Movimiento(personaId = personaId, tipo = TipoMovimiento.PAGO, monto = montoCentavos, nota = nota)
        )
    }

    override suspend fun editarMovimiento(movimiento: Movimiento)   { movimientoDao.update(movimiento) }
    override suspend fun eliminarMovimiento(movimiento: Movimiento) { movimientoDao.delete(movimiento) }

    // ── Catálogo ──────────────────────────────────────────────────────────────
    override fun getCategoriasFlow(): Flow<List<CatalogoCategoria>> = catalogoDao.getAllCategorias()
    override fun getAllProductosFlow(): Flow<List<CatalogoProducto>> = catalogoDao.getAllProductos()
    override fun getProductosByCategoriaFlow(categoriaId: Int): Flow<List<CatalogoProducto>> =
        catalogoDao.getProductosByCategoria(categoriaId)

    override suspend fun getCategoriaConProductos(): List<CategoriaConProductos> {
        val categorias = catalogoDao.getAllCategoriasSnapshot()
        val productos  = catalogoDao.getAllProductosSnapshot()
        val porCategoria = productos.groupBy { it.categoriaId }
        return categorias.map { cat ->
            CategoriaConProductos(cat, porCategoria[cat.id] ?: emptyList())
        }
    }

    override suspend fun insertCategoria(cat: CatalogoCategoria): Long =
        catalogoDao.insertCategoria(cat)

    override suspend fun updateCategoria(cat: CatalogoCategoria) { catalogoDao.updateCategoria(cat) }
    override suspend fun deleteCategoria(cat: CatalogoCategoria) { catalogoDao.deleteCategoria(cat) }
    override suspend fun insertProducto(prod: CatalogoProducto): Long =
        catalogoDao.insertProducto(prod)

    override suspend fun updateProducto(prod: CatalogoProducto) { catalogoDao.updateProducto(prod) }
    override suspend fun deleteProducto(prod: CatalogoProducto) { catalogoDao.deleteProducto(prod) }
}
