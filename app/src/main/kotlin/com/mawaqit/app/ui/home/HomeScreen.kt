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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mawaqit.app.BuildConfig
import com.mawaqit.app.R
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
 *
 * PHASE_3 additions (BOTH temporary, removed in PHASE_4):
 *  1. One-time POST_NOTIFICATIONS request (Android 13+) — without it the azan
 *     notification never appears and the phone checks can't be verified.
 *  2. Five alarm ON/OFF switches — needed to test "disabled alarm never fires".
 */
@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) viewModel.useMyLocation()
    }

    // ── PHASE_3 temp helper #1: notification permission, Android 13+ only ──
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // Denied: alarms still fire audio/vibration; banner just won't show.
    }
    LaunchedEffect(Unit) {
        if (viewModel.needsNotificationPermission()) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    if (state.needsLocation) {
        LocationSetupContent(
            state = state,
            onUseLocation = {
                locationLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            },
            onKarachi = viewModel::testKarachi
        )
    } else {
        TimesContent(state, onToggleAlarm = viewModel::toggleAlarm)
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
private fun TimesContent(
    state: HomeUiState,
    onToggleAlarm: (PrayerName, Boolean) -> Unit
) {
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(name.label(), color = color, fontWeight = if (statuses[name] == PrayerStatus.CURRENT) FontWeight.Bold else null)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(timeStr, color = color, fontWeight = if (statuses[name] == PrayerStatus.CURRENT) FontWeight.Bold else null)
                    Spacer(Modifier.width(12.dp))
                    AlarmToggle(name, state.alarmStates[name], onToggleAlarm)
                }
            }
            HorizontalDivider()
        }

        Spacer(Modifier.height(24.dp))
        BuildStamp()
    }
}

// ── PHASE_3 temp helper #2: per-prayer alarm switch (removed in PHASE_4) ─────

@Composable
private fun AlarmToggle(
    prayer: PrayerName,
    enabled: Boolean?,
    onToggleAlarm: (PrayerName, Boolean) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Switch(
            checked = enabled ?: true, // default ON while the flow loads
            onCheckedChange = { checked -> onToggleAlarm(prayer, checked) }
        )
        Text(
            if (enabled == false) {
                stringResource(R.string.alarm_is_off)
            } else {
                stringResource(R.string.alarm_is_on)
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
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
