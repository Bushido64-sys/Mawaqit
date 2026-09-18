package com.mawaqit.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log

/**
 * Fires when AlarmManager triggers a prayer alarm (PHASE_3_ALARMS.md).
 *
 * Runs on the MAIN thread inside a short broadcast window — so it does the
 * minimum: grab a wake lock (covers AzanService's slow start), hand the prayer
 * name over, start the foreground service. All real work (prefs, audio,
 * notification) lives in AzanService.
 *
 * DI note: plain receivers are not Hilt entry points — this one needs no
 * dependencies, which is exactly why the work was pushed into the service.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_PRAYER_ALARM) return
        val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: return

        // Keep the CPU awake across receiver→service handoff (PERMISSIONS.md WAKE_LOCK).
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG)
        wakeLock.acquire(WAKELOCK_TIMEOUT_MS)

        try {
            val serviceIntent = Intent(context, AzanService::class.java).apply {
                putExtra(EXTRA_PRAYER_NAME, prayerName)
                putExtra(EXTRA_AZAN_TYPE, intent.getStringExtra(EXTRA_AZAN_TYPE))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            Log.i("Mawaqit", "Prayer alarm fired: $prayerName — AzanService starting")
        } catch (e: Exception) {
            // e.g. FGS-from-background denied on some OEMs when only the inexact
            // window fallback ran. Never crash the alarm path.
            Log.e("Mawaqit", "Could not start AzanService for $prayerName", e)
            if (wakeLock.isHeld) wakeLock.release()
        }
    }

    companion object {
        const val ACTION_PRAYER_ALARM = "com.mawaqit.app.PRAYER_ALARM"
        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
        const val EXTRA_AZAN_TYPE = "extra_azan_type"

        private const val WAKELOCK_TAG = "mawaqit:azan_handoff"
        private const val WAKELOCK_TIMEOUT_MS = 60_000L // 60s is plenty to reach startForeground
    }
}
