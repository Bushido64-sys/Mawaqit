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
import androidx.compose.runtime.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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

        Card(
            shape = androidx.compose.foundation.shape.CircleShape,
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.size(300.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
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

                    // cardinal labels
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.parseColor("#12181F")
                            textSize = 28f
                            textAlign = android.graphics.Paint.Align.CENTER
                            isFakeBoldText = true
                        }
                        drawText("N", c.x, c.y - r + 28f, paint)
                        drawText("S", c.x, c.y + r - 8f, paint)
                        drawText("E", c.x + r - 12f, c.y + 10f, paint)
                        drawText("W", c.x - r + 12f, c.y + 10f, paint)
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
