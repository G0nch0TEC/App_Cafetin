package com.proyecto.cafetin.repository

import com.proyecto.cafetin.data.model.Movimiento
import com.proyecto.cafetin.data.model.Persona
import kotlinx.coroutines.flow.Flow


interface ICafetinRepository {
        val personas: Flow<List<Persona>>
        val saldoTotal: Flow<Long>
        val totalAFavor: Flow<Long>
        suspend fun insertPersona(persona: Persona)
        suspend fun updatePersona(persona: Persona)
        suspend fun deletePersona(persona: Persona)
        /** Marca el estado "Enviado" hasta el inicio del día siguiente. Pasar 0L lo limpia. */
        suspend fun marcarEnviado(personaId: Int, hastaMs: Long)
        fun movimientosPorPersonaHoy(personaId: Int): Flow<List<Movimiento>>
        fun movimientosPorDia(fechaMs: Long): Flow<List<Movimiento>>
        suspend fun movimientosPorPersonaEnRango(
            personaId: Int, desde: Long, hasta: Long
        ): List<Movimiento>
        fun saldoPorPersona(personaId: Int): Flow<Long>
        fun cobradoHoy(): Flow<Long>
        suspend fun registrarFiado(personaId: Int, montoCentavos: Long, nota: String)
        suspend fun registrarPago(personaId: Int, montoCentavos: Long, nota: String)
        suspend fun editarMovimiento(movimiento: Movimiento)
        suspend fun eliminarMovimiento(movimiento: Movimiento)
}
