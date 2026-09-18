package com.mawaqit.app.alarm

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mawaqit.app.data.db.MawaqitDatabase
import com.mawaqit.app.data.db.SalahLogEntity
import com.mawaqit.app.data.model.PrayerName
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate

/**
 * Writes a salah_log row (prayed = true) when the user taps "Mark as Prayed"
 * on the azan notification (PHASE_3_ALARMS.md / DATA_SCHEMA.md Table 2).
 * Enqueued by AzanReceiver so the DB write never happens inside onReceive().
 */
@HiltWorker
class MarkPrayedWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val db: MawaqitDatabase
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val prayerName = inputData.getString(KEY_PRAYER)
            ?: return Result.failure()
        if (PrayerName.entries.none { it.name == prayerName }) return Result.failure()

        db.salahLogDao().upsert(
            SalahLogEntity(
                date = LocalDate.now().toString(), // ISO (DATA_SCHEMA.md)
                prayer = prayerName,
                prayed = true,
                markedAt = System.currentTimeMillis()
            )
        )
        return Result.success()
    }

    companion object {
        const val KEY_PRAYER = "key_prayer"
    }
}
