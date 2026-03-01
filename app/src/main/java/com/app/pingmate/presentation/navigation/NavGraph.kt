package com.app.pingmate.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.app.pingmate.presentation.screen.apps.ChooseAppsScreen
import com.app.pingmate.presentation.screen.dashboard.HomeScreen
import com.app.pingmate.presentation.screen.onboarding.PermissionScreen
import com.app.pingmate.presentation.screen.onboarding.WelcomeScreen
import com.app.pingmate.presentation.screen.settings.SettingsScreen

@Composable
fun PingMateNavGraph(navController: NavHostController, startDestination: String) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(route = Screen.Welcome.route) {
            WelcomeScreen(
                onNavigateNext = {
                    navController.navigate(Screen.Permission.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(route = Screen.Permission.route) {
            PermissionScreen(
                onNavigateNext = {
                    navController.navigate(Screen.ChooseApps.route) {
                        popUpTo(Screen.Permission.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(route = Screen.ChooseApps.route) {
            ChooseAppsScreen(
                onNavigateNext = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.ChooseApps.route) { inclusive = true }
                    }
                }
            )
        }

        composable(route = Screen.ChooseAppsFromSettings.route) {
            ChooseAppsScreen(
                onNavigateNext = { navController.popBackStack() }
            )
        }
        
        composable(route = Screen.Home.route) {
            HomeScreen(
                onNotificationClick = { /* Handle details */ },
                onOpenSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(route = Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToChooseApps = { navController.navigate(Screen.ChooseAppsFromSettings.route) }
            )
        }
    }
}

sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object Permission : Screen("permission")
    object ChooseApps : Screen("choose_apps")
    object ChooseAppsFromSettings : Screen("choose_apps_from_settings")
    object Home : Screen("home")
    object Settings : Screen("settings")
}
