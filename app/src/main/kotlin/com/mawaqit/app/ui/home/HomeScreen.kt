package com.mawaqit.app.ui.home

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mawaqit.app.BuildConfig
import com.mawaqit.app.data.model.PrayerName
import com.mawaqit.app.data.model.PrayerTimings
import com.mawaqit.app.util.PrayerStatus
import com.mawaqit.app.util.formatCountdown
import com.mawaqit.app.util.getCurrentPrayerStatus
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

/**
 * PHASE_2 temporary home screen — exists purely to prove the data pipeline
 * (location → AlAdhan → Room → screen). PHASE_4 replaces it with the real
 * design from DESIGN.md. Buttons are plain on purpose.
 */
@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) viewModel.useMyLocation()
    }

    if (state.needsLocation) {
        LocationSetupContent(
            state = state,
            onUseLocation = {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            },
            onKarachi = viewModel::testKarachi
        )
    } else {
        TimesContent(state)
    }
}

// ── state 1: no location yet ─────────────────────────────────────────────────

@Composable
private fun LocationSetupContent(
    state: HomeUiState,
    onUseLocation: () -> Unit,
    onKarachi: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Mawaqit", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            "Salah times, prayer alarms & more",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.height(32.dp))

        if (state.isLoading) {
            CircularProgressIndicator()
        } else {
            Button(onClick = onUseLocation, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("Use My Location")
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onKarachi, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("Test with Karachi")
            }
        }

        state.error?.let { error ->
            Spacer(Modifier.height(12.dp))
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(24.dp))
        BuildStamp()
    }
}

// ── state 2: times visible ───────────────────────────────────────────────────

@Composable
private fun TimesContent(state: HomeUiState) {
    val timings = state.timings ?: return

    // 1-second ticker so the countdown stays live
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1000)
        }
    }

    val statuses = remember(timings, nowMillis) { getCurrentPrayerStatus(timings, nowMillis) }
    val dateText = remember(timings.date) { formatDateIso(timings.date) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Mawaqit", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        state.cityName?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        }
        Text(dateText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)

        if (state.fromCache) {
            Spacer(Modifier.height(8.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Text(
                    "Cached data — couldn't reach the server, showing last saved times",
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Next-prayer highlight card
        state.nextPrayer?.let { next ->
            Spacer(Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("Next prayer", style = MaterialTheme.typography.labelMedium)
                    Text(
                        "${next.name.label()} — ${next.timeStr}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    val until = next.timeMillis - nowMillis
                    if (until > 0) {
                        Text(
                            "in ${formatCountdown(until)}",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Today's five prayers
        listOf(
            PrayerName.FAJR to timings.fajr,
            PrayerName.DHUHR to timings.dhuhr,
            PrayerName.ASR to timings.asr,
            PrayerName.MAGHRIB to timings.maghrib,
            PrayerName.ISHA to timings.isha
        ).forEach { (name, timeStr) ->
            val color = when (statuses[name]) {
                PrayerStatus.CURRENT -> MaterialTheme.colorScheme.primary
                PrayerStatus.PRAYED, PrayerStatus.MISSED -> MaterialTheme.colorScheme.outline
                else -> MaterialTheme.colorScheme.onSurface
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(name.label(), color = color, fontWeight = if (statuses[name] == PrayerStatus.CURRENT) FontWeight.Bold else null)
                Text(timeStr, color = color, fontWeight = if (statuses[name] == PrayerStatus.CURRENT) FontWeight.Bold else null)
            }
            HorizontalDivider()
        }

        Spacer(Modifier.height(24.dp))
        BuildStamp()
    }
}

@Composable
private fun BuildStamp() {
    Text(
        "Build: ${BuildConfig.GIT_SHA}",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outline
    )
}

// ── tiny helpers ─────────────────────────────────────────────────────────────

private fun PrayerName.label(): String =
    name.lowercase().replaceFirstChar { it.uppercase() }

/** "2026-09-15" → "15 Sep 2026" (display only — storage stays ISO). */
private fun formatDateIso(iso: String): String = try {
    LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("d MMM yyyy"))
} catch (e: Exception) {
    iso
}
