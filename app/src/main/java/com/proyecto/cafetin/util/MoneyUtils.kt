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

    /** Formatea un precio positivo en centavos como "S/X.XX". */
    fun Long.toPrecioTexto(): String = "S/${this / 100}.${"%02d".format(this % 100)}"
}
