package com.mawaqit.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mawaqit.app.R
import com.mawaqit.app.ui.theme.PrimaryGold
import com.mawaqit.app.ui.theme.SurfaceDeep
import com.mawaqit.app.util.formatCountdown

/**
 * DESIGN.md Screen 3 hero: dark (SurfaceDeep) 24dp-radius card — "Next Prayer"
 * label, prayer name as hero text, time in gold, then the live countdown ring
 * (gold arc on a faint white track, countdown text centered inside).
 * Flat — no shadow (DESIGN.md §3).
 *
 * [progress] 1.0 → 0.0 across the session (previous prayer → next prayer);
 * computed by HomeViewModel.
 */
@Composable
fun NextPrayerCard(
    prayerLabel: String,
    timeStr: String,
    countdown: String,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val ringSize = 96.dp
    val ringStroke = 6.dp
    val trackColor = Color.White.copy(alpha = 0.15f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceDeep)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.next_prayer),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.7f)
        )
        Text(
            text = prayerLabel,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
        Text(
            text = timeStr,
            style = MaterialTheme.typography.titleMedium,
            color = PrimaryGold
        )

        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier.size(ringSize),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(ringSize)) {
                val stroke = Stroke(width = ringStroke.toPx(), cap = StrokeCap.Round)
                // Track (full circle)
                drawArc(
                    color = trackColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = stroke
                )
                // Progress arc — sweeps with remaining fraction
                drawArc(
                    color = PrimaryGold,
                    startAngle = -90f,
                    sweepAngle = 360f * progress.coerceIn(0f, 1f),
                    useCenter = false,
                    style = stroke
                )
            }
            Text(
                text = countdown,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
