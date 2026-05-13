package com.proyecto.cafetin.backup

import android.content.Context
import android.net.Uri
import com.proyecto.cafetin.data.model.*
import com.proyecto.cafetin.repository.CafetinRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Maneja la exportación e importación de todos los datos de la app en formato JSON.
 *
 * Formato del archivo:
 * {
 *   "version": 1,
 *   "fecha": "2026-05-12T14:30:00",
 *   "personas": [...],
 *   "movimientos": [...],
 *   "catalogo_categorias": [...],
 *   "catalogo_productos": [...]
 * }
 */
class BackupManager(
    private val context: Context,
    private val repository: CafetinRepository
) {

    companion object {
        const val BACKUP_VERSION = 1
        const val MIME_TYPE = "application/json"
    }

    // ── EXPORTAR ──────────────────────────────────────────────────────────────

    /**
     * Exporta todos los datos al URI elegido por el usuario (SAF).
     * Retorna null si todo fue bien, o un mensaje de error si algo falló.
     */
    suspend fun exportar(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val json = construirJson()
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(json.toString(2).toByteArray(Charsets.UTF_8))
            } ?: return@withContext "No se pudo abrir el archivo para escribir"
            null // éxito
        } catch (e: Exception) {
            "Error al exportar: ${e.localizedMessage}"
        }
    }

    private suspend fun construirJson(): JSONObject {
        val personas   = repository.personas.first()
        val categorias = repository.getCategoriasFlow().first()
        val productos  = repository.getAllProductosFlow().first()

        // Necesitamos todos los movimientos — los obtenemos por persona
        val movimientos = mutableListOf<Movimiento>()
        personas.forEach { persona ->
            val movs = repository.movimientosPorPersonaEnRango(
                personaId = persona.id,
                desde = 0L,
                hasta = Long.MAX_VALUE
            )
            movimientos.addAll(movs)
        }

        val fechaStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            .format(Date())

        return JSONObject().apply {
            put("version", BACKUP_VERSION)
            put("fecha", fechaStr)

            // Personas
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

            // Movimientos
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

            // Catálogo — categorías
            put("catalogo_categorias", JSONArray().also { arr ->
                categorias.forEach { c ->
                    arr.put(JSONObject().apply {
                        put("id", c.id)
                        put("nombre", c.nombre)
                        put("emoji", c.emoji)
                        put("orden", c.orden)
                    })
                }
            })

            // Catálogo — productos
            put("catalogo_productos", JSONArray().also { arr ->
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
    }

    // ── IMPORTAR ──────────────────────────────────────────────────────────────

    /**
     * Importa datos desde un archivo JSON elegido por el usuario.
     * REEMPLAZA todos los datos existentes.
     * Retorna null si todo fue bien, o un mensaje de error si algo falló.
     */
    suspend fun importar(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val texto = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.readBytes().toString(Charsets.UTF_8)
            } ?: return@withContext "No se pudo leer el archivo"

            val json = JSONObject(texto)

            // Validar versión
            val version = json.optInt("version", -1)
            if (version != BACKUP_VERSION) {
                return@withContext "Archivo de respaldo incompatible (versión $version)"
            }

            restaurarDatos(json)
            null // éxito
        } catch (e: Exception) {
            "Error al importar: ${e.localizedMessage}"
        }
    }

    private suspend fun restaurarDatos(json: JSONObject) {
        // 1. Borrar todo lo existente
        val personasActuales = repository.personas.first()
        personasActuales.forEach { repository.deletePersona(it) }

        val categoriasActuales = repository.getCategoriasFlow().first()
        categoriasActuales.forEach { repository.deleteCategoria(it) }

        // 2. Insertar categorías (guardamos mapa id_viejo → id_nuevo)
        val mapCategoria = mutableMapOf<Int, Int>()
        val jsonCategorias = json.getJSONArray("catalogo_categorias")
        for (i in 0 until jsonCategorias.length()) {
            val obj = jsonCategorias.getJSONObject(i)
            val idViejo = obj.getInt("id")
            val nuevaCategoria = CatalogoCategoria(
                nombre = obj.getString("nombre"),
                emoji  = obj.getString("emoji"),
                orden  = obj.optInt("orden", 0)
            )
            val idNuevo = repository.insertCategoria(nuevaCategoria).toInt()
            mapCategoria[idViejo] = idNuevo
        }

        // 3. Insertar productos (remapeando categoriaId)
        val mapProducto = mutableMapOf<Int, Int>()
        val jsonProductos = json.getJSONArray("catalogo_productos")
        for (i in 0 until jsonProductos.length()) {
            val obj = jsonProductos.getJSONObject(i)
            val idViejo = obj.getInt("id")
            val catIdViejo = obj.getInt("categoriaId")
            val catIdNuevo = mapCategoria[catIdViejo] ?: continue
            val nuevoProducto = CatalogoProducto(
                categoriaId   = catIdNuevo,
                nombre        = obj.getString("nombre"),
                montoCentavos = obj.getLong("montoCentavos"),
                orden         = obj.optInt("orden", 0)
            )
            val idNuevo = repository.insertProducto(nuevoProducto).toInt()
            mapProducto[idViejo] = idNuevo
        }

        // 4. Insertar personas (guardamos mapa id_viejo → id_nuevo)
        val mapPersona = mutableMapOf<Int, Int>()
        val jsonPersonas = json.getJSONArray("personas")
        for (i in 0 until jsonPersonas.length()) {
            val obj = jsonPersonas.getJSONObject(i)
            val idViejo = obj.getInt("id")
            val nuevaPersona = Persona(
                nombre      = obj.getString("nombre"),
                descripcion = obj.optString("descripcion", ""),
                enviadoHasta = obj.optLong("enviadoHasta", 0L)
            )
            repository.insertPersona(nuevaPersona)
            // Obtener el id generado buscando por nombre+descripcion
            val insertada = repository.personas.first()
                .find { it.nombre == nuevaPersona.nombre && it.descripcion == nuevaPersona.descripcion }
            if (insertada != null) mapPersona[idViejo] = insertada.id
        }

        // 5. Insertar movimientos conservando la fecha original
        val jsonMovimientos = json.getJSONArray("movimientos")
        for (i in 0 until jsonMovimientos.length()) {
            val obj = jsonMovimientos.getJSONObject(i)
            val personaIdViejo = obj.getInt("personaId")
            val personaIdNuevo = mapPersona[personaIdViejo] ?: continue
            val tipo  = TipoMovimiento.valueOf(obj.getString("tipo"))
            val monto = obj.getLong("monto")
            val fecha = obj.getLong("fecha")          // ← fecha original preservada
            val nota  = obj.optString("nota", "")
            repository.insertarMovimientoDirecto(
                Movimiento(
                    personaId = personaIdNuevo,
                    tipo      = tipo,
                    monto     = monto,
                    fecha     = fecha,
                    nota      = nota
                )
            )
        }
    }

    // ── NOMBRE DE ARCHIVO SUGERIDO ─────────────────────────────────────────

    fun nombreArchivoSugerido(): String {
        val fecha = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return "cafetin_backup_$fecha.json"
    }
}
