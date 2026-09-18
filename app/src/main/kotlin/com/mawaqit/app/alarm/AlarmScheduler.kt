package com.mawaqit.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.mawaqit.app.MainActivity
import com.mawaqit.app.data.model.PrayerName
import com.mawaqit.app.data.prefs.AzanOption
import com.mawaqit.app.data.prefs.PrefsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sets/cancels prayer alarms with the system AlarmManager (PHASE_3_ALARMS.md).
 *
 * PHASE-3.1 model: the scheduler is a DUMB EXECUTOR. Callers build an explicit
 * alarm plan (see AlarmRefreshManager) and hand it to [applyAlarmPlan], which
 * cancels everything previously armed and sets exactly the planned alarms.
 * The old one-slot-per-prayer model could only hold TODAY — scheduling the
 * next day overwrote the last one.
 *
 * Reliability notes:
 *  - setAlarmClock() when exact alarms are permitted — most reliable API
 *    (DND-exempt, user-set-alarm semantics, grants FGS-from-background).
 *  - Without SCHEDULE_EXACT_ALARM (API 31/32 before the user flips the
 *    Settings switch), degrade to setWindow() with a 10-minute window —
 *    imperfect but never silently dead. PERMISSIONS.md decision tree.
 *  - One PendingIntent slot PER (prayer, date): requestCode =
 *    prayer.ordinal * 100_000_000 + yyyymmdd — deterministic and collision-
 *    free, so re-applying a plan replaces in place and cancel is precise.
 *  - The ACTIVE_ALARMS pref registry remembers what we armed (AlarmManager
 *    cannot be enumerated) so [cancelAllAlarms] clears every stale slot.
 */
@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PrefsRepository
) {
    private val alarmManager
        get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /** True when the system lets us schedule exact alarms (API 31+ check). */
    fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    /**
     * Cancel every alarm this app previously armed, then set exactly [plan].
     * Idempotent — safe to call any number of times (app open, boot, clock
     * change, daily housekeeping job, toggle change...).
     */
    suspend fun applyAlarmPlan(plan: List<PlannedAlarm>) {
        cancelAllAlarms()
        plan.forEach { arm(it) }
        prefs.setActiveAlarms(plan.map { it.toRegistryEntry() }.toSet())
        Log.i(TAG, "Alarm plan applied — ${plan.size} alarm(s) armed")
    }

    /** Cancel every registered alarm (no-op when nothing is armed). */
    suspend fun cancelAllAlarms() {
        prefs.getActiveAlarmsOnce().forEach { entry ->
            pendingIntentFromEntry(entry)?.let { alarmManager.cancel(it) }
        }
        prefs.setActiveAlarms(emptySet())
    }

    /** Fajr is always the special azan (Rule 14); others follow the user's choice. */
    suspend fun azanTypeFor(prayer: PrayerName): AzanType = when {
        prayer == PrayerName.FAJR -> AzanType.FAJR
        prefs.getSelectedAzanOnce() == AzanOption.MAKKAH -> AzanType.MAKKAH
        else -> AzanType.DEFAULT
    }

    /**
     * Arm one planned alarm. Slot identity comes from [PlannedAlarm.dateIso] —
     * the SAME source the registry entry was built from, so a later cancel
     * always rebuilds a matching requestCode (a DST-shifted wall time must
     * never change the slot identity between arm and cancel).
     */
    private fun arm(scheduled: PlannedAlarm) {
        if (scheduled.timeMillis <= System.currentTimeMillis()) return // plan entry aged out mid-flight
        val pendingIntent = pendingIntentFor(scheduled)
        alarmManager.cancel(pendingIntent) // never double-schedule the same slot
        if (canScheduleExactAlarms()) {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(scheduled.timeMillis, openAppPendingIntent()),
                pendingIntent
            )
            Log.i(TAG, "Exact alarm set: ${scheduled.prayer} at ${scheduled.timeMillis} (${scheduled.azanType})")
        } else {
            alarmManager.setWindow(
                AlarmManager.RTC_WAKEUP,
                scheduled.timeMillis,
                WINDOW_MS,
                pendingIntent
            )
            Log.w(TAG, "Exact-alarm permission missing — ${scheduled.prayer} uses a ${WINDOW_MS / 60000}min window")
        }
    }

    // ── PendingIntent plumbing ───────────────────────────────────────────────

    /**
     * requestCode = prayer.ordinal * 100_000_000 + yyyymmdd.
     * Unique per (prayer, date) with zero collision risk. Extras are NOT part
     * of PendingIntent matching — FLAG_UPDATE_CURRENT swaps them safely, which
     * is how a re-applied plan updates an existing slot in place.
     */
    private fun requestCodeFor(prayer: PrayerName, dateIso: String): Int =
        prayer.ordinal * 100_000_000 + dateIso.replace("-", "").toInt()

    private fun prayerAlarmIntent(prayer: PrayerName, azanType: AzanType): Intent =
        Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_PRAYER_ALARM
            putExtra(AlarmReceiver.EXTRA_PRAYER_NAME, prayer.name)
            putExtra(AlarmReceiver.EXTRA_AZAN_TYPE, azanType.name)
        }

    private fun pendingIntentFor(scheduled: PlannedAlarm): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCodeFor(scheduled.prayer, scheduled.dateIso),
            prayerAlarmIntent(scheduled.prayer, scheduled.azanType),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    /**
     * Rebuild a MATCHING PendingIntent from a registry entry (for cancel()).
     * Matching compares requestCode + intent identity only — the azan extra
     * value is irrelevant here. Malformed entries are skipped, never crash.
     */
    private fun pendingIntentFromEntry(entry: String): PendingIntent? {
        val parts = entry.split(PlannedAlarm.REGISTRY_SEPARATOR)
        val prayer = PrayerName.entries.firstOrNull { it.name == parts.getOrNull(0) }
            ?: return null
        val dateIso = parts.getOrNull(1) ?: return null
        return try {
            PendingIntent.getBroadcast(
                context,
                requestCodeFor(prayer, dateIso),
                prayerAlarmIntent(prayer, AzanType.DEFAULT),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } catch (e: Exception) {
            Log.w(TAG, "Could not rebuild PendingIntent for registry entry '$entry'", e)
            null
        }
    }

    /** Tap target for the system alarm-clock icon (shows the app when tapped). */
    private fun openAppPendingIntent(): PendingIntent =
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    companion object {
        private const val TAG = "Mawaqit"
        private const val WINDOW_MS = 10 * 60_000L // 10 minutes, inexact fallback only
    }
}
