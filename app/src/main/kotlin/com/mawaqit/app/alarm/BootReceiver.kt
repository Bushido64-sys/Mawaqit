package com.mawaqit.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

/**
 * Re-arms prayer alarms after system events (PHASE_3_ALARMS.md, PHASE-3.1
 * closes GAP-2).
 *
 * AlarmManager alarms are RAM-only — they do NOT survive reboot. They also go
 * stale when the user changes the clock or timezone, and after an app update
 * (PendingIntents survive updates, but the armed plan may no longer match
 * reality). All four triggers funnel into ONE fix: hand off to AlarmWorker
 * via WorkManager, which rebuilds the 7-day plan from the offline Room cache.
 *
 * This receiver does no DB work itself (broadcast window is ~10s and the phone
 * may be in direct-boot/locked state): enqueue-only. All heavy lifting happens
 * in AlarmWorker with Hilt-injected dependencies, off the main thread.
 *
 * LOCKED_BOOT_COMPLETED is deliberately ignored here (see PERMISSIONS.md note):
 * our Room DB and DataStore live in credential-encrypted storage, which is not
 * readable before the user unlocks the phone.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                WorkManager.getInstance(context).enqueue(
                    OneTimeWorkRequestBuilder<AlarmWorker>().build()
                )
                Log.i("Mawaqit", "${intent.action} — alarm reschedule enqueued")
            }
            Intent.ACTION_LOCKED_BOOT_COMPLETED -> {
                Log.i("Mawaqit", "Locked boot ignored — waiting for BOOT_COMPLETED (unlocked)")
            }
        }
    }
}
