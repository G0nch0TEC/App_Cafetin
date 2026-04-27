package com.proyecto.cafetin.util

object NotaUtils {

    fun notaBase(nota: String): String =
        Regex(""" x\d+$""").replace(nota, "").trim()

    fun cantidadDeNota(nota: String): Int =
        Regex(""" x(\d+)$""")
            .find(nota)
            ?.groupValues
            ?.get(1)
            ?.toIntOrNull() ?: 1
}