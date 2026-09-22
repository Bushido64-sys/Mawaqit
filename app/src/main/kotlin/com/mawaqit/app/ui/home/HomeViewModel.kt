package com.mawaqit.app.ui.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mawaqit.app.alarm.AlarmRefreshManager
import com.mawaqit.app.data.model.DailyAyah
import com.mawaqit.app.data.model.NextPrayer
import com.mawaqit.app.data.model.PrayerName
import com.mawaqit.app.data.model.PrayerTimings
import com.mawaqit.app.data.prefs.PrefsRepository
import com.mawaqit.app.data.repository.AyahRepository
import com.mawaqit.app.data.repository.SalahRepository
import com.mawaqit.app.data.repository.PrayerRepository
import com.mawaqit.app.util.LocationHelper
import com.mawaqit.app.widget.MawaqitWidget
import com.mawaqit.app.widget.WidgetPromo
import com.mawaqit.app.widget.WidgetUpdateWorker
import androidx.glance.appwidget.GlanceAppWidgetManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * PHASE_4: the real home screen state (PHASE_2/3 temp state holder replaced).
 * Owns: prayer times + next prayer, live countdown tick, salah checkmarks
 * (salah_log via SalahRepository), alarm toggles, daily ayah, location setup.
 * The PHASE-3.2 diagnostics (re-arm/test buttons, diagMessage) are gone —
 * they were temporary.
 */
