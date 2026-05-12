package com.proyecto.cafetin.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.proyecto.cafetin.ui.backup.BackupScreen
import com.proyecto.cafetin.ui.catalogo.CatalogoScreen
import com.proyecto.cafetin.ui.detalle.DetalleScreen
import com.proyecto.cafetin.ui.historial.HistorialScreen
import com.proyecto.cafetin.ui.personas.PersonasScreen

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.PERSONAS) {
        composable(Routes.PERSONAS) {
            PersonasScreen(
                onPersonaClick = { id -> navController.navigate(Routes.detalle(id)) },
                onHistorialClick = { navController.navigate(Routes.HISTORIAL) },
                onBackupClick = { navController.navigate(Routes.BACKUP) }
            )
        }

        composable(
            route = Routes.DETALLE,
            arguments = listOf(navArgument("personaId") { type = NavType.IntType })
        ) { backStack ->
            val personaId = backStack.arguments!!.getInt("personaId")
            DetalleScreen(
                personaId = personaId,
                onBack = { navController.popBackStack() },
                onGestionarCatalogo = { navController.navigate(Routes.CATALOGO) }
            )
        }

        composable(Routes.HISTORIAL) {
            HistorialScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.CATALOGO) {
            CatalogoScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.BACKUP) {
            BackupScreen(onBack = { navController.popBackStack() })
        }
    }
}
