package com.mawaqit.app.ui.qibla

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import android.app.Activity
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.mawaqit.app.R
import com.mawaqit.app.ui.theme.BgOffwhite
import com.mawaqit.app.ui.theme.PrimaryGold
import com.mawaqit.app.ui.theme.SurfaceDeep
import com.mawaqit.app.ui.theme.SurfaceWhite
import com.mawaqit.app.ui.theme.PrimaryBlue
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import com.mawaqit.app.ui.theme.TextMuted
import com.mawaqit.app.ui.theme.TextPrimary
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun QiblaScreen(viewModel: QiblaViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var locationOn by remember {
        mutableStateOf(
    run {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            try {
                lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            } catch (e: Exception) {
                true
            }
        }
        )
    }
    var compassDismissed by remember { mutableStateOf(false) }
    val hasMagnetometer = remember {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as android.hardware.SensorManager
        sm.getDefaultSensor(android.hardware.Sensor.TYPE_MAGNETIC_FIELD) != null ||
            sm.getDefaultSensor(android.hardware.Sensor.TYPE_ROTATION_VECTOR) != null ||
            sm.getDefaultSensor(android.hardware.Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR) != null ||
            sm.getDefaultSensor(android.hardware.Sensor.TYPE_ORIENTATION) != null
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) viewModel.refreshLocation()
    }
    LaunchedEffect(Unit) {
        if (hasPermission) viewModel.refreshLocation()
    }

    fun enableLocationServices() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10_000).build()
        val settingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(request)
            .setAlwaysShow(true)
            .build()
        LocationServices.getSettingsClient(context).checkLocationSettings(settingsRequest)
            .addOnCompleteListener { task ->
                try {
                    task.getResult(Exception::class.java)
                    locationOn = true
                    viewModel.refreshLocation()
                } catch (e: ResolvableApiException) {
                    runCatching { e.startResolutionForResult((context as Activity), 1001) }
                    scope.launch {
                        repeat(15) {
                            kotlinx.coroutines.delay(2_000)
                            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                            val nowOn = try {
                                lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                                    lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                            } catch (e2: Exception) { true }
                            if (nowOn) {
                                locationOn = true
                                viewModel.refreshLocation()
                                return@launch
                            }
                        }
                    }
                } catch (_: Exception) {
                    // user must enable manually — open system settings
                    runCatching {
                        context.startActivity(
                            android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        )
                    }
                }
            }
    }
    val animatedRotation by animateFloatAsState(
        targetValue = state.needleRotation,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "needle"
    )

    if (!hasMagnetometer && !compassDismissed) {
        AlertDialog(
            onDismissRequest = { compassDismissed = true },
            modifier = Modifier.border(1.5.dp, PrimaryBlue, RoundedCornerShape(24.dp)),
            containerColor = SurfaceWhite,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = stringResource(R.string.qibla_no_mag_title),
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.qibla_no_mag_body),
                    color = TextMuted
                )
            },
            confirmButton = {
                Button(
                    onClick = { compassDismissed = true },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryGold,
                        contentColor = Color.White
                    )
                ) {
                    Text(stringResource(R.string.qibla_no_mag_ok), fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    val pullState = rememberPullRefreshState(
        refreshing = state.isLoading,
        onRefresh = { viewModel.refreshLocation() }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgOffwhite)
            .pullRefresh(pullState)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.tab_qibla),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (state.isLoading) stringResource(R.string.qibla_finding_location)
            else state.locationName.ifBlank { " " },
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )
        Spacer(Modifier.height(24.dp))

        if (!hasPermission) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.qibla_need_permission), color = TextMuted)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = {
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }) {
                        Text(stringResource(R.string.qibla_grant_button))
                    }
                }
            }
            return@Column
        }
        if (!locationOn) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.qibla_location_off), color = TextMuted)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { enableLocationServices() }) {
                        Text(stringResource(R.string.qibla_enable_button))
                    }
                }
            }
            return@Column
        }

        Card(
            shape = androidx.compose.foundation.shape.CircleShape,
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.size(300.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                val cardSize = 300.dp
                Box(modifier = Modifier.fillMaxSize()) {
                    Text("N", Modifier.align(Alignment.TopCenter).padding(top = 10.dp), color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text("S", Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp), color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text("E", Modifier.align(Alignment.CenterEnd).padding(end = 10.dp), color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text("W", Modifier.align(Alignment.CenterStart).padding(start = 10.dp), color = TextPrimary, fontWeight = FontWeight.Bold)
                }
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val c = center
                    val r = size.minDimension / 2f - 24f

                    // tick ring
                    for (i in 0 until 24) {
                        val ang = Math.toRadians((i * 15).toDouble())
                        val start = Offset(c.x + (r - 8f) * sin(ang).toFloat(), c.y - (r - 8f) * cos(ang).toFloat())
                        val end = Offset(c.x + r * sin(ang).toFloat(), c.y - r * cos(ang).toFloat())
                        drawLine(TextMuted.copy(alpha = 0.3f), start, end, strokeWidth = 2f, cap = StrokeCap.Round)
                    }


                    // needle
                    rotate(degrees = animatedRotation, pivot = c) {
                        val tip = Offset(c.x, c.y - r + 40f)
                        val tail = Offset(c.x, c.y + r - 40f)
                        drawLine(TextMuted.copy(alpha = 0.5f), c, tail, strokeWidth = 10f, cap = StrokeCap.Round)
                        drawLine(PrimaryGold, c, tip, strokeWidth = 12f, cap = StrokeCap.Round)
                        drawCircle(PrimaryGold, radius = 10f, center = tip)
                    }

                    // center dot
                    drawCircle(SurfaceDeep, radius = 10f, center = c)
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            text = if (state.locationFailed) "—" else "${state.qiblaBearing.toInt()}${stringResource(R.string.towards_mecca)}",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        val aligned = hasMagnetometer && kotlin.math.min(
            kotlin.math.abs(state.deviceHeading - state.qiblaBearing),
            360f - kotlin.math.abs(state.deviceHeading - state.qiblaBearing)
        ) <= 3f
        if (aligned) {
            Spacer(Modifier.height(16.dp))
            androidx.compose.material3.Surface(
                shape = RoundedCornerShape(50),
                color = PrimaryGold,
                tonalElevation = 0.dp,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.qibla_aligned),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Card(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Info, contentDescription = null, tint = TextMuted)
                Spacer(Modifier.size(12.dp))
                Column {
                    Text(stringResource(R.string.qibla_calibration_tip), style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    Text(stringResource(R.string.qibla_accuracy_tip), style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    if (state.locationFailed) {
                        Text(stringResource(R.string.qibla_location_failed), style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    }
                }
            }
        }
    }

        PullRefreshIndicator(
            refreshing = state.isLoading,
            state = pullState,
            backgroundColor = SurfaceWhite,
            contentColor = PrimaryBlue,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}
