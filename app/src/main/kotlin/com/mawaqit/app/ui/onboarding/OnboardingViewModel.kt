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

    private var previewJob: Job? = null

    /** Which voice is currently previewing, so tapping it again stops early. */
    @Volatile
    private var previewingOption: AzanType? = null

    /** ~18s excerpt from 8s in (AzanPreviewPlayer), NOT the shared AzanPlayer:
     *  AzanPlayer is the singleton the azan SERVICE injects, so a preview that reused
     *  it could silence a real prayer azan (PHASE-10.2). Tapping the SAME option again
     *  stops the preview so the user can re-listen without waiting. */
    fun previewAzan(option: AzanType) {
        if (previewingOption == option) {
            stopPreview()
            return
        }
        previewingOption = option
        previewJob?.cancel()
        previewJob = viewModelScope.launch(Dispatchers.IO) {
            azanPreviewPlayer.playPreview(option)
            delay(AzanPreviewPlayer.PREVIEW_LENGTH_MS)
            azanPreviewPlayer.stopPreview()
            previewingOption = null
        }
    }

    fun stopPreview() {
        previewJob?.cancel()
        previewingOption = null
        viewModelScope.launch(Dispatchers.IO) { azanPreviewPlayer.stopPreview() }
    }
}
