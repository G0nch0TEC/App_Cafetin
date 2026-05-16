package com.proyecto.cafetin.network

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object AuthApiService {

    private const val API_BASE = "https://cafetin-view-api-production.up.railway.app/index.php"

    sealed class ResultadoAuth {
        object Exito : ResultadoAuth()
        data class Error(val mensaje: String) : ResultadoAuth()
    }

    /**
     * Confirma el token QR e incluye el deviceId para que el servidor
     * sepa qué base de datos asociar a esta sesión.
     */
    suspend fun confirmarToken(token: String, context: Context): ResultadoAuth =
        withContext(Dispatchers.IO) {
            try {
                val deviceId = (context.applicationContext as com.proyecto.cafetin.CafetinApp)
                    .container.deviceId

                val url  = URL("$API_BASE?_route=auth/confirmar")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod  = "POST"
                    doOutput       = true
                    connectTimeout = 8_000
                    readTimeout    = 8_000
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("X-Device-Id", deviceId)
                }

                val body = JSONObject()
                    .put("token",    token)
                    .put("deviceId", deviceId)
                    .toString()

                conn.outputStream.use { it.write(body.toByteArray()) }

                val code      = conn.responseCode
                val respuesta = conn.inputStream.bufferedReader().readText()
                conn.disconnect()

                if (code == 200) {
                    val json = JSONObject(respuesta)
                    if (json.optBoolean("ok", false)) ResultadoAuth.Exito
                    else ResultadoAuth.Error(json.optString("error", "Respuesta inesperada"))
                } else {
                    val json = JSONObject(conn.errorStream?.bufferedReader()?.readText() ?: "{}")
                    ResultadoAuth.Error(json.optString("error", "Error $code"))
                }

            } catch (e: Exception) {
                ResultadoAuth.Error("Sin conexión: ${e.message}")
            }
        }
}