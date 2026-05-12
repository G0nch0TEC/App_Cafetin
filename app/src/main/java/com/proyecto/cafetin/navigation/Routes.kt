// ── ARCHIVO: navigation/Routes.kt ─────────────────────────────────────────
// Agrega la constante BACKUP

object Routes {
    const val PERSONAS = "personas"
    const val HISTORIAL = "historial"
    const val DETALLE = "detalle/{personaId}"
    const val CATALOGO = "catalogo"
    const val BACKUP = "backup"                    // ← NUEVO

    fun detalle(personaId: Int) = "detalle/$personaId"
}
