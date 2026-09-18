package com.mawaqit.app.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.mawaqit.app.MainActivity
import com.mawaqit.app.R

/**
 * Builds the notification channel and the azan notification itself, kept out of
 * AzanService so the service file stays readable (single responsibility).
 *
 * Channel is IMPORTANCE_HIGH with sound = null — the audio comes from
 * MediaPlayer on the ALARM stream (Rule 14), NOT from the notification.
 * Vibration pattern per PHASE_3_ALARMS.md: [0, 500, 500, 500].
 */
object AzanServiceUi {

    const val CHANNEL_ID = "prayer_alarms"
    const val NOTIFICATION_ID = 1001

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.alarm_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            setSound(null, null)                          // audio is played by AzanPlayer
            vibrationPattern = longArrayOf(0, 500, 500, 500)
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    /** "Azan — Fajr" notification with the "Mark as Prayed" action. */
    fun buildNotification(context: Context, prayerLabel: String): Notification {
        // Tap on the notification body → open the app
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // "Mark as Prayed" action → AzanReceiver (stops service + logs the prayer)
        val markPrayedIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, AzanReceiver::class.java).apply {
                action = AzanReceiver.ACTION_MARK_PRAYED
                putExtra(AzanReceiver.EXTRA_PRAYER_NAME, prayerLabel.uppercase())
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm) // placeholder vector until PHASE_4/9 art
            .setContentTitle(context.getString(R.string.azan_notification_title, prayerLabel))
            .setContentText(context.getString(R.string.azan_notification_body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true) // it's an alarm — no swipe-dismiss; it clears when audio stops
            .setContentIntent(contentIntent)
            .addAction(0, context.getString(R.string.mark_as_prayed), markPrayedIntent)
            .build()
    }

    /**
     * startForeground wrapper: API 29+ wants the service type passed explicitly,
     * and API 34 REQUIRES it to match the manifest's foregroundServiceType.
     */
    fun startAsForeground(service: android.app.Service, notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                service,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            service.startForeground(NOTIFICATION_ID, notification)
        }
    }

    /** Remove the notification when the azan finishes/stops. */
    fun stopForeground(service: android.app.Service) {
        ServiceCompat.stopForeground(service, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }
}
