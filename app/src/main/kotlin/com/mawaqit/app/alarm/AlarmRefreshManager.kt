package com.mawaqit.app.alarm

import android.util.Log
import com.mawaqit.app.data.db.MawaqitDatabase
import com.mawaqit.app.data.model.PrayerName
import com.mawaqit.app.data.prefs.PrefsRepository
import com.mawaqit.app.util.parseTimeToMillis
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The single "re-think all alarms" entry point (PHASE-3.1, closes GAP-1).
 *
 * Builds a plan for the next [DAYS_AHEAD] days from the OFFLINE Room cache —
 * never the network — and hands it to AlarmScheduler. Called from:
 *   - HomeViewModel (every app open)
 *   - AlarmWorker (boot / clock change / timezone change / app update)
 *   - DailyAlarmWorker (the 24h housekeeping job that keeps future days armed
 *     even if the user never opens the app)
 *
 * Why this fixes GAP-1: the month of prayer times is ALREADY cached locally
 * (PrayerRepository refreshes it monthly), so scheduling 7 days ahead costs
 * nothing extra and works fully offline. Skipping a day of app opens no longer
 * silences tomorrow's Fajr.
 */
@Singleton
class AlarmRefreshManager @Inject constructor(
    private val db: MawaqitDatabase,
    private val prefs: PrefsRepository,
    private val scheduler: AlarmScheduler
) {

    /**
     * Rebuild + apply the alarm plan from the cache. Returns how many alarms
     * were armed. Idempotent and side-effect free beyond AlarmManager itself:
     * cancels everything previously set (including disabled prayers' stale
     * alarms from earlier plans), then arms exactly the plan.
     */
    suspend fun refreshAlarmsFromCache(): Int {
        val today = LocalDate.now()
        val end = today.plusDays(DAYS_AHEAD - 1L)
        val rows = db.prayerTimeDao()
            .getPrayerTimesBetween(today.toString(), end.toString())

        if (rows.isEmpty()) {
            // Fresh install before first app open, or cache wiped. Not an error:
            // alarms start on the next successful fetch + app open.
            Log.w(TAG, "No cached prayer times ${today}..$end — alarm plan empty")
        }

        // One prefs snapshot pass — avoids ~35+ datastore reads in the loop below.
        val enabledByPrayer = PrayerName.entries.associateWith { prefs.getAlarmEnabledOnce(it) }
        val now = System.currentTimeMillis()

        val plan = rows.flatMap { row ->
            val date = LocalDate.parse(row.date) // ISO per DATA_SCHEMA.md
            listOf(
                PrayerName.FAJR to row.fajr,
                PrayerName.DHUHR to row.dhuhr,
                PrayerName.ASR to row.asr,
                PrayerName.MAGHRIB to row.maghrib,
                PrayerName.ISHA to row.isha
            ).mapNotNull { (prayer, timeStr) ->
                if (enabledByPrayer[prayer] != true) return@mapNotNull null
                val millis = parseTimeToMillis(timeStr, date)
                if (millis <= now) return@mapNotNull null // today's passed prayers
                PlannedAlarm(
                    prayer = prayer,
                    timeMillis = millis,
                    dateIso = row.date,
                    azanType = scheduler.azanTypeFor(prayer)
                )
            }
        }

        scheduler.applyAlarmPlan(plan)
        return plan.size
    }

    /**
     * PHASE-3.2 diagnostics: fire the full azan chain (notification + audio)
     * in ~[delaySeconds] seconds, bypassing the plan. No salah_log row.
     */
    fun fireTestAlarm(delaySeconds: Long = 10L) = scheduler.scheduleTestAlarm(delaySeconds)

    companion object {
        private const val TAG = "Mawaqit"

        /**
         * How many days of alarms live in AlarmManager at once. 7 is plenty:
         * the monthly cache usually covers it, and the daily housekeeping job
         * rolls the window forward every 24h. Deliberately NOT the whole month —
         * 35 live alarms waste system resources and complicate debugging.
         */
        const val DAYS_AHEAD = 7
    }
}
