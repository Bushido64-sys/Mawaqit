package com.mawaqit.app.ui.onboarding

import android.Manifest
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import com.mawaqit.app.R
import com.mawaqit.app.data.prefs.AzanOption
import com.mawaqit.app.data.repository.FALLBACK_AYAHS
import com.mawaqit.app.ui.components.VideoBackground
import com.mawaqit.app.ui.theme.PrimaryGold
import com.mawaqit.app.ui.theme.SurfaceWhite
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
    onComplete: () -> Unit
) {
    var step by remember { mutableStateOf(0) }
    var city by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("en") }
    var azan by remember { mutableStateOf(AzanOption.DEFAULT) }
    val context = LocalContext.current
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* user's choice; alarms still fire */ }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (step) {
                0 -> {
                    Text(stringResource(R.string.onboarding_welcome_title), fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.onboarding_welcome_subtitle), fontSize = 16.sp, color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(32.dp))
                    GoldButton(stringResource(R.string.onboard_continue)) { step = 1 }
                }
                1 -> {
                    Text(stringResource(R.string.onboard_page2_title), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.onboard_page2_body), color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(24.dp))
                    GoldButton(stringResource(R.string.onboard_detect)) {
                        viewModel.useGpsLocation()
                        step = 2
                    }
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(value = city, onValueChange = { city = it }, placeholder = { Text(stringResource(R.string.onboard_city_hint)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = country, onValueChange = { country = it }, placeholder = { Text(stringResource(R.string.onboard_country_hint)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(16.dp))
                    GoldButton(stringResource(R.string.onboard_continue)) {
                        if (city.isNotBlank() && country.isNotBlank()) viewModel.setManualCity(city, country)
                        step = 2
                    }
                }
                2 -> {
                    Text(stringResource(R.string.onboard_page3_title), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.onboard_page3_body), color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(24.dp))
                    val sample = FALLBACK_AYAHS.first()
                    listOf("en" to "English", "ur" to "اردو").forEach { (tag, label) ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            border = if (language == tag) BorderStroke(2.dp, PrimaryGold) else null,
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                            onClick = {
                                language = tag
                                viewModel.setAppLanguage(tag)
                                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
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
                    Spacer(Modifier.height(24.dp))
                    GoldButton(stringResource(R.string.onboard_continue)) { step = 3 }
                }
                3 -> {
                    Text(stringResource(R.string.onboard_page4_title), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.onboard_page4_body), color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(24.dp))
                    listOf(AzanOption.DEFAULT to "Default Azan", AzanOption.MAKKAH to "Makkah Azan").forEach { (opt, label) ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            border = if (azan == opt) BorderStroke(2.dp, PrimaryGold) else null,
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(label, fontWeight = FontWeight.Medium)
                                Row {
                                    TextButton(onClick = { viewModel.previewAzan(opt) }) { Text("Play") }
                                    TextButton(onClick = {
                                        azan = opt
                                        viewModel.setSelectedAzan(opt)
                                    }) { Text(if (azan == opt) stringResource(R.string.settings_selected) else stringResource(R.string.settings_select)) }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                        Text(stringResource(R.string.onboard_allow_notifs))
                    }
                    Spacer(Modifier.height(16.dp))
                    GoldButton(stringResource(R.string.onboard_continue)) { step = 4 }
                }
                4 -> {
                    Text(stringResource(R.string.onboard_page5_title), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.onboard_page5_body), fontSize = 16.sp, color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(32.dp))
                    GoldButton(stringResource(R.string.onboard_lets_roll)) {
                        viewModel.stopPreview()
                        viewModel.setOnboardingComplete()
                        onComplete()
                    }
                }
            }
            if (step in 1..3) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = {
                    viewModel.stopPreview()
                    viewModel.setOnboardingComplete()
                    onComplete()
                }) { Text(stringResource(R.string.onboard_skip)) }
            }
        }
    }
}

@Composable
private fun GoldButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = MaterialTheme.colorScheme.onSurface),
        modifier = Modifier.fillMaxWidth(0.7f)
    ) { Text(text, fontWeight = FontWeight.SemiBold) }
}
