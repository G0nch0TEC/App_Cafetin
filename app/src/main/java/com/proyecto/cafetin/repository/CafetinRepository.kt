package com.proyecto.cafetin.repository

import com.proyecto.cafetin.data.db.AppDatabase
import com.proyecto.cafetin.data.model.Movimiento
import com.proyecto.cafetin.data.model.Persona
import com.proyecto.cafetin.data.model.TipoMovimiento
import com.proyecto.cafetin.util.DateUtils
import com.proyecto.cafetin.util.DateUtils.inicioDeDia
import kotlinx.coroutines.flow.Flow

class CafetinRepository(db: AppDatabase) : ICafetinRepository {

    private val personaDao    = db.personaDao()
    private val movimientoDao = db.movimientoDao()

    // ── Personas ──────────────────────────────────────────────────────────────
    override val personas = personaDao.getAll()
    override suspend fun insertPersona(persona: Persona)  = personaDao.insert(persona)
    override suspend fun updatePersona(persona: Persona)  = personaDao.update(persona)
    override suspend fun deletePersona(persona: Persona)  = personaDao.delete(persona)

    /**
     * Marca el estado "Enviado" de una persona hasta [hastaMs].
     * El estado expira automáticamente: la UI lo ignora si ya pasó ese timestamp.
     */
    override suspend fun marcarEnviado(personaId: Int, hastaMs: Long) =
        personaDao.marcarEnviado(personaId, hastaMs)

    // ── Movimientos ───────────────────────────────────────────────────────────

    /** Solo los movimientos de HOY para una persona (pantalla Detalle) */
    override fun movimientosPorPersonaHoy(personaId: Int): Flow<List<Movimiento>> =
        movimientoDao.getByPersonaHoy(personaId, inicioDeDia(System.currentTimeMillis()))

    /** Movimientos de una persona en un rango fechas (para exportar PDF) */
    override suspend fun movimientosPorPersonaEnRango(
        personaId: Int,
        desde: Long,
        hasta: Long
    ): List<Movimiento> = movimientoDao.getByPersonaEnRango(personaId, desde, hasta)

    /** Movimientos de cualquier día completo (pantalla Historial general) */
    override fun movimientosPorDia(fechaMs: Long): Flow<List<Movimiento>> {
        val desde = inicioDeDia(fechaMs)
        val hasta = desde + DateUtils.UN_DIA_MS
        return movimientoDao.getByRangoFecha(desde, hasta)
    }

    override fun saldoPorPersona(personaId: Int): Flow<Long> = movimientoDao.getSaldoByPersona(personaId)
    override val saldoTotal: Flow<Long> = movimientoDao.getSaldoTotal()

    override fun cobradoHoy(): Flow<Long> =
        movimientoDao.getCobradoDesde(inicioDeDia(System.currentTimeMillis()))

    // ── Semántica ─────────────────────────────────────────────────────────────
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

    override suspend fun editarMovimiento(movimiento: Movimiento)   = movimientoDao.update(movimiento)
    override suspend fun eliminarMovimiento(movimiento: Movimiento) = movimientoDao.delete(movimiento)
}
