package com.proyecto.cafetin.repository

import com.proyecto.cafetin.data.model.Persona
import kotlinx.coroutines.flow.Flow

interface IPersonaRepository {
    val personas: Flow<List<Persona>>
    suspend fun insertPersona(persona: Persona)
    suspend fun updatePersona(persona: Persona)
    suspend fun deletePersona(persona: Persona)
    suspend fun marcarEnviado(personaId: Int, hastaMs: Long)
}
