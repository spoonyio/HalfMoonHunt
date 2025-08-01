package com.example.halfmoonhunt

import android.Manifest
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
import androidx.core.content.ContextCompat
import com.example.halfmoonhunt.ui.theme.HalfMoonHuntTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setContent {
            HalfMoonHuntTheme {
                var location = remember { mutableStateOf<Location?>(null) }
                var permissionGranted = remember { mutableStateOf(false) }

                val locationPermissionRequest = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted: Boolean ->
                    permissionGranted.value = isGranted
                }

                // Test target location: Ocean Lawn
                val targetLatitude = 37.4333
                val targetLongitude = -122.4424

                LaunchedEffect(Unit) {
                    when {
                        ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED -> {
                            permissionGranted.value = true
                        }

                        else -> {
                            locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    }
                }

                Scaffold {  paddingValues ->
                    Column(modifier = Modifier
                        .padding(paddingValues)
                        .padding(16.dp)
                    ) {
                        Button(onClick = {
                            if (permissionGranted.value) {
                                fusedLocationClient.getCurrentLocation(
                                    Priority.PRIORITY_HIGH_ACCURACY,
                                    null
                                ).addOnSuccessListener { loc: Location? ->
                                    location.value = loc
                                }
                            }
                        }) {
                            Text(text = "Get Current Location")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(onClick = {
                            if (permissionGranted.value) {
                                fusedLocationClient.lastLocation
                                    .addOnSuccessListener { loc: Location? ->
                                        location.value = loc
                                    }
                            }
                        }) {
                            Text(text = "Get Location")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        location.value?.let {
                            Text(text = "Latitude: ${it.latitude}, Longitude: ${it.longitude}")

                            val current = CalculateDistance(it.latitude, it.longitude)
                            val target = CalculateDistance(targetLatitude, targetLongitude)
                            val distanceFt = current.haversine(target)

                            Text(text = "Distance to target: ${"%.1f".format(distanceFt)} feet")
                        } ?: Text(text = "Location not available")
                    }
                }
            }
        }
    }

    // Kotlin implementation of the Haversine formula, modified from source:
    // https://gist.github.com/jferrao/cb44d09da234698a7feee68ca895f491
    class CalculateDistance(private val lat: Double, private val lon: Double) {

        companion object {
            const val EARTH_RADIUS_FT: Double = 20903520.0
        }

        /**
         * Haversine formula. Giving great-circle distances between two points on a sphere from their longitudes and latitudes.
         * It is a special case of a more general formula in spherical trigonometry, the law of haversines, relating the
         * sides and angles of spherical "triangles".
         *
         * https://rosettacode.org/wiki/Haversine_formula#Java
         *
         * @return Distance in kilometers
         */
        fun haversine(destination: CalculateDistance): Double {
            val dLat = Math.toRadians(destination.lat - this.lat)
            val dLon = Math.toRadians(destination.lon - this.lon)
            val originLat = Math.toRadians(this.lat)
            val destinationLat = Math.toRadians(destination.lat)

            val a = Math.pow(Math.sin(dLat / 2), 2.0) +
                    Math.pow(Math.sin(dLon / 2), 2.0) * Math.cos(originLat) * Math.cos(destinationLat)
            val c = 2 * Math.asin(Math.sqrt(a))
            return EARTH_RADIUS_FT * c
        }

    }
}
