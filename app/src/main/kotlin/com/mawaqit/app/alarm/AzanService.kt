package com.mawaqit.app.alarm

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.mawaqit.app.data.db.MawaqitDatabase
import com.mawaqit.app.data.db.SalahLogEntity
import com.mawaqit.app.data.model.PrayerName
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Foreground service that plays the Azan (PHASE_3_ALARMS.md file #3).
 *
 * Lifecycle of one alarm:
 *   AlarmReceiver → startForegroundService(this) → notification up within the
 *   5-second FGS budget → azan plays on the ALARM stream → stops when
 *   (a) the audio completes, (b) 5 minutes elapse (Rule 14), or
 *   (c) the user taps "Mark as Prayed" (AzanReceiver stops this service).
 *
 * START_NOT_STICKY: if Android kills us mid-azan we do NOT want a random
 * replay an hour later — the salah log still says the time was reached.
 */
@AndroidEntryPoint
class AzanService : Service() {

    @Inject lateinit var azanPlayer: AzanPlayer
    @Inject lateinit var db: MawaqitDatabase

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var started = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prayerNameStr = intent?.getStringExtra(AlarmReceiver.EXTRA_PRAYER_NAME)
        val isTest = prayerNameStr == TEST_PRAYER_NAME // PHASE-3.2 diagnostics sentinel
        val prayer = PrayerName.entries.firstOrNull { it.name == prayerNameStr }
        if (prayer == null && !isTest) {
            Log.e("Mawaqit", "AzanService started without a valid prayer — stopping")
            stopSelf()
            return START_NOT_STICKY
        }
        val prayerLabel = if (isTest) "Test azan" else prayer?.displayName() ?: "Azan"

        // 1. Notification FIRST — the system gives ~5s to reach startForeground().
        AzanServiceUi.createChannel(this)
        AzanServiceUi.startAsForeground(
            this,
            AzanServiceUi.buildNotification(this, prayerLabel)
        )

        if (started) return START_NOT_STICKY // already playing for this/another prayer
        started = true

        // 2. Log "time reached" in salah_log (prayed stays false — user must tap).
        //    Only when no row exists yet, so we never overwrite a "prayed" mark.
        //    Test alarms (diagnostics) never touch the log — no fake data.
        if (!isTest && prayer != null) serviceScope.launch {
            try {
                val todayIso = LocalDate.now().toString()
                if (db.salahLogDao().getSalahStatus(todayIso, prayer.name) == null) {
                    db.salahLogDao().upsert(
                        SalahLogEntity(
                            date = todayIso,
                            prayer = prayer.name,
                            prayed = false,
                            markedAt = null
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e("Mawaqit", "Could not write salah_log time-reached row", e)
            }
        }

        // 3. Play the azan (prepare() is synchronous → Dispatchers.IO).
        val azanType = intent
            ?.getStringExtra(AlarmReceiver.EXTRA_AZAN_TYPE)
            ?.let { type ->
                try {
                    AzanType.valueOf(type)
                } catch (_: IllegalArgumentException) {
                    AzanType.DEFAULT
                }
            }
            ?: AzanType.DEFAULT
        serviceScope.launch(Dispatchers.IO) { azanPlayer.playAzan(azanType) }

        // 4. Rule 14: hard 5-minute cutoff, whichever comes first.
        serviceScope.launch {
            delay(FIVE_MINUTES_MS)
            Log.i("Mawaqit", "Azan 5-minute cutoff reached — stopping service")
            stopSelf()
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        azanPlayer.stop()
        AzanServiceUi.stopForeground(this)
        Log.i("Mawaqit", "AzanService destroyed — audio stopped, notification removed")
        super.onDestroy()
    }

    private fun PrayerName.displayName(): String =
        name.lowercase().replaceFirstChar { it.uppercase() }

    companion object {
        private const val FIVE_MINUTES_MS = 5 * 60_000L

        /** Sentinel prayer name for diagnostics test alarms (PHASE-3.2). */
        const val TEST_PRAYER_NAME = "TEST"
    }
}
