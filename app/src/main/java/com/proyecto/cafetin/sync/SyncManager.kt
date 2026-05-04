package com.proyecto.cafetin.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class SyncManager(private val context: Context) {

    companion object {
        // dependiendo de tu url o host, cambiar la url
        const val API_BASE_URL = "http://192.168.18.22/cafetin-view-api"
    }

    fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun sincronizar(): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!isOnline()) return@withContext Result.failure(Exception("Sin conexión a internet"))

            val dbFile = context.getDatabasePath("cafetin_db")
            if (!dbFile.exists()) return@withContext Result.failure(Exception("Base de datos no encontrada"))

            val tempFile = File(context.cacheDir, "cafetin_db_upload.db")
            dbFile.copyTo(tempFile, overwrite = true)

            val boundary = "----CafetinBoundary${System.currentTimeMillis()}"
            val conn = (URL("$API_BASE_URL/upload").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            }

            conn.outputStream.use { out ->
                val header = "--$boundary\r\nContent-Disposition: form-data; name=\"db\"; filename=\"cafetin_db\"\r\nContent-Type: application/octet-stream\r\n\r\n"
                out.write(header.toByteArray())
                tempFile.inputStream().use { it.copyTo(out) }
                out.write("\r\n--$boundary--\r\n".toByteArray())
            }

            val code = conn.responseCode
            tempFile.delete()
            conn.disconnect()

            if (code == 200) Result.success("Sincronizado correctamente")
            else Result.failure(Exception("Error del servidor: $code"))

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}