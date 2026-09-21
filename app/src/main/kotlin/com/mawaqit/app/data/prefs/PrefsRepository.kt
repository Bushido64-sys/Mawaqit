package com.mawaqit.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mawaqit.app.data.model.DailyAyah
import com.mawaqit.app.data.model.PrayerName
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** The Azan choice for the 4 non-Fajr prayers (Fajr is ALWAYS azan_fajr, Rule 14). */
enum class AzanOption { DEFAULT, MAKKAH }

/** All pref key names — exactly as listed in DATA_SCHEMA.md. */
object PrefKeys {
    // Location
    const val SAVED_LATITUDE  = "saved_latitude"     // Float
    const val SAVED_LONGITUDE = "saved_longitude"    // Float
    const val SAVED_CITY_NAME = "saved_city_name"    // String
    const val LOCATION_MODE   = "location_mode"      // "GPS" | "MANUAL"

    // Prayer times cache metadata
    const val LAST_MONTH_FETCHED = "last_month_fetched"  // "2026-09" (ISO year-month)

    // Splash rotation (PHASE_9)
    const val SPLASH_CARD_INDEX = "splash_card_index"    // Int 0–4

    // Onboarding (PHASE_9)
    const val ONBOARDING_COMPLETE = "onboarding_complete" // Boolean

    // Language (PHASE_8)
    const val APP_LANGUAGE = "app_language"              // "en" | "ur"

    // Azan selection (PHASE_3) — "default" | "fajr" | "makkah"
    const val SELECTED_AZAN = "selected_azan"

    // Active alarm registry (PHASE-3.1) — what AlarmManager currently holds
    const val ACTIVE_ALARMS = "active_alarms"            // Set<String>

    // Alarm toggles (PHASE_3) — one per prayer, default true
    const val ALARM_FAJR_ENABLED    = "alarm_fajr_enabled"
    const val ALARM_DHUHR_ENABLED   = "alarm_dhuhr_enabled"
    const val ALARM_ASR_ENABLED     = "alarm_asr_enabled"
    const val ALARM_MAGHRIB_ENABLED = "alarm_maghrib_enabled"
    const val ALARM_ISHA_ENABLED    = "alarm_isha_enabled"

    // Daily Ayah cache (PHASE_6)
    const val DAILY_AYAH_TEXT_AR    = "daily_ayah_text_ar"
    const val DAILY_AYAH_TEXT_EN    = "daily_ayah_text_en"
    const val DAILY_AYAH_TEXT_UR    = "daily_ayah_text_ur"
    const val DAILY_AYAH_REFERENCE  = "daily_ayah_reference"
    const val DAILY_AYAH_FETCH_DATE = "daily_ayah_fetch_date"

    // Widget promo card (PHASE-5.1) — user tapped "Not now" (or added via pin)
    const val WIDGET_PROMO_DISMISSED = "widget_promo_dismissed" // Boolean

    // Quran (PHASE_6) — comma list of surah numbers where bismillah_pre == false
    const val SURAH_NO_BISMILLAH = "surah_no_bismillah" // String e.g. "1,9"

    // Quran reader (PHASE-6.1) — mushaf card redesign
    const val READER_FONT_SCALE = "reader_font_scale" // Float multiplier 0.8–1.5
    const val READER_COACH_DONE = "reader_coach_done" // Boolean — PHASE-6.4 coach marks seen
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mawaqit_prefs")

/**
 * Thin wrapper over Jetpack DataStore Preferences (mawaqit_prefs).
 * suspend/Flow APIs only — synchronous access happens inside background
 * workers via first(), never on the main thread (DATA_SCHEMA.md).
 */
@Singleton
class PrefsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val store get() = context.dataStore

    // ── Location ────────────────────────────────────────────────────────────
    val savedLatitude: Flow<Float?> =
        store.data.map { it[floatPreferencesKey(PrefKeys.SAVED_LATITUDE)] }
    val savedLongitude: Flow<Float?> =
        store.data.map { it[floatPreferencesKey(PrefKeys.SAVED_LONGITUDE)] }
    val savedCityName: Flow<String?> =
        store.data.map { it[stringPreferencesKey(PrefKeys.SAVED_CITY_NAME)] }

