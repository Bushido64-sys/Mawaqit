package com.mawaqit.app.ui.settings

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mawaqit.app.alarm.AlarmRefreshManager
import com.mawaqit.app.data.model.PrayerName
import com.mawaqit.app.data.prefs.AzanOption
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
import kotlinx.coroutines.launch

data class SettingsUiState(
    val alarmStates: Map<PrayerName, Boolean> = emptyMap(),
    val selectedAzan: AzanOption = AzanOption.DEFAULT,
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
    private val azanPlayer: com.mawaqit.app.alarm.AzanPlayer,
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
                    selectedAzan = azan,
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

    fun setSelectedAzan(option: AzanOption) {
        viewModelScope.launch { prefs.setSelectedAzan(option) }
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

    /** ~5s preview on Dispatchers.IO, then stop (ASSETS.md / PHASE_8 spec).
     *  A new preview cancels the previous one's trailing stop() so it can't
     *  kill freshly started playback. */
    private var previewJob: kotlinx.coroutines.Job? = null

    fun previewAzan(option: AzanOption) {
        previewJob?.cancel()
        previewJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            azanPlayer.playAzan(
                if (option == AzanOption.MAKKAH) com.mawaqit.app.alarm.AzanType.MAKKAH
                else com.mawaqit.app.alarm.AzanType.DEFAULT
            )
            kotlinx.coroutines.delay(5000)
            azanPlayer.stop()
        }
    }

    fun stopAzanPreview() {
        previewJob?.cancel()
        previewJob = null
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) { azanPlayer.stop() }
    }
}
