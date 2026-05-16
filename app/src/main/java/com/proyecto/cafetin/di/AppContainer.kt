package com.proyecto.cafetin.di

import android.content.Context
import com.proyecto.cafetin.data.db.AppDatabase
import com.proyecto.cafetin.repository.CafetinRepository
import java.util.UUID

class AppContainer(context: Context) {
    val database   by lazy { AppDatabase.getInstance(context) }
    val repository by lazy { CafetinRepository(database, context) }

    /**
     * ID único del dispositivo — se genera la primera vez y se persiste
     * en SharedPreferences. Identifica qué base de datos usar en el servidor.
     */
    val deviceId: String by lazy {
        val prefs = context.getSharedPreferences("cafetin_prefs", Context.MODE_PRIVATE)
        prefs.getString("device_id", null) ?: run {
            val nuevo = UUID.randomUUID().toString().replace("-", "").take(16)
            prefs.edit().putString("device_id", nuevo).apply()
            nuevo
        }
    }
}