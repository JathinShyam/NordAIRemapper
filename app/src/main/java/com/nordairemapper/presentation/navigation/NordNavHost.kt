package com.nordairemapper.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nordairemapper.presentation.developer.KeyLearningScreen
import com.nordairemapper.presentation.home.HomeScreen

object Routes {
    const val HOME = "home"
    const val KEY_LEARNING = "key_learning"
}

@Composable
fun NordNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onOpenKeyLearning = { navController.navigate(Routes.KEY_LEARNING) },
            )
        }
        composable(Routes.KEY_LEARNING) {
            KeyLearningScreen(onBack = { navController.popBackStack() })
        }
    }
}
