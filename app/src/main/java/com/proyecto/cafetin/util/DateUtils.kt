package com.proyecto.cafetin.util

import java.util.Calendar
import java.util.TimeZone

object DateUtils {

    const val UN_DIA_MS = 24 * 60 * 60 * 1000L

    // ── Funciones internas ────────────────────────────────────────────────────

    /**
     * Devuelve un Calendar local con la hora puesta a medianoche (00:00:00.000),
     * a partir de un timestamp que ya está en hora local.
     */
    private fun baseCalendar(fechaMs: Long) = Calendar.getInstance().apply {
        timeInMillis = fechaMs
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    // ── API pública ───────────────────────────────────────────────────────────

    /**
     * Inicio de día (00:00:00 local) para un timestamp en hora local.
     * Úsala para timestamps generados por el propio sistema (System.currentTimeMillis,
     * valores ya almacenados en BD, etc.).
     */
    fun inicioDeDia(fechaMs: Long): Long =
        baseCalendar(fechaMs).timeInMillis

    /**
     * Fin de día = inicio del día SIGUIENTE en hora local (exclusive-end para rangos).
     */
    fun finDeDia(fechaMs: Long): Long =
        baseCalendar(fechaMs).apply {
            add(Calendar.DAY_OF_MONTH, 1)
        }.timeInMillis

    fun inicioDeDiaHoy(): Long = inicioDeDia(System.currentTimeMillis())
    fun finDeDiaHoy(): Long    = finDeDia(System.currentTimeMillis())

    /**
     * Convierte el timestamp que entrega el DatePicker de Material3 a la
     * medianoche LOCAL del día seleccionado.
     *
     * El problema: el DatePicker siempre devuelve MEDIANOCHE UTC del día que
     * el usuario tocó en la pantalla (p. ej. "29 abr" → 2026-04-29T00:00:00Z).
     * Si simplemente pasamos ese valor a Calendar.getInstance(), Calendar lo
     * interpreta en hora local y, en zonas UTC- (como Perú UTC-5), retrocede
     * al día anterior (Apr 29 00:00 UTC = Apr 28 19:00 PET → Calendar lo pisa
     * a Apr 28 00:00 PET, un día menos del que el usuario eligió).
     *
     * Solución: leemos la fecha (año/mes/día) interpretando el timestamp en UTC,
     * y luego construimos medianoche LOCAL de esa misma fecha. Así el día
     * mostrado en el selector siempre coincide con el día usado en el filtro,
     * sin importar la zona horaria del dispositivo.
     */
    fun desdeDatePicker(utcMidnightMs: Long): Long {
        // 1. Leer la fecha en UTC para obtener el día correcto
        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = utcMidnightMs
        }
        val year  = utcCal.get(Calendar.YEAR)
        val month = utcCal.get(Calendar.MONTH)
        val day   = utcCal.get(Calendar.DAY_OF_MONTH)

        // 2. Construir medianoche LOCAL de esa fecha
        return Calendar.getInstance().apply {
            set(year, month, day, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
