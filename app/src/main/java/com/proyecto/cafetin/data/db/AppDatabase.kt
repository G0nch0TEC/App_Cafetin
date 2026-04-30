package com.proyecto.cafetin.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.proyecto.cafetin.data.model.CatalogoCategoria
import com.proyecto.cafetin.data.model.CatalogoProducto
import com.proyecto.cafetin.data.model.Movimiento
import com.proyecto.cafetin.data.model.Persona

@Database(
    entities = [
        Persona::class,
        Movimiento::class,
        CatalogoCategoria::class,
        CatalogoProducto::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun personaDao(): PersonaDao
    abstract fun movimientoDao(): MovimientoDao
    abstract fun catalogoDao(): CatalogoDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cafetin_db"
                )
                    .addMigrations(
                        DatabaseMigrations.MIGRATION_1_2,
                        DatabaseMigrations.MIGRATION_2_3
                    )
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
