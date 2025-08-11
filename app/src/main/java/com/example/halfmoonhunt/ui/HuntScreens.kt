package com.example.halfmoonhunt.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.halfmoonhunt.HuntViewModel
import com.example.halfmoonhunt.model.SolvedInfo
import com.example.halfmoonhunt.utils.formatTime
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.example.halfmoonhunt.utils.haversine
import com.example.halfmoonhunt.utils.locationPermission

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
    huntVm: HuntViewModel,
    onSolved: () -> Unit,
    onQuit: () -> Unit
) {
    val context = LocalContext.current
    val fused = remember { LocationServices.getFusedLocationProviderClient(context) }

    val clues by huntVm.clues.collectAsState()
    val index by huntVm.currentIndex.collectAsState()
    val elapsed by huntVm.timer.collectAsState()

    val clue = clues.getOrNull(index) ?: run {
        Scaffold { padding -> Column(Modifier.padding(padding).padding(16.dp)) { Text("Loading clue...") } }
        return
    }


    var showHint by remember { mutableStateOf(false) }
    var lastLoc by remember { mutableStateOf<Location?>(null) }
    var distanceMsg by remember { mutableStateOf<String?>(null) }
    var showIncorrect by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Scaffold { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            Text("Timer: ${elapsed.formatTime()}", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            Text("Clue ${index + 1} of ${clues.size}", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            Text(clue.text)
            Spacer(Modifier.height(12.dp))

            Row { OutlinedButton(onClick = { showHint = !showHint }) { Text(if (showHint) "Hide Hint" else "Show Hint") } }

            if (showHint) {
                Spacer(Modifier.height(8.dp))
                Text("Hint: ${clue.hint}", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    if (!locationPermission(context)) { errorMsg = "Location permission not granted."; return@Button }
                    val cts = com.google.android.gms.tasks.CancellationTokenSource()
                    fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                        .addOnSuccessListener { loc ->
                            lastLoc = loc
                            if (loc == null) { distanceMsg = "Location unavailable."; return@addOnSuccessListener }
                            val feet = haversine(loc.latitude, loc.longitude, clue.lat, clue.lon)
                            distanceMsg = "Distance from target: ${"%.1f".format(feet)} ft"
                            if (feet <= clue.threshold) onSolved() else showIncorrect = true
                        }
                        .addOnFailureListener { e -> errorMsg = "Location error: ${e.message}" }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Found It!") }

            Spacer(Modifier.height(12.dp))
            errorMsg?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            if (showIncorrect) {
                AlertDialog(
                    onDismissRequest = { showIncorrect = false },
                    confirmButton = { TextButton(onClick = { showIncorrect = false }) { Text("OK") } },
                    title = { Text("Hmmm... this isn't it") },
                    text = {
                        Column {
                            distanceMsg?.let { Text(it) }
                            Text("Try again!")
                        }
                    }
                )
            }
            TextButton(onClick = onQuit) { Text("Quit") }
        }
    }
}

@Composable
fun SolvedClueScreen(
    huntVm: HuntViewModel,
    solvedClue: SolvedInfo,
    onContinue: () -> Unit
) {
    LaunchedEffect(Unit) { huntVm.pauseTimer() }
    val elapsed by huntVm.timer.collectAsState()
    Scaffold { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            Text("Timer: ${elapsed.formatTime()} (paused)", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(solvedClue.title, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text(solvedClue.body)
            Spacer(Modifier.height(8.dp))
            Text(solvedClue.facts)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onContinue) { Text("Next Clue") }
        }
    }
}

@Composable
fun CompletedScreen(huntVm: HuntViewModel, onHome: () -> Unit) {
    val elapsed by huntVm.timer.collectAsState()
    val solved = remember { huntVm.currentSolved()}
    Scaffold { p ->
        Column(Modifier.padding(p).padding(16.dp)) {
            Text("Treasure Hunt Completed!", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            Text("Total time: ${elapsed.formatTime()}")
            Spacer(Modifier.height(16.dp))

            solved?.let {
                Text(it.title, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(8.dp))
                Text(it.body)
                Spacer(Modifier.height(8.dp))
                if (it.facts.isNotEmpty()) {
                    Text(it.facts)
                    Spacer(Modifier.height(16.dp))
                }
            }

            Button(onClick = onHome, modifier = Modifier.fillMaxWidth()) { Text("Home") }
        }
    }
}