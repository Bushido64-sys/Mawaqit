package com.mawaqit.app.ui.home

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.mawaqit.app.ui.theme.PrimaryBlue
import com.mawaqit.app.ui.theme.PrimaryGold
import com.mawaqit.app.ui.theme.SuccessGreen
import com.mawaqit.app.util.PrayerStatus
import com.mawaqit.app.util.formatCountdown
import com.mawaqit.app.util.parseTimeToMillis
import java.time.LocalDate
import kotlinx.coroutines.delay

/**
 * PHASE_4: the real home screen (DESIGN.md Screen 3) — NextPrayerCard hero,
 * five PrayerRow checkmarks (salah_log), AyahCard, cached-data banner.
 * The Phase 2/3 temporary screen (test buttons, big switches) is replaced.
 * Kept from Phase 3: the one-time notification permission request and the
 * per-prayer alarm switches (now living on the rows where they belong).
 * PHASE-5.1: widget promo card (Alerts-2 prompt pattern) — one-tap system
 * pin dialog, auto-hides once a widget is hosted or permanently dismissed.
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

@OptIn(ExperimentalMaterial3Api::class) // ModalBottomSheet is experimental in M3
@Composable
private fun TimesContent(state: HomeUiState, viewModel: HomeViewModel) {
    val timings = state.timings ?: return
    val now = state.nowMillis

    // PHASE-5.2: promo is a bottom-sheet POPUP after ~5s (user request), not
    // a permanent inline card. Eligibility lives in state.showWidgetPromo
    // (never when a widget is hosted or previously dismissed); the 5s delay
    // + one-shot flag live here, so it appears at most once per app open.
    var showPromoSheet by remember { mutableStateOf(false) }
    var showCalendarSheet by remember { mutableStateOf(false) } // PHASE-4.5
    LaunchedEffect(Unit) {
        delay(5_000)
        if (state.showWidgetPromo) showPromoSheet = true
    }

    if (showPromoSheet && state.showWidgetPromo) {
        ModalBottomSheet(onDismissRequest = { showPromoSheet = false }) {
            WidgetPromoSheetContent(
                onAdd = viewModel::addWidget,
                onDismiss = {
                    showPromoSheet = false
                    viewModel.dismissWidgetPromo() // "Not now" = permanent
                }
            )
        }
    }

    // PHASE-4.5 — the prayer calendar bottom sheet.
    if (showCalendarSheet) {
        ModalBottomSheet(onDismissRequest = { showCalendarSheet = false }) {
            PrayerCalendarSheet(
                month = state.calendarMonth,
                selectedDate = state.calendarSelectedDate,
                daysWithPrayers = state.calendarDaysWithPrayers,
                detail = state.calendarDetail,
                onPrevMonth = { viewModel.changeCalendarMonth(-1) },
                onNextMonth = { viewModel.changeCalendarMonth(1) },
                onSelectDate = viewModel::selectCalendarDate
            )
        }
    }

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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.todays_prayers),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            CalendarChip(onClick = {
                viewModel.openCalendar()
                showCalendarSheet = true
            })
        }
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

/**
 * PHASE-5.2: widget promo as a bottom-sheet popup (DESIGN.md Alerts-2
 * pattern: icon-in-circle, bold headline, muted body, stacked gold pill +
 * quiet secondary). Sheet chrome is Material's own.
 */
