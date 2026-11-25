package com.example.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.DetectedMomentsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LiveDiscoveryScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ScanningRadarScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.StreamConfigScreen
import com.example.ui.screens.VideoStudioScreen
import com.example.ui.viewmodel.BrohiViewModel

object BrohiRoutes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val DISCOVERY = "discovery/{category}"
    const val CONFIG = "config"
    const val RADAR = "radar"
    const val MOMENTS = "moments"
    const val STUDIO = "studio/{shortId}"
    const val SETTINGS = "settings"

    fun discovery(category: String) = "discovery/${Uri.encode(category)}"
    fun studio(shortId: String) = "studio/${Uri.encode(shortId)}"
}

@Composable
fun BrohiAppNavigation(
    viewModel: BrohiViewModel,
    sharedTextUrl: String?,
    sharedVideoUri: Uri?,
    onConsumedShareIntent: () -> Unit
) {
    val navController = rememberNavController()
    val settings by viewModel.settingsFlow.collectAsStateWithLifecycle()

    LaunchedEffect(sharedTextUrl, sharedVideoUri) {
        if (!sharedTextUrl.isNullOrBlank()) {
            viewModel.updateUrlInput(sharedTextUrl)
            onConsumedShareIntent()
            navController.navigate(BrohiRoutes.HOME) {
                popUpTo(BrohiRoutes.SPLASH) { inclusive = true }
            }
        } else if (sharedVideoUri != null) {
            onConsumedShareIntent()
            viewModel.importLocalVideoUri(sharedVideoUri) {
                navController.navigate(BrohiRoutes.CONFIG)
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = BrohiRoutes.SPLASH
    ) {
        composable(BrohiRoutes.SPLASH) {
            SplashScreen(
                onboardingCompleted = settings.onboardingCompleted,
                onNavigateNext = { showOnboarding ->
                    val target = if (showOnboarding) BrohiRoutes.ONBOARDING else BrohiRoutes.HOME
                    navController.navigate(target) {
                        popUpTo(BrohiRoutes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(BrohiRoutes.ONBOARDING) {
            OnboardingScreen(
                onFinish = {
                    viewModel.completeOnboarding()
                    navController.navigate(BrohiRoutes.HOME) {
                        popUpTo(BrohiRoutes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(BrohiRoutes.HOME) {
            HomeScreen(
                viewModel = viewModel,
                onOpenConfig = { navController.navigate(BrohiRoutes.CONFIG) },
                onOpenLiveDiscovery = { cat -> navController.navigate(BrohiRoutes.discovery(cat)) },
                onOpenStudio = { id -> navController.navigate(BrohiRoutes.studio(id)) },
                onOpenSettings = { navController.navigate(BrohiRoutes.SETTINGS) }
            )
        }

        composable(
            route = BrohiRoutes.DISCOVERY,
            arguments = listOf(navArgument("category") { type = NavType.StringType })
        ) { backStackEntry ->
            val category = Uri.decode(backStackEntry.arguments?.getString("category") ?: "GTA V")
            LiveDiscoveryScreen(
                viewModel = viewModel,
                initialCategory = category,
                onBack = { navController.popBackStack() },
                onOpenSettings = { navController.navigate(BrohiRoutes.SETTINGS) },
                onStreamSelected = { navController.navigate(BrohiRoutes.CONFIG) }
            )
        }

        composable(BrohiRoutes.CONFIG) {
            StreamConfigScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onStartScan = { navController.navigate(BrohiRoutes.RADAR) }
            )
        }

        composable(BrohiRoutes.RADAR) {
            ScanningRadarScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onScanComplete = {
                    navController.navigate(BrohiRoutes.MOMENTS) {
                        popUpTo(BrohiRoutes.RADAR) { inclusive = true }
                    }
                }
            )
        }

        composable(BrohiRoutes.MOMENTS) {
            DetectedMomentsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onOpenStudio = { shortId ->
                    navController.navigate(BrohiRoutes.studio(shortId)) {
                        popUpTo(BrohiRoutes.HOME) { inclusive = false }
                    }
                },
                onFinishedToVault = {
                    navController.popBackStack(BrohiRoutes.HOME, inclusive = false)
                }
            )
        }

        composable(
            route = BrohiRoutes.STUDIO,
            arguments = listOf(navArgument("shortId") { type = NavType.StringType })
        ) { backStackEntry ->
            val shortId = Uri.decode(backStackEntry.arguments?.getString("shortId") ?: "")
            VideoStudioScreen(
                viewModel = viewModel,
                shortId = shortId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(BrohiRoutes.SETTINGS) {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
