package com.mawaqit.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mawaqit.app.alarm.AzanPlayer
import com.mawaqit.app.alarm.AzanType
import com.mawaqit.app.data.prefs.AzanOption
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
    private val azanPlayer: AzanPlayer,
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

    fun setSelectedAzan(option: AzanOption) {
        viewModelScope.launch { prefs.setSelectedAzan(option) }
    }

    private var previewJob: Job? = null

    fun previewAzan(option: AzanOption) {
        previewJob?.cancel()
        previewJob = viewModelScope.launch(Dispatchers.IO) {
            azanPlayer.playAzan(
                if (option == AzanOption.MAKKAH) AzanType.MAKKAH else AzanType.DEFAULT
            )
            delay(5000)
            azanPlayer.stop()
        }
    }

    fun stopPreview() {
        previewJob?.cancel()
        viewModelScope.launch(Dispatchers.IO) { azanPlayer.stop() }
    }
}