@Composable
private fun WidgetPromoSheetContent(onAdd: () -> Unit, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(PrimaryGold.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("🕌", style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.widget_promo_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.widget_promo_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        PrimaryPill(stringResource(R.string.widget_promo_add), enabled = true, onClick = onAdd)
        Spacer(Modifier.height(4.dp))
        androidx.compose.material3.TextButton(onClick = onDismiss) {
            Text(
                stringResource(R.string.widget_promo_dismiss),
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

/**
 * PHASE-4.5 — opens the prayer calendar. Gold-bordered circular chip, blue
 * icon — the same gold/blue language as the reader's mark pill.
 */
@Composable
private fun CalendarChip(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, PrimaryGold, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Event,
            contentDescription = stringResource(R.string.calendar_open),
            tint = PrimaryBlue,
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * PHASE-4.5 — the prayer calendar sheet: month grid (Mon-first, gold dot on
 * every day with ≥1 prayed prayer, gold ring on today), ‹ › month paging,
 * and a tapped-day detail listing all five prayers with gold ✓ = prayed.
 * All names/dates render locale-aware; no DB writes — history is read-only.
 */
@Composable
private fun PrayerCalendarSheet(
    month: java.time.YearMonth,
    selectedDate: String?,
    daysWithPrayers: Set<String>,
    detail: Map<String, Boolean>,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (String) -> Unit
) {
    val today = java.time.LocalDate.now()
    val weekDays = remember {
        java.time.DayOfWeek.values().map {
            it.getDisplayName(java.time.format.TextStyle.NARROW, java.util.Locale.getDefault())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
    ) {
        // ── Month header ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CalendarIconButton(Icons.Filled.KeyboardArrowLeft, onPrevMonth)
            Text(
                text = month.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy")),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            CalendarIconButton(Icons.Filled.KeyboardArrowRight, onNextMonth)
        }
        Spacer(Modifier.height(12.dp))

        // ── Weekday initials (Mon-first, locale-aware) ──
        Row(modifier = Modifier.fillMaxWidth()) {
            weekDays.forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(4.dp))

        // ── Day grid (leading blanks align day 1 to its weekday) ──
        val firstOffset = month.atDay(1).dayOfWeek.value - 1 // Mon=1 → 0 blanks
        val daysInMonth = month.lengthOfMonth()
        val rowCount = (firstOffset + daysInMonth + 6) / 7
        repeat(rowCount) { rowIndex ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { col ->
                    val cell = rowIndex * 7 + col
                    if (cell < firstOffset) {
                        Spacer(Modifier.weight(1f))
                    } else {
                        val date = month.atDay(cell - firstOffset + 1)
                        val iso = date.toString()
                        DayCell(
                            day = date.dayOfMonth,
                            isToday = date == today,
                            isSelected = iso == selectedDate,
                            hasPrayers = iso in daysWithPrayers,
                            onClick = { onSelectDate(iso) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
        }

        // ── Tapped-day detail ──
        selectedDate?.let { iso ->
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            val date = try {
                java.time.LocalDate.parse(iso)
            } catch (_: Exception) {
                null
            }
            Text(
                text = date?.format(
                    java.time.format.DateTimeFormatter.ofPattern("EEEE, d MMM yyyy")
                ) ?: iso,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            PrayerName.entries.forEach { prayer ->
                val prayed = detail[prayer.name] == true
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = stringResource(
                            if (prayed) R.string.content_desc_marked_prayed
                            else R.string.content_desc_not_prayed
                        ),
                        tint = if (prayed) SuccessGreen
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = com.mawaqit.app.ui.components.PrayerNameLabel(prayer),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (prayed) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

/** ‹ / › month pager button for the calendar header. */
@Composable
private fun CalendarIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PrimaryBlue,
            modifier = Modifier.size(22.dp)
        )
    }
}

/** One day in the calendar grid: gold ring = today, shade = selected, dot = prayed. */
@Composable
private fun DayCell(
    day: Int,
    isToday: Boolean,
    isSelected: Boolean,
    hasPrayers: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(1.dp)
            .height(36.dp)
            .clip(CircleShape)
            .background(
                when {
                    isSelected -> PrimaryGold.copy(alpha = 0.18f)
                    else -> Color.Transparent
                }
            )
            .border(
                width = if (isToday) 1.dp else 0.dp,
                color = PrimaryGold,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) PrimaryBlue else Color.Unspecified
            )
            if (hasPrayers) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(PrimaryGold)
                )
            }
        }
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