    suspend fun saveLocation(latitude: Double, longitude: Double, cityName: String?) {
        store.edit {
            it[floatPreferencesKey(PrefKeys.SAVED_LATITUDE)] = latitude.toFloat()
            it[floatPreferencesKey(PrefKeys.SAVED_LONGITUDE)] = longitude.toFloat()
            if (cityName != null) {
                it[stringPreferencesKey(PrefKeys.SAVED_CITY_NAME)] = cityName
            }
        }
    }

    /** One-shot read used when the app starts and needs coords immediately. */
    suspend fun getLocationOnce(): Pair<Double, Double>? {
        val prefs = store.data.first()
        val lat = prefs[floatPreferencesKey(PrefKeys.SAVED_LATITUDE)]
        val lon = prefs[floatPreferencesKey(PrefKeys.SAVED_LONGITUDE)]
        return if (lat != null && lon != null) lat.toDouble() to lon.toDouble() else null
    }

    // ── Monthly fetch metadata ──────────────────────────────────────────────
    val lastMonthFetched: Flow<String?> =
        store.data.map { it[stringPreferencesKey(PrefKeys.LAST_MONTH_FETCHED)] }

    suspend fun setLastMonthFetched(monthIso: String) {   // "2026-09"
        store.edit { it[stringPreferencesKey(PrefKeys.LAST_MONTH_FETCHED)] = monthIso }
    }

    suspend fun getLastMonthFetchedOnce(): String? =
        store.data.first()[stringPreferencesKey(PrefKeys.LAST_MONTH_FETCHED)]

    // ── Alarm toggles (PHASE_3) ─────────────────────────────────────────────

    private fun alarmKey(prayer: PrayerName) = booleanPreferencesKey(
        when (prayer) {
            PrayerName.FAJR -> PrefKeys.ALARM_FAJR_ENABLED
            PrayerName.DHUHR -> PrefKeys.ALARM_DHUHR_ENABLED
            PrayerName.ASR -> PrefKeys.ALARM_ASR_ENABLED
            PrayerName.MAGHRIB -> PrefKeys.ALARM_MAGHRIB_ENABLED
            PrayerName.ISHA -> PrefKeys.ALARM_ISHA_ENABLED
        }
    )

    /** One-shot read for the scheduler (AlarmWorker/AlarmScheduler — never main thread). */
    suspend fun getAlarmEnabledOnce(prayer: PrayerName): Boolean =
        store.data.first()[alarmKey(prayer)] ?: true // default ON (DATA_SCHEMA.md)

    /** Live flow for the UI switches. */
    fun alarmEnabledFlow(prayer: PrayerName): Flow<Boolean> =
        store.data.map { it[alarmKey(prayer)] ?: true }

    suspend fun setAlarmEnabled(prayer: PrayerName, enabled: Boolean) {
        store.edit { it[alarmKey(prayer)] = enabled }
    }

    // ── Azan selection (PHASE_3 scheduling; UI picker arrives in PHASE_8) ──

    val selectedAzan: Flow<AzanOption> =
        store.data.map { prefs ->
            when (prefs[stringPreferencesKey(PrefKeys.SELECTED_AZAN)]) {
                "makkah" -> AzanOption.MAKKAH
                else -> AzanOption.DEFAULT
            }
        }

    suspend fun getSelectedAzanOnce(): AzanOption =
        when (store.data.first()[stringPreferencesKey(PrefKeys.SELECTED_AZAN)]) {
            "makkah" -> AzanOption.MAKKAH
            else -> AzanOption.DEFAULT
        }

    suspend fun setSelectedAzan(option: AzanOption) {
        store.edit {
            it[stringPreferencesKey(PrefKeys.SELECTED_AZAN)] =
                if (option == AzanOption.MAKKAH) "makkah" else "default"
        }
    }

    // ── Daily Ayah cache (PHASE_4; UmmahAPI source arrives in PHASE_6) ─────

