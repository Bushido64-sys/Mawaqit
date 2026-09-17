package com.mawaqit.app.util

import com.mawaqit.app.data.model.PrayerName
import com.mawaqit.app.data.model.PrayerTimings
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale

/**
 * AlAdhan sometimes appends a timezone label to timings ("04:38 (PKT)") and
 * sometimes not ("04:38"). NEVER trust the value to be exactly 5 chars —
 * extract the HH:mm part (API_REFERENCE.md gotcha). Apply BEFORE storing
 * in Room or parsing to millis.
 */
fun String.toHhMm(): String = Regex("\\d{2}:\\d{2}").find(this)?.value ?: this

/**
 * "15:31" + a date → epoch milliseconds.
 *
 * NOTE (MVP): prayer times are computed by AlAdhan for the SAVED LOCATION's
 * timezone (e.g. Asia/Karachi), but we map them using the DEVICE timezone.
 * For our target audience (device tz == home tz) this is correct; revisit only
 * if travel-timezone support is ever requested (not in MVP scope).
 */
fun parseTimeToMillis(timeStr: String, date: LocalDate): Long =
    LocalTime.parse(timeStr)
        .atDate(date)
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

/** 9_271_000L → "02:34:31" */
fun formatCountdown(millisUntil: Long): String {
    val totalSeconds = (millisUntil.coerceAtLeast(0)) / 1000
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
}

enum class PrayerStatus { PRAYED, MISSED, CURRENT, UPCOMING }

/**
 * Classify each of today's prayers against [nowMillis].
 * MVP simplification: past prayers are marked PRAYED (they may or may not have
 * been — distinguishing MISSED needs the salah_log, wired in PHASE_4's UI).
 * CURRENT = the most recent prayer that has already passed.
 */
fun getCurrentPrayerStatus(
    timings: PrayerTimings,
    nowMillis: Long = System.currentTimeMillis()
): Map<PrayerName, PrayerStatus> {
    val date = LocalDate.parse(timings.date) // ISO
    val ordered = linkedMapOf(
        PrayerName.FAJR to timings.fajr,
        PrayerName.DHUHR to timings.dhuhr,
        PrayerName.ASR to timings.asr,
        PrayerName.MAGHRIB to timings.maghrib,
        PrayerName.ISHA to timings.isha
    )
    val result = LinkedHashMap<PrayerName, PrayerStatus>()
    var currentSet = false
    // iterate latest→earliest for finding CURRENT, then fill the map in order
    val entries = ordered.entries.toList()
    for ((name, timeStr) in entries) {
        val millis = parseTimeToMillis(timeStr, date)
        result[name] = when {
            millis > nowMillis -> PrayerStatus.UPCOMING
            !currentSet -> { currentSet = true; PrayerStatus.CURRENT }
            else -> PrayerStatus.PRAYED
        }
    }
    return result
}
