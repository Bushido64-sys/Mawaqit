package com.mawaqit.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

/**
 * Re-arms prayer alarms after a phone restart (PHASE_3_ALARMS.md).
 *
 * Alarms do NOT survive reboot — AlarmManager is RAM-only. This receiver does
 * no DB work itself (the broadcast window is ~10s and the app may be in
 * direct-boot/locked state): it just hands off to AlarmWorker via WorkManager,
 * which runs the rescheduling with Hilt-injected dependencies, off the main
 * thread, whenever the system gets around to it.
 *
 * LOCKED_BOOT_COMPLETED is deliberately ignored here (see PERMISSIONS.md note):
 * our Room DB and DataStore live in credential-encrypted storage, which is not
 * readable before the user unlocks the phone.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                WorkManager.getInstance(context).enqueue(
                    OneTimeWorkRequestBuilder<AlarmWorker>().build()
                )
                Log.i("Mawaqit", "Boot completed — alarm reschedule enqueued")
            }
            Intent.ACTION_LOCKED_BOOT_COMPLETED -> {
                Log.i("Mawaqit", "Locked boot ignored — waiting for BOOT_COMPLETED (unlocked)")
            }
        }
    }
}
