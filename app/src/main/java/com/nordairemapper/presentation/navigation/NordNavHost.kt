package com.nordairemapper.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nordairemapper.domain.model.PressType
import com.nordairemapper.presentation.developer.DeveloperScreen
import com.nordairemapper.presentation.developer.KeyLearningScreen
import com.nordairemapper.presentation.home.HomeScreen
import com.nordairemapper.presentation.remap.RemapPlaceholderScreen
import com.nordairemapper.presentation.settings.SettingsPlaceholderScreen

object Routes {
    const val HOME = "home"
    const val KEY_LEARNING = "key_learning"
    const val DEVELOPER = "developer"
    const val SETTINGS = "settings"
    const val REMAP = "remap/{pressType}"

    fun remap(pressType: PressType) = "remap/${pressType.key}"
}

@Composable
fun NordNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onOpenRemap = { pressType -> navController.navigate(Routes.remap(pressType)) },
                onOpenKeyLearning = { navController.navigate(Routes.KEY_LEARNING) },
                onOpenDeveloper = { navController.navigate(Routes.DEVELOPER) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.KEY_LEARNING) {
            KeyLearningScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.DEVELOPER) {
            DeveloperScreen(
                onBack = { navController.popBackStack() },
                onOpenKeyLearning = { navController.navigate(Routes.KEY_LEARNING) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsPlaceholderScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Routes.REMAP,
            arguments = listOf(navArgument("pressType") { type = NavType.StringType }),
        ) { entry ->
            val key = entry.arguments?.getString("pressType") ?: PressType.SINGLE.key
            RemapPlaceholderScreen(
                pressType = PressType.fromKey(key),
                onBack = { navController.popBackStack() },
            )
        }
    }
}