data class HomeUiState(
    // null = still checking whether a location is saved (avoids flashing the
    // setup screen for a frame before the async prefs read lands — user report).
    // true = no saved location → show setup; false = show the home screen.
    val needsLocation: Boolean? = null,
    val isLoading: Boolean = true,
    val timings: PrayerTimings? = null,
    val nextPrayer: NextPrayer? = null,
    val cityName: String? = null,
    val fromCache: Boolean = false,      // true → "cached data" banner
    val error: String? = null,
    val salahLog: Map<PrayerName, Boolean> = emptyMap(),   // prayed checkmarks
    val alarmStates: Map<PrayerName, Boolean> = emptyMap(),// per-prayer alarm switches
    val dailyAyah: DailyAyah? = null,
    val widgetAdded: Boolean = false,     // ≥1 Mawaqit widget hosted (PHASE-5.1)
    val showWidgetPromo: Boolean = false, // promo card visible (never once dismissed/added)
    val nowMillis: Long = System.currentTimeMillis(),      // 1s ticker for countdown
    // PHASE-4.5 prayer calendar — month = "2026-09", selected = ISO day.
    val calendarMonth: java.time.YearMonth = java.time.YearMonth.now(),
    val calendarSelectedDate: String? = null,
    val calendarDaysWithPrayers: Set<String> = emptySet(), // ISO days with ≥1 prayed
    val calendarDetail: Map<String, Boolean> = emptyMap()  // prayer → prayed for selected date
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: PrayerRepository,
    private val salahRepository: SalahRepository,
    private val ayahRepository: AyahRepository,
    private val locationHelper: LocationHelper,
    private val prefs: PrefsRepository,
    private val refreshManager: AlarmRefreshManager,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private var loadedDate: String? = null   // ISO day the current times belong to

    init {
        viewModelScope.launch {
            repository.cityName().collect { city ->
                _state.update { it.copy(cityName = city) }
            }
        }
        viewModelScope.launch {
            if (repository.hasSavedLocation()) {
                loadTimes()
            } else {
                _state.update { it.copy(isLoading = false, needsLocation = true) } // show setup buttons
            }
        }
        observeAlarmToggles()
        observeSalahLog()
        observeWidgetPromo()
        loadDailyAyah()
        startTicker()
        // PHASE-4.5 — the 30-day auto-cleanup is retired: the prayer calendar
        // keeps salah_log history forever (user decision).
    }

    // ── countdown ticker + midnight rollover ────────────────────────────────

    private fun startTicker() {
        viewModelScope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                // Real-time rollover (user-reported bug: hero froze at 00:00:00
                // after the prayer passed, until the app was reopened): the moment
                // the shown next-prayer time passes, recompute it from today's row.
                val timings = _state.value.timings
                val shownNext = _state.value.nextPrayer
                if (timings != null && shownNext != null && shownNext.timeMillis <= now) {
                    val next = repository.getNextPrayer(timings)
                    _state.update { it.copy(nextPrayer = next) }
                }
                _state.update { it.copy(nowMillis = now) }
                val today = java.time.LocalDate.now().toString()
                if (today != loadedDate && loadedDate != null) {
                    loadTimes() // day changed → fetch today's row, re-arm alarms
                    observeSalahLogForToday()
                }
                delay(1000)
            }
        }
    }

    // ── location setup ──────────────────────────────────────────────────────

    /** "Use My Location" — GPS fix, save, load. Permission is checked in the UI. */
    fun useMyLocation() {
        if (!hasLocationPermission()) {
            _state.update { it.copy(error = appContext.getString(com.mawaqit.app.R.string.error_no_location)) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val loc = locationHelper.getCurrentLocation()
            if (loc == null) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = appContext.getString(com.mawaqit.app.R.string.error_no_location)
                    )
                }
            } else {
                repository.setLocation(loc.latitude, loc.longitude, cityName = null)
                loadTimes()
            }
        }
    }

    /** Fixed Karachi coords so times can be verified against aladhan.com. */
    fun testKarachi() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            repository.setLocation(24.8607, 67.0011, cityName = "Karachi")
            loadTimes()
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }

    // ── salah checkmarks (the feature Phase 3's notification wrote into) ────

    fun markPrayed(prayer: PrayerName, prayed: Boolean) {
        viewModelScope.launch {
            salahRepository.markPrayed(prayer, prayed)
        }
    }

    private var salahLogJob: kotlinx.coroutines.Job? = null

    private fun observeSalahLog() {
        observeSalahLogForToday()
    }

    private fun observeSalahLogForToday() {
        salahLogJob?.cancel()
        val today = java.time.LocalDate.now().toString()
        salahLogJob = viewModelScope.launch {
            salahRepository.getSalahLogForDate(today).collect { rows ->
                val map = LinkedHashMap<PrayerName, Boolean>()
                rows.forEach { row ->
                    val name = try {
                        PrayerName.valueOf(row.prayer)
                    } catch (_: IllegalArgumentException) {
                        null
                    }
                    if (name != null && row.prayed) map[name] = true
                }
                _state.update { it.copy(salahLog = map) }
            }
        }
    }

    // ── alarm toggles (same behavior as the Phase 3 test switches) ──────────

    private fun observeAlarmToggles() {
        viewModelScope.launch {
            val flows = PrayerName.entries.map { prefs.alarmEnabledFlow(it) }
            combine(flows) { values ->
                val map = LinkedHashMap<PrayerName, Boolean>()
                PrayerName.entries.forEachIndexed { index, prayer ->
                    map[prayer] = values[index]
                }
                map
            }.collect { map ->
                _state.update { it.copy(alarmStates = map) }
            }
        }
    }

    /** Save the pref, then rebuild the whole 7-day alarm plan (arms + cancels). */
    fun toggleAlarm(prayer: PrayerName, enabled: Boolean) {
        viewModelScope.launch {
            prefs.setAlarmEnabled(prayer, enabled)
            refreshManager.refreshAlarmsFromCache()
        }
    }

    // ── daily ayah ──────────────────────────────────────────────────────────

    private fun loadDailyAyah() {
        viewModelScope.launch {
            val ayah = ayahRepository.getDailyAyah()
            _state.update { it.copy(dailyAyah = ayah) }
        }
    }

    // ── widget promo card (PHASE-5.1) ─────────────────────────────────────

    /** True when at least one Mawaqit widget is hosted on the home screen. */
    private suspend fun isWidgetAdded(): Boolean = try {
        GlanceAppWidgetManager(appContext)
            .getGlanceIds(MawaqitWidget::class.java)
            .isNotEmpty()
    } catch (e: Exception) {
        false // launcher quirk → treat as not added; showing the promo is harmless
    }

    /**
     * Re-evaluates on every prefs change (cheap launcher query) AND right
     * after location setup lands a prefs write — the two moments "promo
     * should appear/hide" can change. Mid-session widget removal/addition is
     * picked up on the next app open (fine for a promo card).
     */
    private fun observeWidgetPromo() {
        viewModelScope.launch {
            prefs.widgetPromoDismissed.collect { dismissed ->
                val added = isWidgetAdded()
                _state.update {
                    it.copy(widgetAdded = added, showWidgetPromo = !dismissed && !added)
                }
            }
        }
    }

    /**
     * System "Add widget?" dialog (requestPinAppWidget). The dialog is async,
     * so the outcome is verified with a short delayed re-check of hosted
     * widgets: added → promo hidden + marked dismissed; denied/slow user →
     * promo simply stays (no permanent punishment for "No").
     */
    fun addWidget() {
        viewModelScope.launch {
            WidgetPromo.pinWidget(appContext)
            delay(3_000) // usual time to answer the system dialog
            val added = isWidgetAdded()
            if (added) {
                prefs.setWidgetPromoDismissed(true)
                _state.update { it.copy(widgetAdded = true, showWidgetPromo = false) }
            }
        }
    }

    /** "Not now" — permanent for this install (reinstalls restore prefs anyway). */
    fun dismissWidgetPromo() {
        viewModelScope.launch {
            prefs.setWidgetPromoDismissed(true)
            _state.update { it.copy(showWidgetPromo = false) }
        }
    }

    // ── PHASE-4.5: prayer calendar ───────────────────────────────────────────

    /** Calendar chip tapped → open the sheet on today's month + select today. */
    fun openCalendar() {
        val today = java.time.LocalDate.now()
        _state.update {
            it.copy(
                calendarMonth = java.time.YearMonth.from(today),
                calendarSelectedDate = today.toString()
            )
        }
        refreshCalendarData()
    }

    /** User paged months with ‹ › — reload that month's prayed-dots. */
    fun changeCalendarMonth(delta: Int) {
        _state.update { it.copy(calendarMonth = it.calendarMonth.plusMonths(delta.toLong())) }
        refreshCalendarData()
    }

    /** User tapped a day — reload its prayed/not-prayed detail. */
    fun selectCalendarDate(iso: String) {
        _state.update { it.copy(calendarSelectedDate = iso) }
        viewModelScope.launch { refreshCalendarDetail() }
    }

    /** One read-only query per month: which ISO days have ≥1 prayed prayer. */
    private fun refreshCalendarData() {
        viewModelScope.launch {
            val month = _state.value.calendarMonth
            val rows = salahRepository.getSalahLogBetween(
                month.atDay(1).toString(),
                month.atEndOfMonth().toString()
            )
            val days = rows.filter { it.prayed }
                .map { it.date }
                .toSet()
            _state.update { it.copy(calendarDaysWithPrayers = days) }
            refreshCalendarDetail()
        }
    }

    /** Detail panel under the grid: prayer → prayed for the selected date. */
    private suspend fun refreshCalendarDetail() {
        val iso = _state.value.calendarSelectedDate ?: return
        val rows = salahRepository.getSalahLogBetween(iso, iso)
        val detail = linkedMapOf<String, Boolean>()
        PrayerName.entries.forEach { prayer ->
            val row = rows.find { it.prayer == prayer.name }
            detail[prayer.name] = row?.prayed == true
        }
        _state.update { it.copy(calendarDetail = detail) }
    }

    // ── notification permission (kept from Phase 3 — still needed) ──────────

    /**
     * True when the azan notification banner can't show (Android 13+ and the
     * user hasn't granted POST_NOTIFICATIONS). Checked before launching the
     * one-time runtime request (PERMISSIONS.md runtime section).
     */
    fun needsNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED

    // ── internals ───────────────────────────────────────────────────────────

    /**
     * Loads today's times: refresh month from API if needed, then read the DB.
     * fromCache=true when the API fetch failed but the DB still has data
     * (offline-first behavior, Rule 8). On success → re-arm the alarm plan.
     */
    private suspend fun loadTimes() {
        _state.update { it.copy(isLoading = true, error = null) }
        val fetched = repository.refreshIfNeeded()
        val timings = repository.getTodayPrayerTimes()
        when {
            timings == null && !fetched ->
                _state.update { it.copy(isLoading = false, needsLocation = true) }
            timings == null && fetched ->
                _state.update { it.copy(isLoading = false) }
            else -> {
                loadedDate = timings?.date
                val next = timings?.let { repository.getNextPrayer(it) }
                _state.update {
                    it.copy(
                        isLoading = false,
                        needsLocation = false,
                        timings = timings,
                        nextPrayer = next,
                        fromCache = !fetched
                    )
                }
                // Every app open re-arms the 7-day plan from the offline cache.
                refreshManager.refreshAlarmsFromCache()
                // PHASE_5: opening the app refreshes the widget immediately.
                WidgetUpdateWorker.refreshNow(appContext)
            }
        }
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
}
