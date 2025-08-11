package com.example.halfmoonhunt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.halfmoonhunt.model.SolvedInfo
import com.example.halfmoonhunt.ui.ClueScreen
import com.example.halfmoonhunt.ui.CompletedScreen
import com.example.halfmoonhunt.ui.PermissionsScreen
import com.example.halfmoonhunt.ui.SolvedClueScreen
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
                val huntVm: HuntViewModel = viewModel()

                NavHost(
                    navController = navController,
                    startDestination = "permissions"
                ) {
                    composable("permissions") {
                        PermissionsScreen(
                            onGranted = {
                                huntVm.loadClues()
                                navController.navigate("start") {
                                    popUpTo("permissions") { inclusive = true }
                                }
                            },

                        )
                    }
                    composable("start") {
                        StartScreen(
                            onStart = {
                                huntVm.resetHunt()
                                huntVm.startTimer()
                                navController.navigate("clue")
                            }
                        )
                    }
                    composable("clue") {
                        ClueScreen(
                            huntVm = huntVm,
                            onSolved = {
                                huntVm.pauseTimer()
                                if (huntVm.isLastClue()) {
                                    navController.navigate("completed") {
                                        popUpTo("clue") { inclusive = true }
                                        launchSingleTop = true
                                    }
                                } else {
                                    navController.navigate("solved")
                                }
                            },
                            onQuit = {
                                huntVm.stopTimer()
                                navController.navigate("start") {
                                    popUpTo("start") { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("solved") {
                        val current = huntVm.currentSolved()
                        val displayed = remember { current }

                        if (displayed != null) {
                            SolvedClueScreen(
                                solvedClue = displayed,
                                onContinue = {
                                    val finished = huntVm.advance()
                                    if (finished) {
                                        navController.navigate("completed") {
                                            popUpTo("clue") { inclusive = true }
                                        }
                                    } else {
                                        huntVm.startTimer()
                                        navController.navigate("clue") {
                                            popUpTo("clue") { inclusive = true }
                                        }
                                    }
                                }
                            )
                        } else {
                            navController.popBackStack()
                        }
                    }

                    composable("completed") {
                        CompletedScreen(
                            huntVm = huntVm,
                            onHome = {
                                huntVm.stopTimer()
                                navController.navigate("start") { popUpTo("start") { inclusive = true } }
                            }
                        )
                    }
                }
            }
        }
    }
}
