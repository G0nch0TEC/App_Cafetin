package com.proyecto.cafetin.util

/**
 * Fuzzy search de máxima tolerancia.
 *
 * Capas en orden de costo (se detiene en la primera que coincide):
 *  1. Contains directo (sin acentos)         — "mar" → "Maria" ✓
 *  2. Prefijo por token                       — "mar" → "Marta" ✓
 *  3. Subsecuencia ordenada                   — "mra" → "Maria" ✓
 *  4. Similitud por bigramas (≥ 40%)          — "juen" → "Juan" ✓
 *  5. Levenshtein con umbral generoso         — "robeto" → "Roberto" ✓
 */
fun fuzzyMatch(texto: String, query: String): Boolean {
    if (query.isBlank()) return true
    if (texto.isBlank()) return false

    val t = texto.normalize()
    val q = query.normalize()

    // Capa 1: contains directo normalizado
    if (t.contains(q)) return true

    val tokensTexto = t.split(Regex("\\s+")).filter { it.isNotEmpty() }
    val tokensQuery = q.split(Regex("\\s+")).filter { it.isNotEmpty() }

    // Cada token del query debe coincidir con ALGÚN token del texto
    return tokensQuery.all { tq ->
        tokensTexto.any { tt -> tokenMatches(tt, tq) }
    }
}

private fun tokenMatches(tt: String, tq: String): Boolean {
    // Capa 1: contains (cualquier dirección)
    if (tt.contains(tq) || tq.contains(tt)) return true

    // Capa 2: prefijo — el query empieza igual que el token del texto
    // "mar" coincide con "maria", "marta", "marco"
    if (tt.startsWith(tq) || tq.startsWith(tt)) return true

    // Capa 3: subsecuencia ordenada — las letras del query aparecen en orden en el texto
    // "mra" → "maria" porque m..r..a aparecen en ese orden
    // Solo aplica si el query tiene al menos 2 letras y cubre ≥50% del token
    if (tq.length >= 2 && tq.length >= tt.length / 2 && isSubsequence(tq, tt)) return true

    // Capa 4: similitud por bigramas — comparte suficientes pares de letras
    // "juen" y "juan" comparten "ju" → similitud alta
    val sim = bigramSimilarity(tt, tq)
    if (sim >= 0.4f) return true

    // Capa 5: Levenshtein con umbral generoso
    val maxLen = maxOf(tt.length, tq.length)
    if (maxLen == 0) return true
    val umbral = when {
        maxLen <= 3 -> 1
        maxLen <= 5 -> 2
        maxLen <= 8 -> 3
        else        -> 4
    }
    return levenshtein(tt, tq) <= umbral
}

/**
 * Verifica si [sub] es subsecuencia de [str].
 * "mra" es subsecuencia de "maria" → true
 */
private fun isSubsequence(sub: String, str: String): Boolean {
    var si = 0
    for (c in str) {
        if (si < sub.length && c == sub[si]) si++
        if (si == sub.length) return true
    }
    return false
}

/**
 * Similitud de bigramas (Dice coefficient).
 * Divide cada cadena en pares de letras consecutivos y mide cuántos comparten.
 * Rango: 0.0 (nada en común) → 1.0 (idénticas).
 */
private fun bigramSimilarity(a: String, b: String): Float {
    if (a.length < 2 || b.length < 2) {
        return if (a == b) 1f else 0f
    }
    val bigramsA = (0 until a.length - 1).map { a.substring(it, it + 2) }
    val bigramsB = (0 until b.length - 1).map { b.substring(it, it + 2) }.toMutableList()

    var matches = 0
    for (bg in bigramsA) {
        val idx = bigramsB.indexOf(bg)
        if (idx >= 0) {
            matches++
            bigramsB.removeAt(idx)
        }
    }
    return (2f * matches) / (bigramsA.size + (b.length - 1))
}

/**
 * Normaliza: minúsculas + sin acentos/diacríticos.
 */
private fun String.normalize(): String =
    this.lowercase()
        .replace('á', 'a').replace('à', 'a').replace('ä', 'a').replace('â', 'a')
        .replace('é', 'e').replace('è', 'e').replace('ë', 'e').replace('ê', 'e')
        .replace('í', 'i').replace('ì', 'i').replace('ï', 'i').replace('î', 'i')
        .replace('ó', 'o').replace('ò', 'o').replace('ö', 'o').replace('ô', 'o')
        .replace('ú', 'u').replace('ù', 'u').replace('ü', 'u').replace('û', 'u')
        .replace('ñ', 'n')

/**
 * Distancia de Levenshtein. O(m*n) tiempo, O(n) memoria.
 */
private fun levenshtein(a: String, b: String): Int {
    if (a == b) return 0
    if (a.isEmpty()) return b.length
    if (b.isEmpty()) return a.length
    var prev = IntArray(b.length + 1) { it }
    var curr = IntArray(b.length + 1)
    for (i in 1..a.length) {
        curr[0] = i
        for (j in 1..b.length) {
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1
            curr[j] = minOf(prev[j] + 1, curr[j - 1] + 1, prev[j - 1] + cost)
        }
        val tmp = prev; prev = curr; curr = tmp
    }
    return prev[b.length]
}
