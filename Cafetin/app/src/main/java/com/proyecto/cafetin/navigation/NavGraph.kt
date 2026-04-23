package com.proyecto.cafetin.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.proyecto.cafetin.ui.detalle.DetalleScreen
import com.proyecto.cafetin.ui.historial.HistorialScreen
import com.proyecto.cafetin.ui.personas.PersonasScreen

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "personas") {
        composable("personas") {
            PersonasScreen(
                onPersonaClick   = { id -> navController.navigate("detalle/$id") },
                onHistorialClick = { navController.navigate("historial") }
            )
        }
        composable(
            route = "detalle/{personaId}",
            arguments = listOf(navArgument("personaId") { type = NavType.IntType })
        ) { backStack ->
            val personaId = backStack.arguments!!.getInt("personaId")
            DetalleScreen(
                personaId = personaId,
                onBack    = { navController.popBackStack() }
            )
        }
        composable("historial") {
            HistorialScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
