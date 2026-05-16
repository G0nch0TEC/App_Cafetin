package com.proyecto.cafetin.data.db

import androidx.room.*
import com.proyecto.cafetin.data.model.Persona
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonaDao {

    @Query("SELECT * FROM personas ORDER BY nombre ASC")
    fun getAll(): Flow<List<Persona>>

    @Query("SELECT * FROM personas ORDER BY nombre ASC")
    suspend fun getAllSnapshot(): List<Persona>

    @Insert
    suspend fun insert(persona: Persona)

    @Update
    suspend fun update(persona: Persona)

    @Delete
    suspend fun delete(persona: Persona)

    /**
     * Marca el estado "Enviado" de una persona estableciendo el timestamp
     * hasta el que es válido. Pasar 0L limpia el estado.
     */
    @Query("UPDATE personas SET enviadoHasta = :hastaMs WHERE id = :personaId")
    suspend fun marcarEnviado(personaId: Int, hastaMs: Long)
}