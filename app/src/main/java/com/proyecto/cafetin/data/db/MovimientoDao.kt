package com.proyecto.cafetin.data.db

import androidx.room.*
import com.proyecto.cafetin.data.model.Movimiento
import kotlinx.coroutines.flow.Flow

@Dao
interface MovimientoDao {

    /** Movimientos de una persona SOLO del día de hoy (pantalla Detalle) */
    @Query("""
        SELECT * FROM movimientos
        WHERE personaId = :personaId AND fecha >= :inicioDia
        ORDER BY fecha DESC
    """)
    fun getByPersonaHoy(personaId: Int, inicioDia: Long): Flow<List<Movimiento>>

    /** Movimientos de una persona en un rango de fechas (exportar PDF) */
    @Query("""
        SELECT * FROM movimientos
        WHERE personaId = :personaId AND fecha >= :desde AND fecha < :hasta
        ORDER BY fecha ASC
    """)
    suspend fun getByPersonaEnRango(personaId: Int, desde: Long, hasta: Long): List<Movimiento>

    /** Todos los movimientos de un día completo (pantalla Historial general) */
    @Query("""
        SELECT * FROM movimientos
        WHERE fecha >= :desde AND fecha < :hasta
        ORDER BY fecha DESC
    """)
    fun getByRangoFecha(desde: Long, hasta: Long): Flow<List<Movimiento>>

    @Query("""
        SELECT COALESCE(SUM(CASE WHEN tipo = 'FIADO' THEN monto ELSE -monto END), 0)
        FROM movimientos WHERE personaId = :personaId
    """)
    fun getSaldoByPersona(personaId: Int): Flow<Long>

    /** Lectura única del saldo real (para exportaciones) */
    @Query("""
        SELECT COALESCE(SUM(CASE WHEN tipo = 'FIADO' THEN monto ELSE -monto END), 0)
        FROM movimientos WHERE personaId = :personaId
    """)
    suspend fun getSaldoByPersonaUnaVez(personaId: Int): Long

    @Query("""
        SELECT COALESCE(SUM(saldo), 0)
        FROM (
            SELECT SUM(CASE WHEN tipo = 'FIADO' THEN monto ELSE -monto END) AS saldo
            FROM movimientos
            GROUP BY personaId
            HAVING saldo > 0
        )
    """)
    fun getSaldoTotal(): Flow<Long>

    /**
     * Suma los saldos negativos (adelantos/pagos en exceso).
     * Devuelve valor positivo: ej. si alguien pagó 50 de más, retorna 50.
     */
    @Query("""
        SELECT COALESCE(-SUM(saldo), 0)
        FROM (
            SELECT SUM(CASE WHEN tipo = 'FIADO' THEN monto ELSE -monto END) AS saldo
            FROM movimientos
            GROUP BY personaId
            HAVING saldo < 0
        )
    """)
    fun getTotalAFavor(): Flow<Long>

    @Query("""
        SELECT COALESCE(SUM(monto), 0) FROM movimientos
        WHERE tipo = 'PAGO' AND fecha >= :desdeFecha
    """)
    fun getCobradoDesde(desdeFecha: Long): Flow<Long>

    @Query("SELECT * FROM movimientos ORDER BY fecha DESC")
    suspend fun getAllSnapshot(): List<Movimiento>

    @Insert
    suspend fun insert(movimiento: Movimiento)

    @Update
    suspend fun update(movimiento: Movimiento)

    @Delete
    suspend fun delete(movimiento: Movimiento)

}