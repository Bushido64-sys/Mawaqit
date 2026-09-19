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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.mawaqit.app.ui.components.AyahCard
import com.mawaqit.app.ui.components.LoadingState
import com.mawaqit.app.ui.components.NextPrayerCard
import com.mawaqit.app.ui.components.PrayerRow
import com.mawaqit.app.ui.theme.PrimaryGold
import com.mawaqit.app.util.PrayerStatus
import com.mawaqit.app.util.formatCountdown
import com.mawaqit.app.util.parseTimeToMillis
import java.time.LocalDate

/**
 * PHASE_4: the real home screen (DESIGN.md Screen 3) — NextPrayerCard hero,
 * five PrayerRow checkmarks (salah_log), AyahCard, cached-data banner.
 * The Phase 2/3 temporary screen (test buttons, big switches) is replaced.
 * Kept from Phase 3: the one-time notification permission request and the
 * per-prayer alarm switches (now living on the rows where they belong).
 */
@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) viewModel.useMyLocation()
    }

    // One-time notification permission (Android 13+) — azan banner needs it.
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* denied: alarms still fire; only the banner is hidden */ }
    LaunchedEffect(Unit) {
        if (viewModel.needsNotificationPermission()) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    when {
        // needsLocation == null → still checking saved prefs: show the spinner,
        // NOT the setup screen (prevents the one-frame "Assalamualaikum" flash).
        state.needsLocation == true -> LocationSetupContent(
            isLoading = state.isLoading,
            error = state.error,
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
        state.isLoading && state.timings == null -> LoadingState()
        else -> TimesContent(state, viewModel)
    }
}

// ── state 1: no location yet ─────────────────────────────────────────────────

@Composable
private fun LocationSetupContent(
    isLoading: Boolean,
    error: String?,
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
        Text(
            stringResource(R.string.onboarding_welcome_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.onboarding_welcome_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        PrimaryPill(stringResource(R.string.allow_location), enabled = !isLoading, onClick = onUseLocation)
        Spacer(Modifier.height(12.dp))
        PrimaryPill(stringResource(R.string.test_with_karachi), enabled = !isLoading, onClick = onKarachi)
        error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(24.dp))
        BuildStamp()
    }
}

// ── state 2: the real home screen ────────────────────────────────────────────

@Composable
private fun TimesContent(state: HomeUiState, viewModel: HomeViewModel) {
    val timings = state.timings ?: return
    val now = state.nowMillis

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Header(state)

        if (state.fromCache) {
            Spacer(Modifier.height(8.dp))
            CachedBanner()
        }

        state.nextPrayer?.let { next ->
            Spacer(Modifier.height(16.dp))
            NextPrayerCard(
                prayerLabel = prayerLabel(next.name),
                timeStr = next.timeStr,
                countdown = formatCountdown(next.timeMillis - now),
                progress = sessionProgress(timings, next.timeMillis, now)
            )
        }

        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.todays_prayers),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))

        val statuses = remember(timings, now) {
            com.mawaqit.app.util.getCurrentPrayerStatus(timings, now)
        }
        val times = linkedMapOf(
            PrayerName.FAJR to timings.fajr,
            PrayerName.DHUHR to timings.dhuhr,
            PrayerName.ASR to timings.asr,
            PrayerName.MAGHRIB to timings.maghrib,
            PrayerName.ISHA to timings.isha
        )
        times.forEach { (prayer, timeStr) ->
            PrayerRow(
                prayer = prayer,
                timeStr = timeStr,
                status = statuses[prayer] ?: PrayerStatus.UPCOMING,
                prayed = state.salahLog[prayer] == true,
                alarmEnabled = state.alarmStates[prayer] ?: true,
                alarmSwitchEnabled = true,
                onTogglePrayed = { prayed -> viewModel.markPrayed(prayer, prayed) },
                onToggleAlarm = { enabled -> viewModel.toggleAlarm(prayer, enabled) }
            )
            HorizontalDivider()
        }

        state.dailyAyah?.let { ayah ->
            Spacer(Modifier.height(20.dp))
            AyahCard(
                arabic = ayah.arabic,
                translation = ayah.english,
                reference = ayah.reference
            )
        }

        Spacer(Modifier.height(16.dp))
        BuildStamp()
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun Header(state: HomeUiState) {
    Column {
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        val place = listOfNotNull(state.cityName, formatDateIso(state.timings?.date ?: ""))
            .filter { it.isNotBlank() }
            .joinToString(" · ")
        if (place.isNotBlank()) {
            Text(place, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun CachedBanner() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            stringResource(R.string.error_using_cached),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Gold pill CTA (DESIGN.md §6 PillButtonPrimary). */
@Composable
private fun PrimaryPill(text: String, enabled: Boolean, onClick: () -> Unit) {
    androidx.compose.material3.Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(50),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = PrimaryGold,
            contentColor = androidx.compose.ui.graphics.Color.White
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Text(text, fontWeight = FontWeight.SemiBold)
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

@Composable
private fun prayerLabel(prayer: PrayerName): String =
    com.mawaqit.app.ui.components.PrayerNameLabel(prayer)

/**
 * Ring progress 1.0 → 0.0 across the current session (previous prayer → next).
 * ponytail: pre-Fajr / month-edge cases clamp to a full ring — harmless visual,
 * real fix (tomorrow's row) is Phase 4 polish alongside GAP-5.
 */
private fun sessionProgress(timings: PrayerTimings, nextMillis: Long, now: Long): Float {
    val date = LocalDate.parse(timings.date)
    val todayTimes = listOf(
        timings.fajr, timings.dhuhr, timings.asr, timings.maghrib, timings.isha
    ).map { parseTimeToMillis(it, date) }
    val prev = todayTimes.filter { it <= now }.maxOrNull() ?: return 1f
    val session = nextMillis - prev
    if (session <= 0) return 1f
    return (1f - (nextMillis - now).toFloat() / session.toFloat()).coerceIn(0.05f, 1f)
}

/** "2026-09-15" → "15 Sep 2026" (display only — storage stays ISO). */
private fun formatDateIso(iso: String): String = try {
    LocalDate.parse(iso).format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy"))
} catch (e: Exception) {
    iso
}
