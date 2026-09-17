package com.mawaqit.app.data.repository

import com.mawaqit.app.data.model.NextPrayer
import com.mawaqit.app.data.model.PrayerTimings
import kotlinx.coroutines.flow.Flow

/**
 * Offline-first prayer times (RULES.md Rule 8): Room DB is the source of truth
 * for the UI; the AlAdhan API only fills it, once per month (PHASE_2).
 */
interface PrayerRepository {

    /**
     * Saves new coordinates and invalidates the cache (location change = refresh
     * trigger per DATA_SCHEMA.md).
     */
    suspend fun setLocation(latitude: Double, longitude: Double, cityName: String?)

    /**
     * Ensures the CURRENT month's data exists:
     *  - already fetched this month (pref) and rows exist → no-op, returns true
     *  - else fetches the AlAdhan monthly calendar and caches all days
     *  - on network failure returns false (caller serves whatever DB has)
     */
    suspend fun refreshIfNeeded(): Boolean

    /** Today's times from the DB; falls back to the latest cached day (offline). */
    suspend fun getTodayPrayerTimes(): PrayerTimings?

    /** Next upcoming prayer (today's remaining, else tomorrow's Fajr). */
    suspend fun getNextPrayer(today: PrayerTimings): NextPrayer?

    /** Saved city label for display (null until user sets a location). */
    fun cityName(): Flow<String?>

    /** True when saved coordinates exist (app has a usable location). */
    suspend fun hasSavedLocation(): Boolean
}
