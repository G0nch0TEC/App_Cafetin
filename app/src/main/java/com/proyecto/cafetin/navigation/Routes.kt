package com.proyecto.cafetin.navigation

object Routes {
    const val PERSONAS = "personas"
    const val HISTORIAL = "historial"
    const val DETALLE = "detalle/{personaId}"

    fun detalle(personaId: Int) = "detalle/$personaId"
}