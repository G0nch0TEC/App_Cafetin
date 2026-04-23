package com.proyecto.cafetin.repository

import com.proyecto.cafetin.data.db.AppDatabase
import com.proyecto.cafetin.data.model.Movimiento
import com.proyecto.cafetin.data.model.Persona
import com.proyecto.cafetin.data.model.TipoMovimiento
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class CafetinRepository(db: AppDatabase) {

    private val personaDao    = db.personaDao()
    private val movimientoDao = db.movimientoDao()

    // ── Personas ──────────────────────────────────────────────────────────────
    val personas = personaDao.getAll()
    suspend fun insertPersona(persona: Persona)  = personaDao.insert(persona)
    suspend fun updatePersona(persona: Persona)  = personaDao.update(persona)
    suspend fun deletePersona(persona: Persona)  = personaDao.delete(persona)

    // ── Movimientos ───────────────────────────────────────────────────────────

    /** Solo los movimientos de HOY para una persona (pantalla Detalle) */
    fun movimientosPorPersonaHoy(personaId: Int): Flow<List<Movimiento>> =
        movimientoDao.getByPersonaHoy(personaId, inicioDeDia(System.currentTimeMillis()))

    /** Movimientos de una persona en un rango fechas (para exportar PDF) */
    suspend fun movimientosPorPersonaEnRango(
        personaId: Int,
        desde: Long,
        hasta: Long
    ): List<Movimiento> = movimientoDao.getByPersonaEnRango(personaId, desde, hasta)

    /** Movimientos de cualquier día completo (pantalla Historial general) */
    fun movimientosPorDia(fechaMs: Long): Flow<List<Movimiento>> {
        val desde = inicioDeDia(fechaMs)
        val hasta = desde + 24 * 60 * 60 * 1000L
        return movimientoDao.getByRangoFecha(desde, hasta)
    }

    fun saldoPorPersona(personaId: Int): Flow<Long> = movimientoDao.getSaldoByPersona(personaId)
    val saldoTotal: Flow<Long> = movimientoDao.getSaldoTotal()

    fun cobradoHoy(): Flow<Long> =
        movimientoDao.getCobradoDesde(inicioDeDia(System.currentTimeMillis()))

    // ── Helpers ───────────────────────────────────────────────────────────────
    fun inicioDeDia(fechaMs: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = fechaMs
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    fun finDeDia(fechaMs: Long): Long = inicioDeDia(fechaMs) + 24 * 60 * 60 * 1000L

    // ── Semántica ─────────────────────────────────────────────────────────────
    suspend fun registrarFiado(personaId: Int, montoCentavos: Long, nota: String = "") {
        movimientoDao.insert(
            Movimiento(personaId = personaId, tipo = TipoMovimiento.FIADO, monto = montoCentavos, nota = nota)
        )
    }

    suspend fun registrarPago(personaId: Int, montoCentavos: Long, nota: String = "") {
        movimientoDao.insert(
            Movimiento(personaId = personaId, tipo = TipoMovimiento.PAGO, monto = montoCentavos, nota = nota)
        )
    }

    suspend fun editarMovimiento(movimiento: Movimiento)   = movimientoDao.update(movimiento)
    suspend fun eliminarMovimiento(movimiento: Movimiento) = movimientoDao.delete(movimiento)
}
