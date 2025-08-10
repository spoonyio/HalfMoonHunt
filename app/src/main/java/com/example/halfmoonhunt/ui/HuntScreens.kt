package com.example.halfmoonhunt.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlin.math.*

@Composable
fun PermissionsScreen(onGranted: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val permissionGranted = remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted.value = granted
        if (granted) onGranted()
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            onGranted()
        }
    }

    Scaffold { padding ->
        Column(Modifier
            .padding(padding)
            .padding(8.dp)) {
            Text("To play the game, we need your location to confirm when you've found each clue.")
            Spacer(Modifier.height(8.dp))
            Button(onClick = { launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }) {
                Text("Allow Access")
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { (context as? Activity)?.finish() }) {
                Text("Exit App")
            }
        }
    }
}

@Composable
fun StartScreen(
    onStart: () -> Unit,
) {
    Scaffold { padding ->
        Column(Modifier
            .padding(padding)
            .padding(16.dp)) {
            Text("Half Moon Hunt", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                "Rules:\n• The goal of the game is to reach the real world locations hinted by the clues as quickly as possible.\n" +
                        "• The timer starts when you press the “Start game” button.\n" +
                        "• Each clue is presented on a dedicated Clue page, where you’ll be given a textual clue to a real world location."
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) { Text("Start Game") }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun ClueScreen(
    onSolved: () -> Unit,
    onQuit: () -> Unit
) {
    val context = LocalContext.current
    val fused = remember { LocationServices.getFusedLocationProviderClient(context) }
    val targetLat = 37.501630
    val targetLon = -122.496700
    val threshold = 500.0

    var showHint by remember { mutableStateOf(false) }
    var lastLoc by remember { mutableStateOf<Location?>(null) }
    var distanceMsg by remember { mutableStateOf<String?>(null) }
    var showIncorrect by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Scaffold { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            Text("Clue", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            Text("Not all giants walk the land... some crash into cliffs.")
            Spacer(Modifier.height(12.dp))
            Row {
                OutlinedButton(onClick = { showHint = !showHint }) {
                    Text(if (showHint) "Hide Hint" else "Show Hint")
                }
            }

            if (showHint) {
                Spacer(Modifier.height(8.dp))
                Text("Hint: Additional Hint", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    if (!locationPermission(context)) {
                        errorMsg = "Location permission not granted."
                        return@Button
                    }
                    val cts = com.google.android.gms.tasks.CancellationTokenSource()
                    fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                        .addOnSuccessListener { loc ->
                            lastLoc = loc
                            if (loc == null) {
                                distanceMsg = "Location unavailable. Set a mock location in the emulator."
                                return@addOnSuccessListener
                            }
                            val feet = haversine(loc.latitude, loc.longitude, targetLat, targetLon)
                            distanceMsg = "Distance to target: ${"%.1f".format(feet)} ft"
                            if (feet <= threshold) onSolved() else showIncorrect = true
                        }
                        .addOnFailureListener { e ->
                            errorMsg = "Location error: ${e.message}"
                        }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Found It!") }
            if (showIncorrect) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { showIncorrect = false },
                    confirmButton = { TextButton(onClick = { showIncorrect = false }) { Text("OK") } },
                    title = { Text("Hmmm... this isn't it,") },
                    text = {
                        Column {
                            lastLoc?.let {
                                Text("Your location: ${it.latitude}, ${it.longitude}")
                            }
                            distanceMsg?.let {
                                Text(it)
                            }
                            Text("Trust your inner compass and try again!")
                        }
                    }
                )
            } else
            Spacer(Modifier.height(12.dp))
            errorMsg?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onQuit) { Text("Quit") }
        }
    }
}

