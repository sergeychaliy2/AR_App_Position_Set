package com.arpositionset.app.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.arpositionset.app.presentation.ar.ArScreen
import com.arpositionset.app.presentation.permissions.CameraPermissionGate
import com.arpositionset.app.presentation.settings.SettingsScreen
import com.arpositionset.app.presentation.theme.ArPositionSetTheme
import dagger.hilt.android.AndroidEntryPoint

private object Routes {
    const val AR = "ar"
    const val SETTINGS = "settings"
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        splash.setKeepOnScreenCondition { false }

        setContent {
            ArPositionSetTheme {
                CameraPermissionGate {
                    val nav = rememberNavController()
                    NavHost(navController = nav, startDestination = Routes.AR) {
                        composable(Routes.AR) {
                            ArScreen(onOpenSettings = { nav.navigate(Routes.SETTINGS) })
                        }
                        composable(Routes.SETTINGS) {
                            SettingsScreen(onBack = { nav.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}
