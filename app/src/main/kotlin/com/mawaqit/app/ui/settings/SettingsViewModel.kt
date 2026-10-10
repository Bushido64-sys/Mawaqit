package com.mawaqit.app.ui.settings

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mawaqit.app.alarm.AlarmRefreshManager
import com.mawaqit.app.alarm.AzanPreviewPlayer
import com.mawaqit.app.alarm.AzanType
import com.mawaqit.app.data.model.PrayerName
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class SettingsUiState(
    val alarmStates: Map<PrayerName, Boolean> = emptyMap(),
    val selectedAzan: AzanType = AzanType.DEFAULT,
    val azanVolume: Float = 1f,
    val azanForceAlarm: Boolean = false,
    val appLanguage: String = "en",
    val cityName: String? = null,
    val readerFontScale: Float = 1.0f,
    val notificationsAllowed: Boolean = true,
    val exactAlarmAllowed: Boolean = true,
    val batteryOptimized: Boolean = true, // true = still optimized (bad)
    val appVersion: String = "1.0.0",
    val locationUpdating: Boolean = false,
    val locationError: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: PrefsRepository,
    private val refreshManager: AlarmRefreshManager,
    private val repository: PrayerRepository,
    private val locationHelper: LocationHelper,
    private val azanPreviewPlayer: AzanPreviewPlayer,
    private val countriesApi: com.mawaqit.app.data.api.CountriesNowApiService,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        observe()
        refreshHealth()
    }

    private fun observe() {
        viewModelScope.launch {
            combine(
                prefs.selectedAzan,
                prefs.azanVolume,
                prefs.azanForceAlarm,
                prefs.appLanguage,
                prefs.savedCityName
            ) { azan, volume, force, lang, city ->
                SettingsUiState(
                    selectedAzan = AzanType.selectedFromStorage(azan),
                    azanVolume = volume,
                    azanForceAlarm = force,
                    appLanguage = lang,
                    cityName = city
                )
            }.collect { base ->
                _state.value = base.copy(
                    alarmStates = _state.value.alarmStates,
                    readerFontScale = _state.value.readerFontScale,
                    notificationsAllowed = _state.value.notificationsAllowed,
                    exactAlarmAllowed = _state.value.exactAlarmAllowed,
                    batteryOptimized = _state.value.batteryOptimized,
                    appVersion = _state.value.appVersion,
                    locationUpdating = _state.value.locationUpdating,
                    locationError = _state.value.locationError
                )
            }
        }
        viewModelScope.launch {
            val flows = PrayerName.entries.map { prefs.alarmEnabledFlow(it) }
            combine(flows) { values ->
                val map = LinkedHashMap<PrayerName, Boolean>()
                PrayerName.entries.forEachIndexed { i, p -> map[p] = values[i] }
                map
            }.collect { map -> _state.value = _state.value.copy(alarmStates = map) }
        }
        viewModelScope.launch {
            prefs.readerFontScale.collect { scale ->
                _state.value = _state.value.copy(readerFontScale = scale)
            }
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(appVersion = "1.0.0-${com.mawaqit.app.BuildConfig.GIT_SHA}")
        }
    }

    /** Re-check system permissions/optimization — call on every screen resume. */
    fun refreshHealth() {
        val notif = ContextCompat.checkSelfPermission(
            appContext, android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || run {
            val am = appContext.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            am.canScheduleExactAlarms()
        }
        val pm = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        val battery = !pm.isIgnoringBatteryOptimizations(appContext.packageName)
        _state.value = _state.value.copy(
            notificationsAllowed = notif,
            exactAlarmAllowed = exact,
            batteryOptimized = battery
        )
    }

    fun toggleAlarm(prayer: PrayerName, enabled: Boolean) {
        viewModelScope.launch {
            prefs.setAlarmEnabled(prayer, enabled)
            refreshManager.refreshAlarmsFromCache()
        }
    }

    fun setSelectedAzan(option: AzanType) {
        viewModelScope.launch { prefs.setSelectedAzan(option.storage) }
    }

    fun setAzanVolume(volume: Float) {
        viewModelScope.launch { prefs.setAzanVolume(volume) }
    }

    fun setAzanForceAlarm(force: Boolean) {
        viewModelScope.launch { prefs.setAzanForceAlarm(force) }
    }

    fun setAppLanguage(language: String) {
        viewModelScope.launch { prefs.setAppLanguage(language) }
    }

    fun setReaderFontScale(scale: Float) {
        viewModelScope.launch { prefs.setReaderFontScale(scale) }
    }

    // ── Location (PHASE-8.1) ───────────────────────────────────────────────

    private val _countries = MutableStateFlow<List<Country>>(emptyList())
    val countries: StateFlow<List<Country>> = _countries

    data class Country(val name: String, val cities: List<String>)

    private val _placesLoading = MutableStateFlow(false)
    val placesLoading: StateFlow<Boolean> = _placesLoading

    private val _placesError = MutableStateFlow<String?>(null)
    val placesError: StateFlow<String?> = _placesError

    private val _cities = MutableStateFlow<List<String>>(emptyList())
    val cities: StateFlow<List<String>> = _cities

    fun loadCountries() {
        viewModelScope.launch {
            _placesLoading.value = true
            _placesError.value = null
            try {
                val res = countriesApi.getCountries()
                _countries.value = res.data.orEmpty()
                    .mapNotNull { c -> c.country?.let { Country(it, c.cities.orEmpty()) } }
                    .sortedBy { it.name }
            } catch (_: Exception) {
                _placesError.value = "Could not load countries"
            }
            _placesLoading.value = false
        }
    }

    fun loadCities(country: String) {
        _cities.value = _countries.value.firstOrNull { it.name == country }?.cities.orEmpty()
        _placesError.value = null
    }

    fun pickManualCity(city: String, country: String) {
        viewModelScope.launch {
            repository.setManualLocation(city, country)
            _state.value = _state.value.copy(cityName = "$city, $country", locationError = null)
        }
    }

    // ── Calculation method (PHASE-8.1) ─────────────────────────────────────

    private val _calculationMethod = MutableStateFlow(1)
    val calculationMethod: StateFlow<Int> = _calculationMethod

    init {
        viewModelScope.launch {
            prefs.calculationMethod.collect { _calculationMethod.value = it }
        }
    }

    fun setCalculationMethod(method: Int) {
        viewModelScope.launch { repository.setCalculationMethod(method) }
    }

    // ── Old GPS-only changeLocation kept for auto-detect reuse ─────────────

    fun changeLocation() {
        viewModelScope.launch {
            _state.value = _state.value.copy(locationUpdating = true, locationError = null)
            val loc = try { locationHelper.getCurrentLocation() } catch (_: Exception) { null }
            if (loc == null) {
                _state.value = _state.value.copy(
                    locationUpdating = false,
                    locationError = appContext.getString(com.mawaqit.app.R.string.error_no_location)
                )
            } else {
                repository.setLocation(loc.latitude, loc.longitude, cityName = null)
                _state.value = _state.value.copy(locationUpdating = false)
            }
        }
    }

    fun clearLocationError() {
        _state.value = _state.value.copy(locationError = null)
    }

    // ── Azan voice preview (PHASE-10.2 / 10.3) ──────────────────────────────────
    // Voice auditions use AzanPreviewPlayer, never AzanPlayer: AzanPlayer is the
    // singleton AzanService injects, so a preview sharing it could silence a real
    // prayer azan. This is separately observable so the UI can animate the button and
    // show real progress instead of the user guessing how long is left.

    /** Which voice is currently previewing (null = nothing playing). */
    private val _previewing = MutableStateFlow<AzanType?>(null)
    val previewing: StateFlow<AzanType?> = _previewing.asStateFlow()

    /** 0f..1f through the excerpt, read from the player itself so it cannot drift. */
    private val _previewProgress = MutableStateFlow(0f)
    val previewProgress: StateFlow<Float> = _previewProgress.asStateFlow()

    private var previewJob: kotlinx.coroutines.Job? = null

    /** Tapping the SAME voice's play button again stops the preview early, so the user
     *  can re-listen without waiting out the rest of the excerpt. */
    fun previewAzan(option: AzanType) {
        if (_previewing.value == option) {
            stopAzanPreview()
            return
        }
        _previewing.value = option
        _previewProgress.value = 0f
        previewJob?.cancel()
        previewJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            azanPreviewPlayer.playPreview(option)
            // Drive the progress bar from the player's real position rather than a
            // guessed clock, then stop at the end.
            while (isActive) {
                val p = azanPreviewPlayer.previewProgress()
                if (p == null) break                 // audio finished or was stopped
                if (p >= 1f) break
                _previewProgress.value = p
                kotlinx.coroutines.delay(100)
            }
            _previewProgress.value = 0f
            _previewing.value = null
            azanPreviewPlayer.stopPreview()
        }
    }

    fun stopAzanPreview() {
        previewJob?.cancel()
        previewJob = null
        _previewProgress.value = 0f
        _previewing.value = null
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) { azanPreviewPlayer.stopPreview() }
    }
}
