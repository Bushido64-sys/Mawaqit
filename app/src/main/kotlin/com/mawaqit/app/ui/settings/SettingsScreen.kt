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
import com.mawaqit.app.data.prefs.AzanOption
import com.mawaqit.app.ui.theme.PrimaryGold
import com.mawaqit.app.ui.theme.SurfaceWhite
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showAzanSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.refreshHealth() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            stringResource(R.string.settings),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Semibold
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
                Text(if (state.selectedAzan == AzanOption.MAKKAH) stringResource(R.string.azan_makkah) else stringResource(R.string.azan_default))
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
                            containerColor = if (state.appLanguage == tag) PrimaryGold else MaterialTheme.colorScheme.surfaceVariant
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
                            containerColor = if (kotlin.math.abs(state.readerFontScale - mult) < 0.01f) PrimaryGold else MaterialTheme.colorScheme.surfaceVariant
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
                TextButton(onClick = viewModel::changeLocation, enabled = !state.locationUpdating) {
                    Text(if (state.locationUpdating) "…" else stringResource(R.string.settings_change))
                }
            }
            if (state.locationError != null) Text(state.locationError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        }
        Spacer(Modifier.height(12.dp))

        // ── Calculation method (display-only) ────────────────────────────
        SectionCard {
            Text(stringResource(R.string.settings_calculation_method), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(stringResource(R.string.settings_karachi_method), fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
        }
        Spacer(Modifier.height(12.dp))

        // ── About ────────────────────────────────────────────────────────
        SectionCard {
            Text(stringResource(R.string.settings_about), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text("${stringResource(R.string.settings_app_version)}: ${state.appVersion}", fontSize = 14.sp)
            Text("${stringResource(R.string.settings_prayer_times_by)} AlAdhan.com", fontSize = 14.sp)
        }
    }

    if (showAzanSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(onDismissRequest = {
            viewModel.stopAzanPreview()
            showAzanSheet = false
        }, sheetState = sheetState) {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.settings_azan_sound), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                AzanOption.entries.forEach { option ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (option == AzanOption.MAKKAH) stringResource(R.string.azan_makkah) else stringResource(R.string.azan_default))
                        Row {
                            TextButton(onClick = { viewModel.previewAzan(option) }) { Text(stringResource(R.string.settings_preview)) }
                            Button(onClick = { viewModel.setSelectedAzan(option) }) {
                                Text(if (state.selectedAzan == option) stringResource(R.string.settings_selected) else stringResource(R.string.settings_select))
                            }
                        }
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
