package com.proyecto.cafetin

import android.app.Application
import com.proyecto.cafetin.di.AppContainer

class CafetinApp : Application() {

    val container by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        // Sync manual únicamente — sin WorkManager
    }
}
