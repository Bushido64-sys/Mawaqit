package com.mawaqit.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.Text
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.mawaqit.app.ui.theme.MawaqitTheme
import dagger.hilt.android.AndroidEntryPoint

// BuildConfig.GIT_SHA is stamped automatically at build time (see
// app/build.gradle.kts) so you can always tell WHICH build is on the phone.

// AppCompatActivity (not ComponentActivity) so per-app language switching via
// AppCompatDelegate.setApplicationLocales (PHASE_8) works on API < 33.
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            MawaqitTheme {
                // Navigation will be wired in PHASE_4 — placeholder for now:
                Text("Mawaqit — Phase 1 Setup Complete\nBuild: ${BuildConfig.GIT_SHA}")
            }
        }
    }
}
