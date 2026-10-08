package com.mawaqit.app.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.animateColorAsState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import com.mawaqit.app.R
import com.mawaqit.app.data.prefs.AzanOption
import com.mawaqit.app.data.repository.FALLBACK_AYAHS
import com.mawaqit.app.ui.components.VideoBackground
import com.mawaqit.app.ui.theme.PrimaryGold
import com.mawaqit.app.ui.theme.SurfaceWhite
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.MainScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
    onComplete: () -> Unit
) {
    var step by remember { mutableStateOf(0) }
    var language by remember { mutableStateOf("en") }
    var azan by remember { mutableStateOf(AzanOption.DEFAULT) }
    var selectedCity by remember { mutableStateOf<String?>(null) }
    var pickedCountry by remember { mutableStateOf<String?>(null) }
    val countries by viewModel.countries.collectAsStateWithLifecycle()
    val countryNames = remember(countries) { countries.map { it.name } }
    val cities = remember(countries, pickedCountry) {
        countries.firstOrNull { it.name == pickedCountry }?.cities.orEmpty()
    }
    val context = LocalContext.current
    var notifGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> notifGranted = granted }
    var locationDenied by remember { mutableStateOf(false) }
    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            locationDenied = false
            // Permission alone doesn't turn the radio on — force the system
            // sheet first, then fetch (same Phase-7 fix as Qibla/Home).
            val req = com.google.android.gms.location.LocationRequest
                .Builder(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, 10_000).build()
            val sreq = com.google.android.gms.location.LocationSettingsRequest.Builder()
                .addLocationRequest(req).setAlwaysShow(true).build()
            com.google.android.gms.location.LocationServices
                .getSettingsClient(context)
                .checkLocationSettings(sreq).addOnCompleteListener { task ->
                    try {
                        task.getResult(Exception::class.java)
                        viewModel.useGpsLocation()
                        step = 2
                    } catch (e: com.google.android.gms.common.api.ResolvableApiException) {
                        runCatching {
                            e.startResolutionForResult(context as android.app.Activity, 1002)
                        }
                        kotlinx.coroutines.MainScope().launch {
                            repeat(15) {
                                kotlinx.coroutines.delay(2_000)
                                val lm = context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
                                val on = try {
                                    lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
                                        lm.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
                                } catch (_: Exception) { true }
                                if (on) { viewModel.useGpsLocation(); step = 2; return@launch }
                            }
                        }
                    } catch (_: Exception) {
                        viewModel.useGpsLocation()
                        step = 2
                    }
                }
        } else {
            locationDenied = true
        }
    }

    VideoBackground(overlayAlpha = 0.62f, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            com.mawaqit.app.ui.components.OrnamentHeader()
            Spacer(Modifier.height(16.dp))
            when (step) {
                0 -> {
                    StepCard {
                        Text(stringResource(R.string.onboarding_welcome_title), fontSize = 26.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.onboarding_welcome_subtitle), fontSize = 15.sp, color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(24.dp))
                        OnboardPrimaryButton(stringResource(R.string.onboard_continue)) { step = 1 }
                    }
                }
                1 -> {
                    StepCard {
                        Text(stringResource(R.string.onboard_page2_title), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.onboard_page2_body), fontSize = 14.sp, color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        Row(Modifier.fillMaxWidth()) {
                            Column(Modifier.weight(1f)) {
                                Text("Country", fontWeight = FontWeight.SemiBold, color = PrimaryGold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                                androidx.compose.runtime.key(countryNames) {
                                    WheelPicker(
                                        items = countryNames,
                                        enabled = countryNames.isNotEmpty(),
                                        modifier = Modifier.fillMaxWidth(),
                                        onSettled = { i -> pickedCountry = countryNames.getOrNull(i) }
                                    )
                                }
                            }
                            Spacer(Modifier.height(0.dp))
                            Column(Modifier.weight(1f)) {
                                Text("City", fontWeight = FontWeight.SemiBold, color = PrimaryGold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                                androidx.compose.runtime.key(pickedCountry, cities) {
                                    WheelPicker(
                                        items = cities,
                                        enabled = pickedCountry != null && cities.isNotEmpty(),
                                        modifier = Modifier.fillMaxWidth(),
                                        onSettled = { i ->
                                            val c = cities.getOrNull(i)
                                            if (c != null && pickedCountry != null) {
                                                selectedCity = c
                                                viewModel.pickManualLocation(c, pickedCountry!!)
                                            }
                                        }
                                    )
                                }
                                if (pickedCountry == null) {
                                    Text("Select country first", fontSize = 12.sp, color = com.mawaqit.app.ui.theme.PrimaryBlue.copy(alpha = 0.7f))
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        OnboardPrimaryButton(stringResource(R.string.onboard_detect)) {
                            locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                        if (locationDenied) {
                            Text("Location denied — pick your country and city in the wheels, or allow it in Settings.", fontSize = 12.sp, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                        }
                        Spacer(Modifier.height(8.dp))
                        OnboardPrimaryButton(stringResource(R.string.onboard_continue)) { step = 2 }
                        Spacer(Modifier.height(4.dp))
                        SkipLink(vm = viewModel, language = language) { onComplete() }
                    }
                }
                2 -> {
                    StepCard {
                        Text(stringResource(R.string.onboard_page3_title), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.onboard_page3_body), fontSize = 14.sp, color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        val sample = FALLBACK_AYAHS.first()
                        listOf("en" to "English", "ur" to "اردو").forEach { (tag, label) ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                border = if (language == tag) BorderStroke(2.5.dp, com.mawaqit.app.ui.theme.PrimaryBlue) else BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.35f)),
                                colors = CardDefaults.cardColors(containerColor = if (language == tag) com.mawaqit.app.ui.theme.ChipBg else SurfaceWhite),
                                onClick = {
                                    language = tag
                                    // Persist only — setApplicationLocales is applied
                                    // once at "Let's Roll". Applying it here recreates
                                    // the Activity and restarts the onboarding flow
                                    // (user bug, PHASE-9.1).
                                    viewModel.setAppLanguage(tag)
                                }
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(sample.arabic, fontSize = 20.sp)
                                    Spacer(Modifier.height(6.dp))
                                    Text(if (tag == "en") sample.english else sample.urdu, fontSize = 14.sp)
                                    Text(sample.reference, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                    Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = PrimaryGold)
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        OnboardPrimaryButton(stringResource(R.string.onboard_continue)) { step = 3 }
                        Spacer(Modifier.height(4.dp))
                        SkipLink(vm = viewModel, language = language) { onComplete() }
                    }
                }
                3 -> {
                    StepCard {
                        Text(stringResource(R.string.onboard_page4_title), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.onboard_page4_body), fontSize = 14.sp, color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        listOf(AzanOption.DEFAULT to "Default Azan", AzanOption.MAKKAH to "Makkah Azan").forEach { (opt, label) ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                border = if (azan == opt) BorderStroke(2.5.dp, com.mawaqit.app.ui.theme.PrimaryBlue) else BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.35f)),
                                colors = CardDefaults.cardColors(containerColor = if (azan == opt) com.mawaqit.app.ui.theme.ChipBg else SurfaceWhite),
                                onClick = {
                                    azan = opt
                                    viewModel.setSelectedAzan(opt)
                                }
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(label, fontWeight = FontWeight.Medium)
                                    Box(
                                        Modifier
                                            .height(40.dp)
                                            .clip(CircleShape)
                                            .background(com.mawaqit.app.ui.theme.PrimaryBlue),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        androidx.compose.material3.IconButton(onClick = { viewModel.previewAzan(opt) }) {
                                            Icon(Icons.Filled.PlayArrow, contentDescription = "Play preview", tint = androidx.compose.ui.graphics.Color.White)
                                        }
                                    }
                                }
                            }
                        }
                        if (!notifGranted) {
                            Spacer(Modifier.height(12.dp))
                            TextButton(onClick = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                                Text(stringResource(R.string.onboard_allow_notifs), color = com.mawaqit.app.ui.theme.PrimaryBlue)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        OnboardPrimaryButton(stringResource(R.string.onboard_continue)) { step = 4 }
                        Spacer(Modifier.height(4.dp))
                        SkipLink(vm = viewModel, language = language) { onComplete() }
                    }
                }
                4 -> {
                    StepCard {
                        Text(stringResource(R.string.onboard_page5_title), fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.onboard_page5_body), fontSize = 15.sp, color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(24.dp))
                        OnboardPrimaryButton(stringResource(R.string.onboard_lets_roll)) {
                            viewModel.stopPreview()
                            viewModel.finish {
                                // Apply the saved language NOW — applying it earlier
                                // recreates the Activity mid-flow (user bug).
                                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language))
                                onComplete()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SkipLink(vm: OnboardingViewModel, language: String, onSkipped: () -> Unit) {
    androidx.compose.material3.TextButton(onClick = {
        vm.stopPreview()
        vm.finish {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language))
            onSkipped()
        }
    }) { Text(stringResource(R.string.onboard_skip), color = PrimaryGold) }
}

@Composable
private fun StepCard(content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite.copy(alpha = 0.96f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) { content() }
    }
}

@Composable
private fun OnboardPrimaryButton(text: String, onClick: () -> Unit) {
    val interaction = androidx.compose.foundation.interaction.MutableInteractionSource()
    val pressed by interaction.collectIsPressedAsState()
    val container by androidx.compose.animation.animateColorAsState(
        targetValue = if (pressed) com.mawaqit.app.ui.theme.PrimaryBlue else androidx.compose.ui.graphics.Color.Transparent,
        label = "btn_bg"
    )
    val fg by androidx.compose.animation.animateColorAsState(
        targetValue = if (pressed) androidx.compose.ui.graphics.Color.White else com.mawaqit.app.ui.theme.PrimaryBlue,
        label = "btn_fg"
    )
    Button(
        onClick = onClick,
        interactionSource = interaction,
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = fg),
        border = BorderStroke(2.dp, com.mawaqit.app.ui.theme.PrimaryBlue),
        modifier = Modifier.fillMaxWidth(0.7f)
    ) { Text(text, fontWeight = FontWeight.SemiBold) }
}
