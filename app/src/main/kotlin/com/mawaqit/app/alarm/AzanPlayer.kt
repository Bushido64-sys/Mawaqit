package com.mawaqit.app.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.util.Log
import com.mawaqit.app.data.prefs.PrefsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Plays the bundled Azan MP3s on the ALARM stream (Rule 14: azan must respect
 * alarm volume and play on silent).
 *
 * BUGFIX vs the original ASSETS.md snippet (patched into the guidebook
 * 2026-09-18): MediaPlayer.create() returns an ALREADY-prepared player, and
 * setAudioAttributes() after that throws IllegalStateException. So we build
 * the player manually: attributes FIRST, then data source, then prepare().
 * playAzan() must be called off the main thread (prepare() is synchronous) —
 * AzanService dispatches to Dispatchers.IO.
 */
@Singleton
class AzanPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PrefsRepository
) {
    private var mediaPlayer: MediaPlayer? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    fun playAzan(azanType: AzanType) {
        stop() // never two players at once
        val volume = runBlocking { prefs.getAzanVolumeOnce() }
        val forceAlarm = runBlocking { prefs.getAzanForceAlarmOnce() }
        if (forceAlarm) requestAudioFocus()
        val player = MediaPlayer()
        try {
            player.setAudioAttributes(alarmAttributes())
            player.setDataSource(
                context,
                Uri.parse("android.resource://${context.packageName}/${azanType.resId}")
            )
            player.isLooping = false
            player.setVolume(volume, volume)
            player.setOnCompletionListener { mp ->
                mp.release()
                if (mediaPlayer === mp) mediaPlayer = null
                releaseAudioFocus()
            }
            player.prepare()
            player.start()
            mediaPlayer = player
        } catch (e: Exception) {
            // Corrupt/missing file must never crash the service (Rule 8 spirit).
            Log.e(TAG, "Azan playback failed for $azanType", e)
            try { player.release() } catch (_: Exception) { /* already released */ }
            mediaPlayer = null
            releaseAudioFocus()
        }
    }

    fun stop() {
        mediaPlayer?.let { mp ->
            try {
                mp.stop()
            } catch (_: IllegalStateException) {
                // stop() is only legal after prepare(); a failed prepare already
                // left the player unusable — releasing below is what matters.
            }
            mp.release()
        }
        mediaPlayer = null
        releaseAudioFocus()
    }

    /** "Force alarm": duck other players while the adhan sounds (Muslim Pro parity). */
    private fun requestAudioFocus() {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(alarmAttributes())
                .build()
            am.requestAudioFocus(req)
            audioFocusRequest = req
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(null, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        }
    }

    private fun releaseAudioFocus() {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
            audioFocusRequest = null
        } else {
            @Suppress("DEPRECATION")
            am.abandonAudioFocus(null)
        }
    }

    private fun alarmAttributes(): AudioAttributes =
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)     // STREAM_ALARM — Rule 14
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

    companion object {
        private const val TAG = "Mawaqit"
    }
}
