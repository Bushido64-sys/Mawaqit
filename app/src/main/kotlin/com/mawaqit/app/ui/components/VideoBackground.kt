package com.mawaqit.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val videoResId = remember {
        context.resources.getIdentifier("splash_video", "raw", context.packageName)
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (videoResId != 0) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        setVideoURI(Uri.parse("android.resource://${ctx.packageName}/$videoResId"))
                        setOnPreparedListener { it.isLooping = true }
                        start()
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
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
