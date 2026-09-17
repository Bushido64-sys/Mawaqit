package com.mawaqit.app.data.api

import com.google.gson.annotations.SerializedName

/**
 * AlAdhan response DTOs — shapes match API_REFERENCE.md (live-verified).
 * Only the fields we actually use are mapped; Gson ignores the rest.
 */

/** GET /calendar/{year}/{month} → data is an array of days */
data class AladhanCalendarResponse(
    val code: Int?,
    val status: String?,
    val data: List<AladhanDay>?
)

/** GET /timingsByCity/{date} → data is a single day */
data class AladhanDayResponse(
    val code: Int?,
    val status: String?,
    val data: AladhanDay?
)

data class AladhanDay(
    val timings: AladhanTimings?,
    val date: AladhanDateInfo?
)

data class AladhanTimings(
    @SerializedName("Fajr")    val fajr: String?,
    @SerializedName("Dhuhr")   val dhuhr: String?,
    @SerializedName("Asr")     val asr: String?,
    @SerializedName("Maghrib") val maghrib: String?,
    @SerializedName("Isha")    val isha: String?
    // Sunrise/Sunset/Imsak/Midnight etc. intentionally unmapped — not needed (API_REFERENCE.md)
)

data class AladhanDateInfo(
    val gregorian: AladhanGregorian?
)

data class AladhanGregorian(
    // "15-09-2026" — AlAdhan's DD-MM-YYYY; convert to ISO at the parsing boundary!
    @SerializedName("date") val date: String?
)
