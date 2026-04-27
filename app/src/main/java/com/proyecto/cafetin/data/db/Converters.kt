package com.proyecto.cafetin.data.db

import androidx.room.TypeConverter
import com.proyecto.cafetin.data.model.TipoMovimiento

class Converters {

    @TypeConverter
    fun fromTipo(tipo: TipoMovimiento): String = tipo.name

    @TypeConverter
    fun toTipo(value: String): TipoMovimiento =
        runCatching { TipoMovimiento.valueOf(value) }
            .getOrDefault(TipoMovimiento.FIADO)
}
