package com.mawaqit.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.mawaqit.app.ui.MawaqitNavGraph
import com.mawaqit.app.ui.onboarding.OnboardingScreen
import com.mawaqit.app.ui.splash.SplashScreen
import com.mawaqit.app.ui.theme.MawaqitTheme
import com.mawaqit.app.data.prefs.PrefsRepository
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch

// AppCompatActivity (not ComponentActivity) so per-app language switching via
// AppCompatDelegate.setApplicationLocales (PHASE_8) works on API < 33.
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var prefs: PrefsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            MawaqitTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val scope = rememberCoroutineScope()
                    // Onboarding already done? Splash then plays longer with a
                    // muted background video (user request, PHASE-9.1).
                    var onboardingDone by remember { mutableStateOf<Boolean?>(null) }
                    androidx.compose.runtime.LaunchedEffect(Unit) {
                        onboardingDone = prefs.getOnboardingCompleteOnce()
                    }
                    onboardingDone?.let { done ->
                    androidx.navigation.compose.NavHost(navController = navController, startDestination = "splash") {
                        composable("splash") {
                            SplashScreen(prefs = prefs, longSplash = done) {
                                scope.launch {
                                    navController.navigate(if (done) "main" else "onboarding") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                }
                            }
                        }
                        composable("onboarding") {
                            OnboardingScreen {
                                navController.navigate("main") {
                                    popUpTo("onboarding") { inclusive = true }
                                }
                            }
                        }
                        composable("main") {
                            MawaqitNavGraph(rememberNavController())
                        }
                    }
                    }
                }
            }
        }
    }
}
