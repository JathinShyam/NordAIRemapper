package com.nordairemapper.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nordairemapper.domain.model.PressType
import com.nordairemapper.presentation.developer.DeveloperScreen
import com.nordairemapper.presentation.developer.KeyLearningScreen
import com.nordairemapper.presentation.home.HomeScreen
import com.nordairemapper.presentation.onboarding.OnboardingScreen
import com.nordairemapper.presentation.backup.BackupScreen
import com.nordairemapper.presentation.overlay.OverlaySettingsScreen
import com.nordairemapper.presentation.remap.RemapScreen
import com.nordairemapper.presentation.settings.SettingsPlaceholderScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val KEY_LEARNING = "key_learning"
    const val DEVELOPER = "developer"
    const val SETTINGS = "settings"
    const val OVERLAY_SETTINGS = "overlay_settings"
    const val BACKUP = "backup"
    const val REMAP = "remap/{pressType}"

    fun remap(pressType: PressType) = "remap/${pressType.key}"
}

@Composable
fun NordNavHost(
    rootViewModel: RootViewModel = hiltViewModel(),
) {
    val onboardingCompleted by rootViewModel.onboardingCompleted.collectAsStateWithLifecycle()

    when (val completed = onboardingCompleted) {
        null -> Loading()
        else -> AppNavHost(onboardingCompleted = completed)
    }
}

@Composable
private fun Loading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun AppNavHost(onboardingCompleted: Boolean) {
    val navController = rememberNavController()
    val start = if (onboardingCompleted) Routes.HOME else Routes.ONBOARDING

    NavHost(navController = navController, startDestination = start) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                onOpenRemap = { pressType -> navController.navigate(Routes.remap(pressType)) },
                onOpenKeyLearning = { navController.navigate(Routes.KEY_LEARNING) },
                onOpenDeveloper = { navController.navigate(Routes.DEVELOPER) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenOverlaySettings = { navController.navigate(Routes.OVERLAY_SETTINGS) },
                onOpenBackup = { navController.navigate(Routes.BACKUP) },
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
        composable(Routes.OVERLAY_SETTINGS) {
            OverlaySettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.BACKUP) {
            BackupScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Routes.REMAP,
            arguments = listOf(navArgument("pressType") { type = NavType.StringType }),
        ) {
            RemapScreen(onBack = { navController.popBackStack() })
        }
    }
}
