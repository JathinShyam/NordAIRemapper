package com.nordairemapper.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.nordairemapper.presentation.detection.EnableDetectionScreen
import com.nordairemapper.presentation.home.HomeScreen
import com.nordairemapper.presentation.onboarding.OnboardingScreen
import com.nordairemapper.presentation.backup.BackupScreen
import com.nordairemapper.presentation.overlay.OverlaySettingsScreen
import com.nordairemapper.presentation.remap.RemapScreen
import com.nordairemapper.presentation.settings.AppearanceSettingsScreen
import com.nordairemapper.presentation.settings.ExclusionsScreen
import com.nordairemapper.presentation.settings.FeedbackScreen
import com.nordairemapper.presentation.settings.LockScreenSettingsScreen
import com.nordairemapper.presentation.settings.PermissionsScreen
import com.nordairemapper.presentation.settings.SettingsScreen
import com.nordairemapper.presentation.settings.VisualOverlayScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val KEY_LEARNING = "key_learning"
    const val DEVELOPER = "developer"
    const val ENABLE_DETECTION = "enable_detection"
    const val SETTINGS = "settings"
    const val OVERLAY_SETTINGS = "overlay_settings"
    const val BACKUP = "backup"
    const val FEEDBACK = "feedback"
    const val VISUAL_OVERLAY = "visual_overlay"
    const val LOCK_SCREEN = "lock_screen"
    const val EXCLUSIONS = "exclusions"
    const val APPEARANCE = "appearance"
    const val PERMISSIONS = "permissions"
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
    // Freeze the start destination for this composition lifetime: flipping
    // onboardingCompleted mid-session would otherwise rebuild the graph with a
    // different start and leave stale destinations on the back stack (user
    // report: Back from Home reopened the onboarding finish page).
    val start = remember { if (onboardingCompleted) Routes.HOME else Routes.ONBOARDING }

    NavHost(navController = navController, startDestination = start) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(Routes.HOME) {
                        // Wipe everything below Home — no onboarding leftovers.
                        popUpTo(0) { inclusive = true }
                    }
                },
                // Unlock is embedded in onboarding page 2 — no subpage detour.
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                onOpenRemap = { pressType -> navController.navigate(Routes.remap(pressType)) },
                onOpenKeyLearning = { navController.navigate(Routes.KEY_LEARNING) },
                onOpenDeveloper = { navController.navigate(Routes.DEVELOPER) },
                onOpenEnableDetection = { navController.navigate(Routes.ENABLE_DETECTION) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenOverlaySettings = { navController.navigate(Routes.OVERLAY_SETTINGS) },
                onOpenBackup = { navController.navigate(Routes.BACKUP) },
            )
        }
        composable(Routes.KEY_LEARNING) {
            KeyLearningScreen(
                onBack = { navController.popBackStack() },
                onOpenEnableDetection = { navController.navigate(Routes.ENABLE_DETECTION) },
            )
        }
        composable(Routes.ENABLE_DETECTION) {
            EnableDetectionScreen(
                onBack = { navController.popBackStack() },
                onContinue = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(Routes.DEVELOPER) {
            DeveloperScreen(
                onBack = { navController.popBackStack() },
                onOpenKeyLearning = { navController.navigate(Routes.KEY_LEARNING) },
                onOpenEnableDetection = { navController.navigate(Routes.ENABLE_DETECTION) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenDeveloper = { navController.navigate(Routes.DEVELOPER) },
                onOpenKeyLearning = { navController.navigate(Routes.KEY_LEARNING) },
                onOpenBackup = { navController.navigate(Routes.BACKUP) },
                onOpenOverlay = { navController.navigate(Routes.OVERLAY_SETTINGS) },
                onOpenFeedback = { navController.navigate(Routes.FEEDBACK) },
                onOpenVisualOverlay = { navController.navigate(Routes.VISUAL_OVERLAY) },
                onOpenLockScreen = { navController.navigate(Routes.LOCK_SCREEN) },
                onOpenExclusions = { navController.navigate(Routes.EXCLUSIONS) },
                onOpenAppearance = { navController.navigate(Routes.APPEARANCE) },
                onOpenPermissions = { navController.navigate(Routes.PERMISSIONS) },
                onRestartOnboarding = {
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.FEEDBACK) {
            FeedbackScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.VISUAL_OVERLAY) {
            VisualOverlayScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.LOCK_SCREEN) {
            LockScreenSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.EXCLUSIONS) {
            ExclusionsScreen(
                onBack = { navController.popBackStack() },
                onOpenEnableDetection = { navController.navigate(Routes.ENABLE_DETECTION) },
            )
        }
        composable(Routes.APPEARANCE) {
            AppearanceSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.PERMISSIONS) {
            PermissionsScreen(
                onBack = { navController.popBackStack() },
                onOpenEnableDetection = { navController.navigate(Routes.ENABLE_DETECTION) },
            )
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
