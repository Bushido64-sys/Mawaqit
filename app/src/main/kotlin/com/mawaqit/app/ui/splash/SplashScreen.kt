package com.mawaqit.app.ui.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import com.mawaqit.app.R
import com.mawaqit.app.data.prefs.PrefsRepository
import com.mawaqit.app.ui.components.VideoBackground
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue

/**
 * PHASE_9 splash — full-screen bundled video, ONE rotating Islamic card per
 * launch (~5s: 800ms fade in → 3.4s hold → 800ms fade out). Tap skips.
 * Index rotation via PrefsRepository (DataStore) — replaces the guidebook's
 * SharedPreferences snippet (ponytail: one prefs API).
 */
@Composable
fun SplashScreen(
    prefs: PrefsRepository,
    longSplash: Boolean = false,
    onSplashComplete: () -> Unit
) {
    data class Card(val h: Int, val d: Int, val r: Int)
    val cards = listOf(
        Card(R.string.splash_card1_h, R.string.splash_card1_d, R.string.splash_card1_r),
        Card(R.string.splash_card2_h, R.string.splash_card2_d, R.string.splash_card2_r),
        Card(R.string.splash_card3_h, R.string.splash_card3_d, R.string.splash_card3_r),
        Card(R.string.splash_card4_h, R.string.splash_card4_d, R.string.splash_card4_r),
        Card(R.string.splash_card5_h, R.string.splash_card5_d, R.string.splash_card5_r)
    )
    var index by remember { mutableIntStateOf(0) }
    var alpha by remember { mutableFloatStateOf(0f) }
    val animatedAlpha by animateFloatAsState(targetValue = alpha, animationSpec = tween(800), label = "splash_fade")

    var didFinish by remember { mutableStateOf(false) }
    val finish = {
        if (!didFinish) {
            didFinish = true
            onSplashComplete()
        }
    }

    LaunchedEffect(Unit) {
        val stored = prefs.getSplashCardIndexOnce()
        index = stored % cards.size
        prefs.setSplashCardIndex((stored + 1) % cards.size)
        alpha = 1f
        delay(if (longSplash) 8600 else 4200)
        alpha = 0f
        delay(800)
        finish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { finish() }
    ) {
        VideoBackground(overlayAlpha = 0.55f, modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { this.alpha = animatedAlpha }
                    .padding(horizontal = 32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("✦", color = Color(0xFFE38F33), fontSize = 24.sp)
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(cards[index].h),
                    color = Color.White,
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(cards[index].d),
                    color = Color(0xCCFFFFFF),
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(cards[index].r),
                    color = Color(0x99FFFFFF),
                    fontSize = 13.sp
                )
            }
        }
    }
}
