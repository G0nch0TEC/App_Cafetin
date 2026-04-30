package com.proyecto.cafetin.repository

import com.proyecto.cafetin.data.model.Movimiento
import kotlinx.coroutines.flow.Flow

interface IMovimientoRepository {
    val saldoTotal: Flow<Long>
    val totalAFavor: Flow<Long>
    fun movimientosPorPersonaHoy(personaId: Int): Flow<List<Movimiento>>
    fun movimientosPorDia(fechaMs: Long): Flow<List<Movimiento>>
    suspend fun movimientosPorPersonaEnRango(personaId: Int, desde: Long, hasta: Long): List<Movimiento>
    fun saldoPorPersona(personaId: Int): Flow<Long>
    fun cobradoHoy(): Flow<Long>
    suspend fun registrarFiado(personaId: Int, montoCentavos: Long, nota: String)
    suspend fun registrarPago(personaId: Int, montoCentavos: Long, nota: String)
    suspend fun editarMovimiento(movimiento: Movimiento)
    suspend fun eliminarMovimiento(movimiento: Movimiento)
}
