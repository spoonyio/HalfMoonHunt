package com.example.halfmoonhunt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.halfmoonhunt.ui.ClueScreen
import com.example.halfmoonhunt.ui.PermissionsScreen
import com.example.halfmoonhunt.ui.StartScreen
import com.example.halfmoonhunt.ui.theme.HalfMoonHuntTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setContent {
            HalfMoonHuntTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "permissions"
                ) {
                    composable("permissions") {
                        PermissionsScreen(
                            onGranted = {
                                navController.navigate("start") {
                                    popUpTo("permissions") { inclusive = true }
                                }
                            },

                        )
                    }
                    composable("start") {
                        StartScreen(
                            onStart = { navController.navigate("clue") }
                        )
                    }
                    composable("clue") {
                        ClueScreen(
                            onSolved = {
                            },
                            onQuit = {
                                navController.navigate("start") {
                                    popUpTo("start") { inclusive = true }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
