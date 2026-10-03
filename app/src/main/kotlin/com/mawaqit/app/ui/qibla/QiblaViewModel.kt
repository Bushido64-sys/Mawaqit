package com.mawaqit.app.ui.qibla

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.location.Geocoder
import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.mawaqit.app.util.CompassSensorManager
import com.mawaqit.app.util.LocationHelper
import com.mawaqit.app.util.QiblaCalculator

data class QiblaUiState(
    val deviceHeading: Float = 0f,
    val qiblaBearing: Float = 0f,
    val needleRotation: Float = 0f,
    val locationName: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val isLoading: Boolean = true,
    val locationFailed: Boolean = false
)

@HiltViewModel
class QiblaViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationHelper: LocationHelper,
    private val compass: CompassSensorManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(QiblaUiState())
    val uiState: StateFlow<QiblaUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val location = locationHelper.getCurrentLocation()
            if (location == null) {
                _uiState.value = _uiState.value.copy(isLoading = false, locationFailed = true)
            } else {
                val bearing = QiblaCalculator.calculateQiblaBearing(location.latitude, location.longitude)
                val name = try {
                    Geocoder(context).getFromLocation(location.latitude, location.longitude, 1)
                        ?.firstOrNull()?.locality ?: ""
                } catch (e: Exception) { "" }
                _uiState.value = _uiState.value.copy(
                    qiblaBearing = bearing,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    locationName = name,
                    isLoading = false
                )
            }
        }
        viewModelScope.launch {
            compass.getHeadingFlow().collect { heading ->
                val bearing = _uiState.value.qiblaBearing
                _uiState.value = _uiState.value.copy(
                    deviceHeading = heading,
                    needleRotation = ((bearing - heading) + 360f) % 360f
                )
            }
        }
    }
}
