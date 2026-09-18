package com.mawaqit.app.alarm

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Daily alarm housekeeping (PHASE-3.1, closes GAP-1).
 *
 * Once every ~24h it re-applies the 7-day alarm plan from the offline cache.
 * This is what lets the app sit untouched for DAYS and still ring Fajr on
 * day 7 — the housekeeping job rolls the alarm window forward every night,
 * no app open, no internet.
 *
 * WorkManager periodic jobs survive reboots and Doze; battery-not-low keeps
 * it polite. If a run is missed (device off), the next run catches up, and
 * BOOT_COMPLETED's AlarmWorker covers the restart case anyway.
 */
@HiltWorker
class DailyAlarmWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val refreshManager: AlarmRefreshManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val count = refreshManager.refreshAlarmsFromCache()
        Log.i(TAG, "Daily alarm housekeeping done — $count alarm(s) armed")
        return Result.success()
    }

    companion object {
        private const val TAG = "Mawaqit"
        private const val UNIQUE_WORK_NAME = "mawaqit_daily_alarm_housekeeping"

        /**
         * Registers the job if missing, leaves it untouched if present (KEEP).
         * Called from Application.onCreate on every process start — cheap,
         * idempotent, and a safety net after reboots (though periodic work is
         * persisted by WorkManager anyway).
         */
        fun ensureScheduled(context: Context) {
            val request = PeriodicWorkRequestBuilder<DailyAlarmWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(1, TimeUnit.HOURS) // app open already arms alarms; no rush
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
