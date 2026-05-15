package com.proyecto.cafetin.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.proyecto.cafetin.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class SyncManager(private val context: Context) {

    companion object {
        const val API_BASE_URL = "https://cafetin-view-api-production.up.railway.app/index.php"
        private const val UPLOAD_API_KEY = "cafetin2026xK9mP"
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

            val db = AppDatabase.getInstance(context)

            db.openHelper.writableDatabase
                .query("PRAGMA wal_checkpoint(FULL)")
                .close()

            db.close()

            Thread.sleep(300)

            val dbFile = context.getDatabasePath("cafetin_db")
            if (!dbFile.exists())
                return@withContext Result.failure(
                    Exception("Base de datos no encontrada")
                )
            val tempFile = File(context.cacheDir, "cafetin_db_upload.db")
            dbFile.copyTo(tempFile, overwrite = true)

            val boundary = "----CafetinBoundary${System.currentTimeMillis()}"
            val uploadUrl = "$API_BASE_URL?_route=upload"

            val conn = (URL(uploadUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 10_000
                readTimeout = 30_000
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                setRequestProperty("X-Api-Key", UPLOAD_API_KEY)
            }

            conn.outputStream.use { out ->
                val header = "--$boundary\r\nContent-Disposition: form-data; name=\"db\"; filename=\"cafetin_db\"\r\nContent-Type: application/octet-stream\r\n\r\n"
                out.write(header.toByteArray())
                tempFile.inputStream().use { it.copyTo(out) }
                out.write("\r\n--$boundary--\r\n".toByteArray())
            }

            val code = conn.responseCode
            val body = try {
                conn.inputStream.bufferedReader().readText()
            } catch (_: Exception) {
                conn.errorStream?.bufferedReader()?.readText() ?: "sin respuesta"
            }

            tempFile.delete()
            conn.disconnect()

            if (code == 200) {
                Result.success("Respuesta API:\n$body")
            } else {
                Result.failure(Exception("Error $code:\n$body"))
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}