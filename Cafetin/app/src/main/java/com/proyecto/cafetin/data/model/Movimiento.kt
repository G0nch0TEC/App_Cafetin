package com.proyecto.cafetin.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TipoMovimiento { FIADO, PAGO }

@Entity(
    tableName = "movimientos",
    foreignKeys = [
        ForeignKey(
            entity = Persona::class,
            parentColumns = ["id"],
            childColumns = ["personaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("personaId")]
)
data class Movimiento(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val personaId: Int,
    val tipo: TipoMovimiento,
    val monto: Long,
    val fecha: Long = System.currentTimeMillis(),
    val nota: String = ""
)
