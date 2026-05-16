package com.proyecto.cafetin.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val deviceId = (applicationContext as com.proyecto.cafetin.CafetinApp).container.deviceId
        val syncManager = SyncManager(applicationContext, deviceId)
        return if (syncManager.isOnline()) {
            val resultado = syncManager.sincronizar()
            if (resultado.isSuccess) Result.success() else Result.retry()
        } else {
            Result.retry()  // WorkManager reintentará cuando haya red
        }
    }
}