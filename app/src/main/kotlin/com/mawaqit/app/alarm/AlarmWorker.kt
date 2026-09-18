package com.mawaqit.app.alarm

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Rebuilds and applies the alarm plan from the OFFLINE Room cache
 * (PHASE_3_ALARMS.md: background events must never make an API call).
 *
 * Enqueued by BootReceiver for four triggers (PHASE-3.1): boot completed,
 * clock changed, timezone changed, app updated. All follow the same path:
 * AlarmRefreshManager.refreshAlarmsFromCache() → AlarmScheduler.applyAlarmPlan.
 *
 * Empty cache (fresh install, never opened) is not an error: logs, succeeds,
 * and alarms start on the next app open.
 */
@HiltWorker
class AlarmWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val refreshManager: AlarmRefreshManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val count = refreshManager.refreshAlarmsFromCache()
        Log.i(TAG, "Alarm reschedule complete — $count alarm(s) armed")
        return Result.success()
    }

    companion object {
        private const val TAG = "Mawaqit"
    }
}
