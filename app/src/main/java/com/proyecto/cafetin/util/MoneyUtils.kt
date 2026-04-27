package com.proyecto.cafetin.util

import kotlin.math.abs

object MoneyUtils {

    fun Long.centavosAtexto(): String {
        val signo = if (this < 0) "-" else ""
        val valor = abs(this)

        return buildString {
            append(signo)
            append("S/")
            append(valor / 100)
            append(".")
            append("%02d".format(valor % 100))
        }
    }
}