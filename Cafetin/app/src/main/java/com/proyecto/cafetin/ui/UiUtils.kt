package com.proyecto.cafetin.ui

object UiUtils {
    fun iniciales(nombre: String): String =
        nombre.trim().split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
}