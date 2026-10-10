package com.mawaqit.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mawaqit.app.alarm.AzanPreviewPlayer
import com.mawaqit.app.alarm.AzanType
import com.mawaqit.app.data.prefs.PrefsRepository
import com.mawaqit.app.data.repository.PrayerRepository
import com.mawaqit.app.util.LocationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val prefs: PrefsRepository,
    private val repository: PrayerRepository,
    private val locationHelper: LocationHelper,
    private val azanPreviewPlayer: AzanPreviewPlayer,
    private val countriesApi: com.mawaqit.app.data.api.CountriesNowApiService
) : ViewModel() {

    // ── country/city wheels (PHASE-9.1) ──────────────────────────────────────
    data class Country(val name: String, val cities: List<String>)

    private val _countries = kotlinx.coroutines.flow.MutableStateFlow<List<Country>>(emptyList())
    val countries: kotlinx.coroutines.flow.StateFlow<List<Country>> = _countries

    init {
        viewModelScope.launch {
            try {
                val res = countriesApi.getCountries()
                _countries.value = res.data.orEmpty()
                    .mapNotNull { c -> c.country?.let { Country(it, c.cities.orEmpty()) } }
                    .sortedBy { it.name }
            } catch (_: Exception) { /* offline — wheels stay empty, GPS path still works */ }
        }
    }

    fun setOnboardingComplete() {
        viewModelScope.launch { prefs.setOnboardingComplete(true) }
    }

    /** Persist the flag, THEN run the callback (locale apply + navigate) —
     *  so the Activity recreate from setApplicationLocales never races the
     *  DataStore write and re-shows onboarding (user bug, PHASE-9.2). */
    fun finish(onDone: () -> Unit) {
        viewModelScope.launch {
            prefs.setOnboardingComplete(true)
            onDone()
        }
    }

    fun useGpsLocation() {
        viewModelScope.launch {
            val loc = try { locationHelper.getCurrentLocation() } catch (_: Exception) { null }
            if (loc != null) repository.setLocation(loc.latitude, loc.longitude, cityName = null)
        }
    }

    fun setManualCity(city: String, country: String) {
        viewModelScope.launch { repository.setManualLocation(city.trim(), country.trim()) }
    }

    fun pickManualLocation(city: String, country: String) {
        viewModelScope.launch { repository.setManualLocation(city, country) }
    }

    fun setAppLanguage(language: String) {
        viewModelScope.launch { prefs.setAppLanguage(language) }
    }

    fun setSelectedAzan(option: AzanType) {
        viewModelScope.launch { prefs.setSelectedAzan(option.storage) }
    }

    // ── Azan voice preview (PHASE-10.2 / 10.3) ──────────────────────────────────
    // Observable so the UI can animate play -> pause and show real progress, instead
    // of the user guessing how long the excerpt runs. Uses AzanPreviewPlayer, not the
    // AzanPlayer singleton the azan service injects.

    private val _previewing = kotlinx.coroutines.flow.MutableStateFlow<AzanType?>(null)
    val previewing: kotlinx.coroutines.flow.StateFlow<AzanType?> = _previewing.asStateFlow()

    private val _previewProgress = kotlinx.coroutines.flow.MutableStateFlow(0f)
    val previewProgress: kotlinx.coroutines.flow.StateFlow<Float> = _previewProgress.asStateFlow()

    private var previewJob: Job? = null

    /** ~25s excerpt from each voice's own measured phrase boundary. Tapping the SAME
     *  option again stops the preview so the user can re-listen without waiting. */
    fun previewAzan(option: AzanType) {
        if (_previewing.value == option) {
            stopPreview()
            return
        }
        _previewing.value = option
        _previewProgress.value = 0f
        previewJob?.cancel()
        previewJob = viewModelScope.launch(Dispatchers.Default) {
            azanPreviewPlayer.playPreview(option)
            // Progress comes from the player's real position, so the bar cannot drift
            // away from the audio.
            while (isActive) {
                val p = azanPreviewPlayer.previewProgress() ?: break
                if (p >= 1f) break
                _previewProgress.value = p
                delay(100)
            }
            _previewProgress.value = 0f
            _previewing.value = null
            azanPreviewPlayer.stopPreview()
        }
    }

    fun stopPreview() {
        previewJob?.cancel()
        previewJob = null
        _previewProgress.value = 0f
        _previewing.value = null
        viewModelScope.launch(Dispatchers.IO) { azanPreviewPlayer.stopPreview() }
    }
}
