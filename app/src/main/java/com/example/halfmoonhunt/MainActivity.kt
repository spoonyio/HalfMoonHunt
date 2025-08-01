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
                        } ?: Text(text = "Location not available")
                    }
                }
            }
        }
    }
}
