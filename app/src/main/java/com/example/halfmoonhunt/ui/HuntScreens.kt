package com.example.halfmoonhunt.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.halfmoonhunt.HuntViewModel
import com.example.halfmoonhunt.R
import com.example.halfmoonhunt.model.SolvedInfo
import com.example.halfmoonhunt.utils.formatTime
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.example.halfmoonhunt.utils.haversine
import com.example.halfmoonhunt.utils.locationPermission

@Composable
fun PermissionsScreen(onGranted: () -> Unit) {
    val context = LocalContext.current
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

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.share_your_location), style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.location_permission_body_text),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 40.dp)
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp)
                    ) {
                    Text(stringResource(R.string.allow_access))
                }
                TextButton(onClick = { (context as? Activity)?.finish() }) {
                    Text(stringResource(R.string.exit_app))
                }
            }
        }
    }
}

@Composable
fun StartScreen(
    rules: List<String>,
    onStart: () -> Unit,
) {
    Scaffold(
        containerColor = Color(0xFFD9BBA0)
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Card(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(Modifier.padding(32.dp)) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.welcome_header),
                        style = MaterialTheme.typography.headlineLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.welcome_body_text))
                    Spacer(Modifier.height(16.dp))
                    RulesBox(rules = rules, modifier = Modifier.padding(top = 8.dp))
                    Spacer(Modifier.height(24.dp))
                    Text(
                        stringResource(R.string.start_game_button_caption),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onStart,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.start_game)) }
                    Spacer(Modifier.height(16.dp))
                }
            }
            Text(
                text = stringResource(R.string.half_moon_hunt),
                fontFamily = FontFamily(Font(R.font.yellowtail_regular)),
                fontSize = 36.sp,
                color = Color.White,
                modifier = Modifier.offset(y = (8).dp)
            )
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
        Scaffold { padding -> Column(Modifier
            .padding(padding)
            .padding(16.dp)) { Text(stringResource(R.string.loading_clue)) } }
        return
    }


    var showHint by remember { mutableStateOf(false) }
    var distanceMsg by remember { mutableStateOf<String?>(null) }
    var showIncorrect by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Scaffold { padding ->
        Column(Modifier
            .padding(padding)
            .padding(24.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.timer_colon, elapsed.formatTime()), style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.height(8.dp))

            Text(
                text = context.getString(R.string.clue_progress, index + 1, clues.size),
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(6.dp))
            Text(clue.text,  style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(12.dp))

            Row { OutlinedButton(onClick = {
                showHint = !showHint }) {
                    Text(if (showHint) stringResource(R.string.hide_hint)
                        else stringResource(R.string.show_hint)
                    )
                }
            }

            if (showHint) {
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.hint_colon, clue.hint), style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    if (!locationPermission(context)) { errorMsg =
                        context.getString(R.string.location_permission_not_granted); return@Button }
                    val cts = com.google.android.gms.tasks.CancellationTokenSource()
                    fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                        .addOnSuccessListener { loc ->
                            if (loc == null) { distanceMsg =
                                context.getString(R.string.location_unavailable); return@addOnSuccessListener }
                            val feet = haversine(loc.latitude, loc.longitude, clue.lat, clue.lon)
                            distanceMsg = context.getString(
                                R.string.distance_from_target_ft,
                                "%.1f".format(feet)
                            )
                            if (feet <= clue.threshold) onSolved() else showIncorrect = true
                        }
                        .addOnFailureListener { e -> errorMsg =
                            context.getString(R.string.location_error, e.message) }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.found_it)) }

            errorMsg?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            if (showIncorrect) {
                AlertDialog(
                    onDismissRequest = { showIncorrect = false },
                    confirmButton = { TextButton(onClick = { showIncorrect = false }) { Text(
                        stringResource(R.string.ok)
                    ) } },
                    title = { Text(stringResource(R.string.hmmm_this_isn_t_it)) },
                    text = {
                        Column {
                            distanceMsg?.let { Text(it) }
                            Text(stringResource(R.string.try_again))
                        }
                    }
                )
            }
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TextButton(onClick = onQuit) {
                    Text(stringResource(R.string.quit))
                }
            }
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
        Column(Modifier
            .padding(padding)
            .padding(16.dp)) {
            Text(stringResource(R.string.timer_colon, elapsed.formatTime()), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(solvedClue.title, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text(solvedClue.body)
            Spacer(Modifier.height(8.dp))
            Text(solvedClue.facts)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onContinue) { Text(stringResource(R.string.next_clue)) }
        }
    }
}

@Composable
fun CompletedScreen(huntVm: HuntViewModel, onHome: () -> Unit) {
    val elapsed by huntVm.timer.collectAsState()
    val solved = remember { huntVm.currentSolved()}
    Scaffold { p ->
        Column(Modifier
            .padding(p)
            .padding(16.dp)) {
            Text(stringResource(R.string.treasure_hunt_completed), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.total_time, elapsed.formatTime()))
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

            Button(onClick = onHome, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.home)) }
        }
    }
}

@Composable
fun RulesBox(rules: List<String>, modifier: Modifier = Modifier) {
    val scroll = rememberScrollState()
    Text(stringResource(R.string.rules), style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))
    HorizontalDivider(
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
        thickness = 1.dp
    )
    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 160.dp)
            .height(320.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(Modifier
            .padding(9.dp)
            .verticalScroll(scroll)) {
            rules.forEach { rule ->
                Row(Modifier.padding(bottom = 6.dp)) {
                    Text(stringResource(R.string.bullet))
                    Text(rule)
                }
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    HorizontalDivider(
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
        thickness = 1.dp
    )
}