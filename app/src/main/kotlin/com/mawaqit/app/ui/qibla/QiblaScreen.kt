package com.mawaqit.app.ui.qibla

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.Button
import com.mawaqit.app.R
import com.mawaqit.app.ui.theme.BgOffwhite
import com.mawaqit.app.ui.theme.PrimaryGold
import com.mawaqit.app.ui.theme.SurfaceDeep
import com.mawaqit.app.ui.theme.SurfaceWhite
import com.mawaqit.app.ui.theme.TextMuted
import com.mawaqit.app.ui.theme.TextPrimary
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun QiblaScreen(viewModel: QiblaViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
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
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) viewModel.refreshLocation()
    }
    LaunchedEffect(Unit) {
        if (hasPermission) viewModel.refreshLocation()
    }
    val animatedRotation by animateFloatAsState(
        targetValue = state.needleRotation,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "needle"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgOffwhite)
            .padding(horizontal = 24.dp, vertical = 16.dp),
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
                Text(stringResource(R.string.qibla_location_off), modifier = Modifier.padding(16.dp), color = TextMuted)
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
}
