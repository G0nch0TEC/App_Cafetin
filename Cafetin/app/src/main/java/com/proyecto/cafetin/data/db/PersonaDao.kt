package com.proyecto.cafetin.data.db

import androidx.room.*
import com.proyecto.cafetin.data.model.Persona
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonaDao {

    @Query("SELECT * FROM personas ORDER BY nombre ASC")
    fun getAll(): Flow<List<Persona>>

    @Insert
    suspend fun insert(persona: Persona)

    @Update
    suspend fun update(persona: Persona)

    @Delete
    suspend fun delete(persona: Persona)
}
