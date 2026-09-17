package com.mawaqit.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.mawaqit.app.ui.home.HomeScreen
import com.mawaqit.app.ui.theme.MawaqitTheme
import dagger.hilt.android.AndroidEntryPoint

// AppCompatActivity (not ComponentActivity) so per-app language switching via
// AppCompatDelegate.setApplicationLocales (PHASE_8) works on API < 33.
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            MawaqitTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // PHASE_2: temp screen proving the data pipeline.
                    // Navigation graph arrives in PHASE_4.
                    HomeScreen()
                }
            }
        }
    }
}
