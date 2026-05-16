package com.proyecto.cafetin.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.proyecto.cafetin.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class SyncManager(private val context: Context, private val deviceId: String) {

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

            val db            = AppDatabase.getInstance(context)
            val personaDao    = db.personaDao()
            val movimientoDao = db.movimientoDao()
            val catalogoDao   = db.catalogoDao()

            val personas    = personaDao.getAllSnapshot()
            val movimientos = movimientoDao.getAllSnapshot()
            val categorias  = catalogoDao.getAllCategoriasSnapshot()
            val productos   = catalogoDao.getAllProductosSnapshot()

            val payload = JSONObject().apply {
                put("personas", JSONArray().also { arr ->
                    personas.forEach { p ->
                        arr.put(JSONObject().apply {
                            put("id", p.id)
                            put("nombre", p.nombre)
                            put("descripcion", p.descripcion)
                            put("enviadoHasta", p.enviadoHasta)
                        })
                    }
                })
                put("movimientos", JSONArray().also { arr ->
                    movimientos.forEach { m ->
                        arr.put(JSONObject().apply {
                            put("id", m.id)
                            put("personaId", m.personaId)
                            put("tipo", m.tipo.name)
                            put("monto", m.monto)
                            put("fecha", m.fecha)
                            put("nota", m.nota)
                        })
                    }
                })
                put("categorias", JSONArray().also { arr ->
                    categorias.forEach { c ->
                        arr.put(JSONObject().apply {
                            put("id", c.id)
                            put("nombre", c.nombre)
                            put("emoji", c.emoji)
                            put("orden", c.orden)
                        })
                    }
                })
                put("productos", JSONArray().also { arr ->
                    productos.forEach { p ->
                        arr.put(JSONObject().apply {
                            put("id", p.id)
                            put("categoriaId", p.categoriaId)
                            put("nombre", p.nombre)
                            put("montoCentavos", p.montoCentavos)
                            put("orden", p.orden)
                        })
                    }
                })
            }

            val body      = payload.toString()
            val uploadUrl = "$API_BASE_URL?_route=upload"

            val conn = (URL(uploadUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput      = true
                connectTimeout = 10_000
                readTimeout    = 30_000
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("X-Api-Key",  UPLOAD_API_KEY)
                setRequestProperty("X-Device-Id", deviceId)
            }

            conn.outputStream.use { out ->
                out.write(body.toByteArray(Charsets.UTF_8))
            }

            val code     = conn.responseCode
            val respBody = try {
                conn.inputStream.bufferedReader().readText()
            } catch (_: Exception) {
                conn.errorStream?.bufferedReader()?.readText() ?: "sin respuesta"
            }

            conn.disconnect()

            if (code == 200) Result.success("Sincronizado")
            else Result.failure(Exception("Error $code: $respBody"))

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}