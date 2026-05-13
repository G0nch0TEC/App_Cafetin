package com.proyecto.cafetin.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Servicio para confirmar el token QR con el servidor web.
 * Usa HttpURLConnection puro — sin dependencias extra.
 */
object AuthApiService {

    // ⚠️ Debe coincidir con API_BASE_URL del resto de la app
    private const val API_BASE = "http://192.168.18.22/cafetin-view-api/index.php"

    sealed class ResultadoAuth {
        object Exito    : ResultadoAuth()
        data class Error(val mensaje: String) : ResultadoAuth()
    }

    /**
     * Manda el token escaneado al servidor para autorizar el acceso web.
     * Llamar desde una corrutina (suspend).
     */
    suspend fun confirmarToken(token: String): ResultadoAuth = withContext(Dispatchers.IO) {
        try {
            val url  = URL("$API_BASE?_route=auth/confirmar")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod        = "POST"
                doOutput             = true
                connectTimeout       = 8_000
                readTimeout          = 8_000
                setRequestProperty("Content-Type", "application/json")
            }

            val body = JSONObject().put("token", token).toString()
            conn.outputStream.use { it.write(body.toByteArray()) }

            val code     = conn.responseCode
            val respuesta = conn.inputStream.bufferedReader().readText()
            conn.disconnect()

            if (code == 200) {
                val json = JSONObject(respuesta)
                if (json.optBoolean("ok", false)) {
                    ResultadoAuth.Exito
                } else {
                    ResultadoAuth.Error(json.optString("error", "Respuesta inesperada"))
                }
            } else {
                val json = JSONObject(conn.errorStream?.bufferedReader()?.readText() ?: "{}")
                ResultadoAuth.Error(json.optString("error", "Error $code"))
            }

        } catch (e: Exception) {
            ResultadoAuth.Error("Sin conexión: ${e.message}")
        }
    }
}
