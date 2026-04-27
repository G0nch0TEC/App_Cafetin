package com.proyecto.cafetin.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "personas",
    indices = [Index(value = ["nombre", "descripcion"], unique = true)]
)
data class Persona(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val descripcion: String = "",
    /**
     * Timestamp (ms) hasta el cual esta persona tiene estado "Enviado".
     * 0L significa que no está en ese estado.
     * El estado expira automáticamente al inicio del día siguiente.
     */
    val enviadoHasta: Long = 0L
)
