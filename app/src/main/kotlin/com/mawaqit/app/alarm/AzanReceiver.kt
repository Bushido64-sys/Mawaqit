package com.mawaqit.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

/**
 * Handles the "Mark as Prayed" action on the azan notification
 * (PHASE_3_ALARMS.md — the notification action sends a broadcast here).
 *
 * Two jobs, both tiny and main-thread-safe:
 *  1. Stop AzanService (kills audio + notification immediately).
 *  2. Enqueue MarkPrayedWorker to write salah_log in the background —
 *     DB writes are suspend calls and must NOT run inside onReceive.
 */
class AzanReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_MARK_PRAYED) return
        val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: return

        context.stopService(Intent(context, AzanService::class.java))

        WorkManager.getInstance(context).enqueue(
            OneTimeWorkRequestBuilder<MarkPrayedWorker>()
                .setInputData(workDataOf(MarkPrayedWorker.KEY_PRAYER to prayerName))
                .build()
        )
    }

    companion object {
        const val ACTION_MARK_PRAYED = "com.mawaqit.app.ACTION_MARK_PRAYED"
        const val EXTRA_PRAYER_NAME = AlarmReceiver.EXTRA_PRAYER_NAME
    }
}
