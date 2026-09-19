package com.mawaqit.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mawaqit.app.R
import com.mawaqit.app.data.model.PrayerName

/**
 * Prayer display names from strings.xml (Rule 4 — never hardcoded inline).
 * Single shared helper so every screen shows identical names.
 */
@Composable
fun PrayerNameLabel(prayer: PrayerName): String = stringResource(prayerNameRes(prayer))

/**
 * Non-compose variant (PHASE_5 widget: RemoteViews contexts have no
 * stringResource — they resolve via Context.getString).
 */
fun prayerNameRes(prayer: PrayerName): Int = when (prayer) {
    PrayerName.FAJR -> R.string.fajr
    PrayerName.DHUHR -> R.string.dhuhr
    PrayerName.ASR -> R.string.asr
    PrayerName.MAGHRIB -> R.string.maghrib
    PrayerName.ISHA -> R.string.isha
}
