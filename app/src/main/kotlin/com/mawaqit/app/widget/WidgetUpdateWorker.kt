package com.mawaqit.app.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mawaqit.app.data.repository.PrayerRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * PHASE_5: keeps the widget honest with three independent refresh nets
 * (one failing never leaves a stale prayer on the home screen):
 *
 * 1. 15-min WorkManager heartbeat (this worker, periodic) — PHASE_5_WIDGET.md.
 * 2. A one-shot scheduled ~1s AFTER the next prayer's wall time, re-armed by
 *    every heartbeat run → the widget flips to the new prayer the moment it
 *    rings, not up to 15 min later. WorkManager persists both across reboots.
 * 3. The system's own updatePeriodMillis=30 min (mawaqit_widget_info.xml).
 *
 * Reads Room directly at render time (patched doc: no DataStore round-trip —
 * one source of truth). A dropped run (phone off) catches up next period —
 * housekeeping, not a deadline.
 */
@HiltWorker
class WidgetUpdateWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: PrayerRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        // Side effect of every run: keep the prayer-pass one-shot aligned with
        // the latest DB state (this is also how it re-arms after reboots).
        schedulePrayerPassOneShot(repository)
        return try {
            MawaqitWidget().updateAll(appContext)
            Log.i(TAG, "Widget refreshed")
            Result.success()
        } catch (e: Exception) {
            Log.w(TAG, "Widget refresh failed", e)
            Result.retry()
        }
    }

    /**
     * One-shot ≈1s AFTER the next prayer's wall time passes → the redraw shows
     * the NEW next prayer. No-op when there's no data yet (fresh install) —
     * the heartbeat re-arms it once data exists.
     */
    private suspend fun schedulePrayerPassOneShot(repository: PrayerRepository) {
        val next = try {
            repository.getTodayPrayerTimes()?.let { repository.getNextPrayer(it) }
        } catch (e: Exception) {
            null
        } ?: return

        val delayMs = (next.timeMillis + 1_000L) - System.currentTimeMillis()
        if (delayMs <= 0) return
        WorkManager.getInstance(appContext).enqueueUniqueWork(
            ONE_SHOT_NAME,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<WidgetUpdateWorker>()
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .build()
        )
    }

    companion object {
        private const val TAG = "Mawaqit"
        private const val PERIODIC_NAME = "mawaqit_widget_update"
        private const val ONE_SHOT_NAME = "mawaqit_widget_prayer_pass"

        /**
         * App-open refresh (HomeViewModel.loadTimes). Enqueued as a REPLACE
         * one-shot so back-to-back app opens don't stack workers, and it
         * doubles as a prayer-pass one-shot re-arm via doWork's side effect.
         */
        fun refreshNow(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                ONE_SHOT_NAME,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<WidgetUpdateWorker>().build()
            )
        }

        /** Registers the 15-min heartbeat once; idempotent (KEEP). */
        fun ensureScheduled(context: Context) {
            val request = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder().setRequiresBatteryNotLow(true).build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
