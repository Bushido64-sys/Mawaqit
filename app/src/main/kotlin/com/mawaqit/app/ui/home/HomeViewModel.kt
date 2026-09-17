package com.mawaqit.app.ui.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mawaqit.app.data.model.NextPrayer
import com.mawaqit.app.data.model.PrayerTimings
import com.mawaqit.app.data.repository.PrayerRepository
import com.mawaqit.app.util.LocationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * PHASE_2 temporary state holder — PHASE_4 replaces the UI with the real design
 * (and a proper prayer-list UI with salah marking), so this stays minimal.
 */
data class HomeUiState(
    val needsLocation: Boolean = true,   // no saved coords yet → show buttons
    val isLoading: Boolean = false,
    val timings: PrayerTimings? = null,
    val nextPrayer: NextPrayer? = null,
    val cityName: String? = null,
    val fromCache: Boolean = false,      // true → show "Cached data" banner
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: PrayerRepository,
    private val locationHelper: LocationHelper,
    @ApplicationContext private val appContext: android.content.Context
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

    // ── internals ───────────────────────────────────────────────────────────

    /**
     * Loads today's times: refresh month from API if needed, then read the DB.
     * fromCache=true when the API fetch failed but the DB still has data
     * (offline-first behavior, Rule 8).
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
            }
        }
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
}
