package com.example.halfmoonhunt

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.example.halfmoonhunt.ui.ClueScreen
import com.example.halfmoonhunt.ui.PermissionsScreen
import com.example.halfmoonhunt.ui.StartScreen
import com.example.halfmoonhunt.ui.theme.HalfMoonHuntTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

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

    // Kotlin implementation of the Haversine formula, modified from source:
    // https://gist.github.com/jferrao/cb44d09da234698a7feee68ca895f491
//    class CalculateDistance(private val lat: Double, private val lon: Double) {
//
//        companion object {
//            const val EARTH_RADIUS_FT: Double = 20903520.0
//        }
//
//        /**
//         * Haversine formula. Giving great-circle distances between two points on a sphere from their longitudes and latitudes.
//         * It is a special case of a more general formula in spherical trigonometry, the law of haversines, relating the
//         * sides and angles of spherical "triangles".
//         *
//         * https://rosettacode.org/wiki/Haversine_formula#Java
//         *
//         * @return Distance in kilometers
//         */
//        fun haversine(destination: CalculateDistance): Double {
//            val dLat = Math.toRadians(destination.lat - this.lat)
//            val dLon = Math.toRadians(destination.lon - this.lon)
//            val originLat = Math.toRadians(this.lat)
//            val destinationLat = Math.toRadians(destination.lat)
//
//            val a = sin(dLat / 2).pow(2.0) +
//                    sin(dLon / 2).pow(2.0) * cos(originLat) * cos(destinationLat)
//            val c = 2 * asin(sqrt(a))
//            return EARTH_RADIUS_FT * c
//        }
//
//    }
}
