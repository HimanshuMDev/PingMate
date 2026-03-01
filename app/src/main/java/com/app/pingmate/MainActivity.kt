package com.app.pingmate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.app.pingmate.presentation.navigation.PingMateNavGraph
import com.app.pingmate.presentation.navigation.Screen
import com.app.pingmate.presentation.screen.onboarding.isNotificationServiceEnabled
import com.app.pingmate.ui.theme.PingMateTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val prefs = getSharedPreferences("PingMatePrefs", android.content.Context.MODE_PRIVATE)
        val isOnboardingComplete = prefs.getBoolean("onboarding_complete", false)
        val hasPermission = isNotificationServiceEnabled(this)

        val startDestination = if (isOnboardingComplete && hasPermission) {
            Screen.Home.route
        } else if (hasPermission) {
            Screen.ChooseApps.route
        } else {
            Screen.Welcome.route
        }

        setContent {
            PingMateTheme {
                val navController = rememberNavController()
                PingMateNavGraph(
                    navController = navController,
                    startDestination = startDestination
                )
            }
        }
    }
}