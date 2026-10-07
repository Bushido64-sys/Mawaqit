package com.mawaqit.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import android.net.Uri
import android.widget.VideoView
import com.mawaqit.app.ui.theme.SplashGradientEnd
import com.mawaqit.app.ui.theme.SplashGradientStart

/**
 * Full-screen looping video background (ASSETS.md §2) with a graceful
 * gradient fallback.
 *
 * PHASE_6 reality: res/raw/splash_video.mp4 does NOT exist yet (user's
 * assignment/06 compression pending). `R.raw.*` ids are compile-time — a
 * missing file would break the build — so the video is resolved BY NAME via
 * Resources.getIdentifier (0 when absent). The day the MP4 lands, the video
 * plays automatically with zero code change. Until then: blue gradient
 * (Splash* tokens, DESIGN.md §1) — the reading screen is fully testable now.
 */
@Composable
fun VideoBackground(
    overlayAlpha: Float = 0.7f,
    modifier: Modifier = Modifier,
    muted: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val videoResId = remember {
        context.resources.getIdentifier("splash_video", "raw", context.packageName)
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (videoResId != 0) {
            androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
                var videoW by remember { mutableStateOf(0) }
                var videoH by remember { mutableStateOf(0) }
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            setVideoURI(Uri.parse("android.resource://${ctx.packageName}/$videoResId"))
                            setOnPreparedListener { mp ->
                                mp.isLooping = true
                                if (muted) mp.setVolume(0f, 0f)
                                videoW = mp.videoWidth
                                videoH = mp.videoHeight
                                start()
                            }
                        }
                    },
                    modifier = Modifier
                        .let {
                            val screenAspect = maxWidth.value / maxHeight.value
                            val videoAspect = if (videoW > 0 && videoH > 0) videoW.toFloat() / videoH else screenAspect
                            return@let if (videoAspect > screenAspect) {
                                // video wider → match height, overflow width
                                Modifier.height(maxHeight).width(maxHeight * videoAspect)
                            } else {
                                // video taller → match width, overflow height
                                Modifier.width(maxWidth).height(maxWidth / videoAspect)
                            }
                        }
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(SplashGradientStart, SplashGradientEnd)
                        )
                    )
            )
        }
        // Dark overlay — text readability over video/gradient (ASSETS.md §2)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = overlayAlpha))
        )
        content()
    }
}
