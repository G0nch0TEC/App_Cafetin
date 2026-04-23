package com.proyecto.cafetin.util

import java.util.Calendar

object DateUtils {

    private fun baseCalendar(fechaMs: Long) = Calendar.getInstance().apply {
        timeInMillis = fechaMs
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    fun inicioDeDia(fechaMs: Long): Long =
        baseCalendar(fechaMs).timeInMillis

    fun finDeDia(fechaMs: Long): Long =
        baseCalendar(fechaMs).apply {
            add(Calendar.DAY_OF_MONTH, 1)
        }.timeInMillis

    fun inicioDeDiaHoy(): Long = inicioDeDia(System.currentTimeMillis())
    fun finDeDiaHoy(): Long = finDeDia(System.currentTimeMillis())
}
