package com.mawaqit.app.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import com.mawaqit.app.data.prefs.PrefsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Plays a short, judgeable excerpt of a bundled Azan so the user can pick a voice.
 *
 * WHY THIS IS SEPARATE FROM [AzanPlayer] (PHASE-10.2):
 * `AzanPlayer` is a `@Singleton` that [AzanService] injects, so a preview and a real
 * prayer azan were fighting over one `MediaPlayer`. Stopping a preview could kill a
 * ringing azan and vice versa. This class owns a player that [AzanService] never sees,
 * so the two cannot interfere by construction — not by discipline.
 *
 * The excerpt is NOT a fidelity test of the alarm. Its only job is to let the user
 * recognise each muezzin, which needs the part of the adhan that actually carries
 * voice and melody. A recording's first seconds are "Allahu Akbar" x4 — slow, chant-
 * like, heavy with mosque reverb, and the LEAST distinctive part of the voice (measured:
 * the opening phrase runs to 22s on Alafasy, 9s on Abdulbasit, 4-8s on the rest).
 * Playing from 0 therefore gave the user no way to tell two muezzins apart, which is
 * why the original 5s preview was unusable.
 *
 * So: each voice skips to its own measured phrase boundary ([AzanType.previewStartSec])
 * and plays [PREVIEW_LENGTH_MS] — long enough to hear a full shahada phrase, short
 * enough to audition several voices without waiting.
 *
 * The Settings/onboarding UI reads [previewProgress] to drive the play/pause toggle and
 * the progress bar, so what the user SEES is what the audio is actually doing.
 *
 * Uses USAGE_MEDIA so the preview follows the phone's normal media volume and is never
 * silenced by a quiet ringer (a preview nobody can hear is worse than no preview).
 * The real azan keeps USAGE_ALARM per Rule 14 — that behaviour is unchanged.
 */
@Singleton
class AzanPreviewPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PrefsRepository
) {
    private var mediaPlayer: MediaPlayer? = null

    /** Offset the current preview started at, so [previewProgress] can subtract it. */
    private var startOffsetMs: Int = 0

    /**
     * Play a ~25s excerpt of [azanType], starting at that voice's own measured
     * [AzanType.previewStartSec]. Safe to call repeatedly — each call replaces any
     * preview still playing.
     */
    fun playPreview(azanType: AzanType) {
        stopPreview() // never two previews at once
        val volume = runBlocking { prefs.getAzanVolumeOnce() }
        val player = MediaPlayer()
        try {
            player.setAudioAttributes(previewAttributes())
            player.setDataSource(
                context,
                Uri.parse("android.resource://${context.packageName}/${azanType.resId}")
            )
            player.isLooping = false
            player.setVolume(volume, volume)
            player.setOnCompletionListener { mp ->
                mp.release()
                if (mediaPlayer === mp) mediaPlayer = null
            }
            // prepare() is synchronous and blocks on the small bundled file, so this
            // whole method must run off the main thread (callers use Dispatchers.IO).
            player.prepare()
            val offsetMs = azanType.previewStartSec * 1_000
            player.seekTo(offsetMs)
            player.start()
            mediaPlayer = player
            startOffsetMs = offsetMs
        } catch (e: Exception) {
            // A bad resId or an unseekable file must never crash the Settings screen.
            Log.e(TAG, "Azan preview failed for $azanType", e)
            try { player.release() } catch (_: Exception) { /* already released */ }
            mediaPlayer = null
        }
    }

    /** How far along the excerpt we are, 0f..1f, read from the player itself so the
     *  UI can never drift out of sync with the audio. Null when nothing is previewing. */
    fun previewProgress(): Float? {
        val mp = mediaPlayer ?: return null
        if (!mp.isPlaying) return null
        // currentPosition is absolute in the file; subtract the offset we seeked to.
        val into = mp.currentPosition - startOffsetMs
        if (into < 0) return 0f
        return (into.toFloat() / PREVIEW_LENGTH_MS).coerceIn(0f, 1f)
    }

    /** Stop the preview and release its player. Always safe to call. */
    fun stopPreview() {
        mediaPlayer?.let { mp ->
            try {
                if (mp.isPlaying) mp.stop()
            } catch (_: IllegalStateException) {
                // stop() is only legal after prepare(); a failed prepare already left
                // the player unusable — releasing below is what matters.
            }
            mp.release()
        }
        mediaPlayer = null
    }

    companion object {
        private const val TAG = "Mawaqit"

        /** How long the excerpt plays (ms). Long because it feeds kotlinx's delay(). */
        const val PREVIEW_LENGTH_MS = 25_000L

        private fun previewAttributes(): AudioAttributes =
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)          // normal media volume
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
    }
}