    /** (ayah, fetchDate ISO) or null when nothing cached yet. */
    suspend fun getDailyAyahOnce(): Pair<DailyAyah, String>? {
        val p = store.data.first()
        val arabic = p[stringPreferencesKey(PrefKeys.DAILY_AYAH_TEXT_AR)] ?: return null
        val english = p[stringPreferencesKey(PrefKeys.DAILY_AYAH_TEXT_EN)] ?: return null
        val reference = p[stringPreferencesKey(PrefKeys.DAILY_AYAH_REFERENCE)] ?: return null
        val date = p[stringPreferencesKey(PrefKeys.DAILY_AYAH_FETCH_DATE)] ?: return null
        val urdu = p[stringPreferencesKey(PrefKeys.DAILY_AYAH_TEXT_UR)] ?: ""
        return DailyAyah(arabic, english, urdu, reference) to date
    }

    suspend fun setDailyAyah(ayah: DailyAyah, dateIso: String) {
        store.edit {
            it[stringPreferencesKey(PrefKeys.DAILY_AYAH_TEXT_AR)] = ayah.arabic
            it[stringPreferencesKey(PrefKeys.DAILY_AYAH_TEXT_EN)] = ayah.english
            it[stringPreferencesKey(PrefKeys.DAILY_AYAH_TEXT_UR)] = ayah.urdu
            it[stringPreferencesKey(PrefKeys.DAILY_AYAH_REFERENCE)] = ayah.reference
            it[stringPreferencesKey(PrefKeys.DAILY_AYAH_FETCH_DATE)] = dateIso
        }
    }

    // ── Active alarm registry (PHASE-3.1) ──────────────────────────────────

    /**
     * What AlarmManager currently holds for us, one entry per armed alarm:
     * "PRAYER|dateIso|timeMillis|azanType" (see PlannedAlarm.toRegistryEntry).
     * AlarmManager cannot be enumerated, so this registry is how
     * AlarmScheduler.cancelAllAlarms() knows which slots to clear.
     * Empty set == nothing armed.
     */
    suspend fun getActiveAlarmsOnce(): Set<String> =
        store.data.first()[stringSetPreferencesKey(PrefKeys.ACTIVE_ALARMS)] ?: emptySet()

    suspend fun setActiveAlarms(entries: Set<String>) {
        store.edit { it[stringSetPreferencesKey(PrefKeys.ACTIVE_ALARMS)] = entries }
    }

    // ── Widget promo card (PHASE-5.1) ───────────────────────────────────────

    val widgetPromoDismissed: Flow<Boolean> =
        store.data.map { it[booleanPreferencesKey(PrefKeys.WIDGET_PROMO_DISMISSED)] ?: false }

    suspend fun setWidgetPromoDismissed(dismissed: Boolean) {
        store.edit { it[booleanPreferencesKey(PrefKeys.WIDGET_PROMO_DISMISSED)] = dismissed }
    }

    // ── Quran: bismillah_pre flags (PHASE_6) ────────────────────────────────

    /** Surah numbers where the Bismillah header must NOT be shown (1, 9). */
    suspend fun getSurahsWithoutBismillahOnce(): Set<Int> =
        store.data.first()[stringPreferencesKey(PrefKeys.SURAH_NO_BISMILLAH)]
            ?.split(",")
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?.toSet()
            ?: emptySet()

    suspend fun setSurahsWithoutBismillah(numbers: Set<Int>) {
        store.edit {
            it[stringPreferencesKey(PrefKeys.SURAH_NO_BISMILLAH)] = numbers.sorted().joinToString(",")
        }
    }

    // ── Quran reader (PHASE-6.1) ──────────────────────────────────────────

    /** Arabic text scale multiplier — S 0.85 · M 1.0 · L 1.15 · XL 1.3. */
    val readerFontScale: Flow<Float> =
        store.data.map { it[floatPreferencesKey(PrefKeys.READER_FONT_SCALE)] ?: 1.0f }

    suspend fun setReaderFontScale(scale: Float) {
        store.edit { it[floatPreferencesKey(PrefKeys.READER_FONT_SCALE)] = scale }
    }

    /** PHASE-6.4 coach marks (swipe + mark-as-read) — shown once, then forever quiet. */
    val readerCoachDone: Flow<Boolean> =
        store.data.map { it[booleanPreferencesKey(PrefKeys.READER_COACH_DONE)] ?: false }

    suspend fun setReaderCoachDone() {
        store.edit { it[booleanPreferencesKey(PrefKeys.READER_COACH_DONE)] = true }
    }
}
