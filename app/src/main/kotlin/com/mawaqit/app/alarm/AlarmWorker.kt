package com.mawaqit.app.alarm

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mawaqit.app.data.db.MawaqitDatabase
import com.mawaqit.app.data.model.PrayerTimings
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate

/**
 * Re-schedules today's enabled prayer alarms from the OFFLINE Room cache
 * (PHASE_3_ALARMS.md: BootReceiver must never make an API call).
 *
 * Runs via WorkManager after BOOT_COMPLETED. If the DB has no row for today
 * (fresh install + reboot before first app open), it logs and succeeds — the
 * alarms get set the next time the app is opened and HomeViewModel schedules.
 */
@HiltWorker
class AlarmWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val db: MawaqitDatabase,
    private val scheduler: AlarmScheduler
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val todayIso = LocalDate.now().toString() // ISO, per DATA_SCHEMA.md
        val row = db.prayerTimeDao().getPrayerTimeByDate(todayIso)

        if (row == null) {
            Log.w(TAG, "No cached prayer times for $todayIso — alarms resume on next app open")
            return Result.success()
        }

        scheduler.scheduleAllPrayerAlarms(
            PrayerTimings(
                date = row.date,
                fajr = row.fajr,
                dhuhr = row.dhuhr,
                asr = row.asr,
                maghrib = row.maghrib,
                isha = row.isha,
                latitude = row.latitude,
                longitude = row.longitude
            )
        )
        Log.i(TAG, "Alarms rescheduled from DB after boot ($todayIso)")
        return Result.success()
    }

    companion object {
        private const val TAG = "Mawaqit"
    }
}
