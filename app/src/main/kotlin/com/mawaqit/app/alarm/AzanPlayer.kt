package com.mawaqit.app.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val context: Context
) {
    private var mediaPlayer: MediaPlayer? = null

    fun playAzan(azanType: AzanType) {
        stop() // never two players at once
        val player = MediaPlayer()
        try {
            player.setAudioAttributes(alarmAttributes())
            player.setDataSource(
                context,
                Uri.parse("android.resource://${context.packageName}/${azanType.audioResId()}")
            )
            player.isLooping = false
            player.setOnCompletionListener { mp ->
                mp.release()
                if (mediaPlayer === mp) mediaPlayer = null
            }
            player.prepare()
            player.start()
            mediaPlayer = player
        } catch (e: Exception) {
            // Corrupt/missing file must never crash the service (Rule 8 spirit).
            Log.e(TAG, "Azan playback failed for $azanType", e)
            try { player.release() } catch (_: Exception) { /* already released */ }
            mediaPlayer = null
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
