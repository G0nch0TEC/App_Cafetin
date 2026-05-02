package com.proyecto.cafetin

import android.app.Application
import androidx.work.*
import com.proyecto.cafetin.di.AppContainer
import com.proyecto.cafetin.sync.SyncWorker
import java.util.concurrent.TimeUnit

class CafetinApp : Application() {

    val container by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "cafetin_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
}