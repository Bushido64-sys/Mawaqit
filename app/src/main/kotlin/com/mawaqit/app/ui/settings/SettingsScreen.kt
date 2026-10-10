package com.mawaqit.app.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Slider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.mawaqit.app.R
import com.mawaqit.app.data.model.PrayerName
import com.mawaqit.app.alarm.AzanType
import com.mawaqit.app.ui.theme.PrimaryGold
import com.mawaqit.app.ui.components.OrnamentHeader
import com.mawaqit.app.ui.theme.SurfaceWhite
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale

data class CalcMethod(val id: Int, val name: String)

private val CALCULATION_METHODS = listOf(
    CalcMethod(1, "University of Islamic Sciences, Karachi"),
    CalcMethod(4, "Umm al-Qura, Makkah"),
    CalcMethod(3, "Muslim World League"),
    CalcMethod(2, "Islamic Society of North America (ISNA)"),
    CalcMethod(5, "Egyptian General Authority of Survey"),
    CalcMethod(13, "Diyanet, Turkey"),
    CalcMethod(8, "Gulf Region"),
    CalcMethod(15, "Moonsighting Committee Worldwide")
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val methodId by viewModel.calculationMethod.collectAsStateWithLifecycle()
    val countries by viewModel.countries.collectAsStateWithLifecycle()
    val cities by viewModel.cities.collectAsStateWithLifecycle()
    val placesLoading by viewModel.placesLoading.collectAsStateWithLifecycle()
    val placesError by viewModel.placesError.collectAsStateWithLifecycle()
    // Which voice is previewing + how far through — drives the animated play/pause
    // button and the progress bar (PHASE-10.3).
    val previewing by viewModel.previewing.collectAsStateWithLifecycle()
    val previewProgress by viewModel.previewProgress.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showAzanSheet by remember { mutableStateOf(false) }
    var showLocationSheet by remember { mutableStateOf(false) }
    var showMethodSheet by remember { mutableStateOf(false) }
    var pickedCountry by remember { mutableStateOf<String?>(null) }
    var countryQuery by remember { mutableStateOf("") }
    var cityQuery by remember { mutableStateOf("") }
    val stateMethod = methodId
    val filteredCountries = remember(countries, countryQuery) {
        countries.filter { it.name.contains(countryQuery, ignoreCase = true) }
    }
    val filteredCities = remember(cities, cityQuery) {
        cities.filter { it.contains(cityQuery, ignoreCase = true) }
    }

    LaunchedEffect(Unit) { viewModel.refreshHealth() }

    // Fix Now opens system pages — re-check health the moment the user returns,
    // so the card updates without leaving the Settings tab (user report).
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) viewModel.refreshHealth()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        OrnamentHeader()
        Text(
            stringResource(R.string.settings),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(12.dp))

        // ── Alarm Health (auto-hides when all green) ─────────────────────
        if (!state.notificationsAllowed || !state.exactAlarmAllowed || state.batteryOptimized) {
            SectionCard {
                Text(stringResource(R.string.settings_alarm_health), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                if (!state.notificationsAllowed) HealthRow(stringResource(R.string.settings_notifications_off)) {
                    context.startActivity(Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName)
                    })
                }
                if (!state.exactAlarmAllowed) HealthRow(stringResource(R.string.settings_exact_alarm_off)) {
                    context.startActivity(Intent(AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:${context.packageName}")
                    })
                }
                if (state.batteryOptimized) HealthRow(stringResource(R.string.settings_battery_desc)) {
                    context.startActivity(Intent(AndroidSettings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    })
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // ── Prayer Alarms ────────────────────────────────────────────────
        SectionCard {
            Text(stringResource(R.string.settings_prayer_alarms), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            PrayerName.entries.forEach { prayer ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(prayer.name.lowercase().replaceFirstChar { it.uppercase() })
                    Switch(
                        checked = state.alarmStates[prayer] ?: true,
                        onCheckedChange = { viewModel.toggleAlarm(prayer, it) }
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        // ── Azan Sound + volume + force alarm ────────────────────────────
        SectionCard {
            Text(stringResource(R.string.settings_azan_sound), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Row(
                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(state.selectedAzan.labelRes))
                TextButton(onClick = { showAzanSheet = true }) { Text(stringResource(R.string.settings_change)) }
            }
            Text(stringResource(R.string.settings_azan_volume), fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
            Slider(
                value = state.azanVolume,
                onValueChange = viewModel::setAzanVolume,
                valueRange = 0f..1f
            )
            Row(
                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_force_alarm))
                    Text(stringResource(R.string.settings_force_alarm_desc), fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                }
                Switch(checked = state.azanForceAlarm, onCheckedChange = viewModel::setAzanForceAlarm)
            }
        }
        Spacer(Modifier.height(12.dp))

        // ── Language ─────────────────────────────────────────────────────
        SectionCard {
            Text(stringResource(R.string.settings_language), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Row(Modifier.padding(vertical = 8.dp)) {
                listOf("en" to "English", "ur" to "اردو").forEach { (tag, label) ->
                    Button(
                        onClick = {
                            viewModel.setAppLanguage(tag)
                            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.appLanguage == tag) PrimaryGold else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    ) { Text(label) }
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        // ── Reader font size ─────────────────────────────────────────────
        SectionCard {
            Text(stringResource(R.string.settings_reader_font_size), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Row(Modifier.padding(vertical = 8.dp)) {
                listOf(0.85f to "S", 1.0f to "M", 1.15f to "L", 1.3f to "XL").forEach { (mult, label) ->
                    Button(
                        onClick = { viewModel.setReaderFontScale(mult) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (kotlin.math.abs(state.readerFontScale - mult) < 0.01f) PrimaryGold else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    ) { Text(label) }
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        // ── Location ─────────────────────────────────────────────────────
        SectionCard {
            Text(stringResource(R.string.settings_location), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Row(
                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(state.cityName ?: stringResource(R.string.settings_gps_location))
                TextButton(onClick = { showLocationSheet = true }, enabled = !state.locationUpdating) {
                    Text(if (state.locationUpdating) "…" else stringResource(R.string.settings_change))
                }
            }
            if (state.locationError != null) Text(state.locationError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        }
        Spacer(Modifier.height(12.dp))

        // ── Calculation method ───────────────────────────────────────────
        SectionCard {
            Text(stringResource(R.string.settings_calculation_method), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    CALCULATION_METHODS.firstOrNull { it.id == stateMethod }?.name
                        ?: stringResource(R.string.settings_karachi_method),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp, end = 8.dp)
                )
                TextButton(onClick = { showMethodSheet = true }) {
                    Text(stringResource(R.string.settings_change))
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        // ── About ────────────────────────────────────────────────────────
        SectionCard {
            Text(stringResource(R.string.settings_about), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text("${stringResource(R.string.settings_app_version)}: ${state.appVersion}", fontSize = 14.sp)
            Text("${stringResource(R.string.settings_prayer_times_by)} AlAdhan.com", fontSize = 14.sp)
        }
    }

    if (showMethodSheet) {
        val methodSheetState = rememberModalBottomSheetState()
        ModalBottomSheet(onDismissRequest = { showMethodSheet = false }, sheetState = methodSheetState) {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.settings_calculation_method), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                CALCULATION_METHODS.forEach { m ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(m.name)
                        TextButton(onClick = {
                            viewModel.setCalculationMethod(m.id)
                            showMethodSheet = false
                        }) {
                            Text(if (methodId == m.id) stringResource(R.string.settings_selected) else stringResource(R.string.settings_select))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showLocationSheet) {
        val locSheetState = rememberModalBottomSheetState()
        ModalBottomSheet(onDismissRequest = { showLocationSheet = false; pickedCountry = null }, sheetState = locSheetState) {
            Column(Modifier.padding(16.dp).height(480.dp).verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.settings_location), fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                TextButton(onClick = {
                    showLocationSheet = false
                    viewModel.changeLocation() // GPS auto-detect (existing flow)
                }) { Text("Detect automatically (GPS)", fontSize = 16.sp) }
                if (pickedCountry == null) {
                    LaunchedEffect(Unit) { viewModel.loadCountries() }
                    androidx.compose.material3.OutlinedTextField(
                        value = countryQuery,
                        onValueChange = { countryQuery = it },
                        placeholder = { Text("Search country") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        singleLine = true
                    )
                    if (placesLoading) Text("Loading…", fontSize = 14.sp)
                    if (placesError != null) Text(placesError!!, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                    filteredCountries.forEach { country ->
                        androidx.compose.material3.TextButton(
                            onClick = {
                                pickedCountry = country.name
                                cityQuery = ""
                                viewModel.loadCities(country.name)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(country.name, fontSize = 17.sp) }
                    }
                } else {
                    TextButton(onClick = { pickedCountry = null; cityQuery = "" }) { Text("← $pickedCountry", fontSize = 16.sp) }
                    androidx.compose.material3.OutlinedTextField(
                        value = cityQuery,
                        onValueChange = { cityQuery = it },
                        placeholder = { Text("Search city") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        singleLine = true
                    )
                    if (cities.isEmpty() && !placesLoading) Text("No cities found", fontSize = 14.sp)
                    filteredCities.forEach { city ->
                        androidx.compose.material3.TextButton(
                            onClick = {
                                viewModel.pickManualCity(city, pickedCountry!!)
                                showLocationSheet = false
                                pickedCountry = null
                                cityQuery = ""
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(city, fontSize = 17.sp) }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showAzanSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(onDismissRequest = {
            viewModel.stopAzanPreview()
            showAzanSheet = false
        }, sheetState = sheetState) {
            // 6 voices (PHASE-10) is ~300dp of rows — tall enough to clip against the
            // sheet's partial-expansion height on a small phone. Scroll so nothing hides.
            Column(
                Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(stringResource(R.string.settings_azan_sound), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                AzanType.choices.forEach { option ->
                    val isPreviewing = previewing == option
                    val buttonScale by animateFloatAsState(
                        targetValue = if (isPreviewing) 1.08f else 1f,
                        animationSpec = tween(180),
                        label = "preview_scale"
                    )
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(option.labelRes))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Play -> pause toggle beside the Select button, driven by
                            // real playback state (user request).
                            IconButton(
                                onClick = { viewModel.previewAzan(option) },
                                modifier = Modifier
                                    .size(40.dp)
                                    .scale(buttonScale)
                                    .background(PrimaryBlue, CircleShape)
                            ) {
                                Crossfade(
                                    targetState = isPreviewing,
                                    animationSpec = tween(180),
                                    label = "preview_toggle"
                                ) { playing ->
                                    Icon(
                                        imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        contentDescription = stringResource(if (playing) R.string.settings_pause_preview else R.string.settings_preview),
                                        tint = androidx.compose.ui.graphics.Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = { viewModel.setSelectedAzan(option) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (state.selectedAzan == option) PrimaryGold else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Text(if (state.selectedAzan == option) stringResource(R.string.settings_selected) else stringResource(R.string.settings_select))
                            }
                        }
                    }
                    if (isPreviewing) {
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { previewProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(50)),
                            color = PrimaryBlue,
                            trackColor = PrimaryBlue.copy(alpha = 0.18f)
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) { content() }
    }
}

@Composable
private fun HealthRow(text: String, onFix: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, fontSize = 14.sp, modifier = Modifier.weight(1f))
        TextButton(onClick = onFix) { Text(stringResource(R.string.settings_fix_now)) }
    }
}
