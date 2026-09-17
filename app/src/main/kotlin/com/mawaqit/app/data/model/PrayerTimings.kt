package com.mawaqit.app.data.model

/**
 * Prayer times for ONE day.
 * [date] is ISO YYYY-MM-DD (see DATA_SCHEMA.md — all stored dates are ISO).
 * Time strings are plain "HH:mm" (timezone suffixes stripped at the parsing layer).
 */
data class PrayerTimings(
    val date: String,      // "2026-09-15"
    val fajr: String,      // "04:38"
    val dhuhr: String,     // "12:12"
    val asr: String,       // "15:31"
    val maghrib: String,   // "18:27"
    val isha: String,      // "19:46"
    val latitude: Double,
    val longitude: Double
)

enum class PrayerName { FAJR, DHUHR, ASR, MAGHRIB, ISHA }

/** The next upcoming prayer, with epoch millis for alarm scheduling (PHASE_3). */
data class NextPrayer(
    val name: PrayerName,
    val timeStr: String,    // "15:31"
    val timeMillis: Long    // epoch milliseconds
)
