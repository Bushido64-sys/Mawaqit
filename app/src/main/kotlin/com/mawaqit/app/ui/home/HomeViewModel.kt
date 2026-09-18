package com.mawaqit.app.ui.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mawaqit.app.alarm.AlarmScheduler
import com.mawaqit.app.data.model.NextPrayer
import com.mawaqit.app.data.model.PrayerName
import com.mawaqit.app.data.model.PrayerTimings
import com.mawaqit.app.data.prefs.PrefsRepository
import com.mawaqit.app.data.repository.PrayerRepository
import com.mawaqit.app.util.LocationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * PHASE_2 temporary state holder — PHASE_4 replaces the UI with the real design.
 * PHASE_3 adds: alarm toggle states (for the TEMP test switches) and alarm
 * scheduling whenever today's times load.
 */
data class HomeUiState(
    val needsLocation: Boolean = true,   // no saved coords yet → show buttons
    val isLoading: Boolean = false,
    val timings: PrayerTimings? = null,
    val nextPrayer: NextPrayer? = null,
    val cityName: String? = null,
    val fromCache: Boolean = false,      // true → show "Cached data" banner
    val error: String? = null,
    val alarmStates: Map<PrayerName, Boolean> = emptyMap() // PHASE_3 temp test UI
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: PrayerRepository,
    private val locationHelper: LocationHelper,
    private val prefs: PrefsRepository,
    private val alarmScheduler: AlarmScheduler,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.cityName().collect { city ->
                _state.update { it.copy(cityName = city) }
            }
        }
        viewModelScope.launch {
            if (repository.hasSavedLocation()) loadTimes()
        }
        observeAlarmToggles()
    }

    /** "Use My Location" — GPS fix, save, load. Permission is checked in the UI. */
    fun useMyLocation() {
        if (!hasLocationPermission()) {
            _state.update { it.copy(error = "Location permission not granted yet.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val loc = locationHelper.getCurrentLocation()
            if (loc == null) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Couldn't get a GPS fix. Turn location on and retry, or use Test Karachi."
                    )
                }
            } else {
                repository.setLocation(loc.latitude, loc.longitude, cityName = null)
                loadTimes()
            }
        }
    }

    /** "Test with Karachi" — fixed coords so times can be verified against aladhan.com. */
    fun testKarachi() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            repository.setLocation(24.8607, 67.0011, cityName = "Karachi (test)")
            loadTimes()
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }

    // ── PHASE_3: alarms ─────────────────────────────────────────────────────

    /** Live per-prayer toggle states for the TEMP test switches. */
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

    /**
     * TEMP test-UI switch: save the pref, then re-schedule (or cancel) with
     * AlarmScheduler. scheduleAllPrayerAlarms handles both directions — it
     * cancels disabled prayers and schedules enabled future ones.
     */
    fun toggleAlarm(prayer: PrayerName, enabled: Boolean) {
        viewModelScope.launch {
            prefs.setAlarmEnabled(prayer, enabled)
            val timings = repository.getTodayPrayerTimes() ?: return@launch
            alarmScheduler.scheduleAllPrayerAlarms(timings)
        }
    }

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
     * (offline-first behavior, Rule 8). On success → schedule today's alarms.
     */
    private suspend fun loadTimes() {
        _state.update { it.copy(isLoading = true, error = null) }
        val fetched = repository.refreshIfNeeded()
        val timings = repository.getTodayPrayerTimes()
        when {
            timings == null && !fetched ->
                _state.update { it.copy(isLoading = false, needsLocation = true) }
            timings == null && fetched ->
                _state.update { it.copy(isLoading = false, error = "Fetched month but no row for today.") }
            else -> {
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
                // PHASE_3: every app open re-arms today's enabled alarms
                // (PHASE_3_ALARMS.md scheduling logic). Skips past times.
                timings?.let { alarmScheduler.scheduleAllPrayerAlarms(it) }
            }
        }
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
}
