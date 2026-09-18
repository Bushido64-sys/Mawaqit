package com.mawaqit.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.mawaqit.app.MainActivity
import com.mawaqit.app.data.model.PrayerName
import com.mawaqit.app.data.model.PrayerTimings
import com.mawaqit.app.data.prefs.AzanOption
import com.mawaqit.app.data.prefs.PrefsRepository
import com.mawaqit.app.util.parseTimeToMillis
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sets/cancels the 5 prayer alarms with the system AlarmManager (PHASE_3_ALARMS.md).
 *
 * Reliability notes:
 *  - setAlarmClock() is used when exact alarms are permitted — it is the most
 *    reliable API (DND-exempt, treated like a user-set alarm clock) and grants
 *    the app the right to start a foreground service from the background.
 *  - If SCHEDULE_EXACT_ALARM is not granted (API 31/32 before the user flips the
 *    Settings switch), we degrade to setWindow() with a 10-minute window —
 *    imperfect but never silently dead. PERMISSIONS.md decision tree.
 *  - PendingIntent per prayer uses prayer.ordinal as requestCode so
 *    cancel/update are per-prayer precise.
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
     * Schedules every enabled prayer alarm from ONE day's timings, skipping
     * times that already passed (PHASE_3_ALARMS.md scheduling logic).
     */
    suspend fun scheduleAllPrayerAlarms(timings: PrayerTimings) {
        val date = LocalDate.parse(timings.date) // ISO
        listOf(
            PrayerName.FAJR to timings.fajr,
            PrayerName.DHUHR to timings.dhuhr,
            PrayerName.ASR to timings.asr,
            PrayerName.MAGHRIB to timings.maghrib,
            PrayerName.ISHA to timings.isha
        ).forEach { (prayer, timeStr) ->
            val millis = parseTimeToMillis(timeStr, date)
            if (!prefs.getAlarmEnabledOnce(prayer)) {
                cancelAlarm(prayer)
            } else if (millis > System.currentTimeMillis()) {
                scheduleSingleAlarm(prayer, millis)
            }
        }
    }

    suspend fun scheduleSingleAlarm(prayer: PrayerName, timeMillis: Long) {
        if (timeMillis <= System.currentTimeMillis()) return // already passed today
        scheduleSingleAlarm(prayer, timeMillis, azanTypeFor(prayer))
    }

    fun cancelAlarm(prayer: PrayerName) {
        // AzanType value in the extra is irrelevant for matching — the
        // requestCode + Intent identity is what AlarmManager compares.
        alarmManager.cancel(pendingIntent(prayer, AzanType.DEFAULT))
    }

    fun cancelAllAlarms() {
        PrayerName.entries.forEach { cancelAlarm(it) }
    }

    /** Fajr is always the special azan (Rule 14); others follow the user's choice. */
    suspend fun azanTypeFor(prayer: PrayerName): AzanType = when {
        prayer == PrayerName.FAJR -> AzanType.FAJR
        prefs.getSelectedAzanOnce() == AzanOption.MAKKAH -> AzanType.MAKKAH
        else -> AzanType.DEFAULT
    }

    private fun scheduleSingleAlarm(prayer: PrayerName, timeMillis: Long, azanType: AzanType) {
        val pendingIntent = pendingIntent(prayer, azanType)
        alarmManager.cancel(pendingIntent) // never double-schedule the same prayer
        if (canScheduleExactAlarms()) {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(timeMillis, openAppPendingIntent()),
                pendingIntent
            )
            Log.i(TAG, "Exact alarm set: $prayer at $timeMillis ($azanType)")
        } else {
            alarmManager.setWindow(AlarmManager.RTC_WAKEUP, timeMillis, WINDOW_MS, pendingIntent)
            Log.w(TAG, "Exact-alarm permission missing — $prayer uses a ${WINDOW_MS / 60000}min window")
        }
    }

    private fun pendingIntent(prayer: PrayerName, azanType: AzanType): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_PRAYER_ALARM
            putExtra(AlarmReceiver.EXTRA_PRAYER_NAME, prayer.name)
            putExtra(AlarmReceiver.EXTRA_AZAN_TYPE, azanType.name)
        }
        return PendingIntent.getBroadcast(
            context,
            prayer.ordinal, // unique per prayer — FLAG_UPDATE_CURRENT updates, not duplicates
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
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
