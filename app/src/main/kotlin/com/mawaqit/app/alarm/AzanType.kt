package com.mawaqit.app.alarm

import androidx.annotation.RawRes
import androidx.annotation.StringRes
import com.mawaqit.app.R

/**
 * Every bundled Azan — the SINGLE source of truth (RULES.md Rule 14A).
 *
 * Each entry carries its own display label and audio file, so adding a voice is one enum
 * line plus the `res/raw` file and the EN/UR strings. No `when` mapping anywhere.
 *
 * [FAJR] is the only non-selectable entry: Rule 14 says Fajr always uses the special
 * azan that includes "As-salatu khayrun min an-nawm". It therefore participates in
 * playback but never shows up in a picker.
 */
enum class AzanType(
    @StringRes val labelRes: Int,
    @RawRes val resId: Int,
    val userSelectable: Boolean,
    /** What PrefsRepository stores in `selected_azan`. */
    val storage: String,
    /**
     * Where this voice's preview excerpt begins, in SECONDS.
     *
     * Per-voice because an adhan's opening "Allahu Akbar" x4 has no fixed length — it
     * runs 22s on Alafasy, 9s on Abdulbasit, 4-8s on the rest. One shared offset would
     * start some voices mid-phrase. Each value below is a measured phrase boundary
     * (end of a >0.7s silence) that sits just past that opening chant, so the preview
     * opens cleanly on the shahada — the part that actually identifies the muezzin.
     */
    val previewStartSec: Int,
) {
    ALAFASY(R.string.azan_alafasy, R.raw.azan_alafasy, userSelectable = true, storage = "alafasy", previewStartSec = 24),
    ABDULBASIT(R.string.azan_abdulbasit, R.raw.azan_abdulbasit, userSelectable = true, storage = "abdulbasit", previewStartSec = 15),
    ALI_MALA(R.string.azan_ali_mala, R.raw.azan_ali_mala, userSelectable = true, storage = "ali_mala", previewStartSec = 24),
    NAFEES(R.string.azan_nafees, R.raw.azan_nafees, userSelectable = true, storage = "nafees", previewStartSec = 15),
    OZCAN(R.string.azan_ozcan, R.raw.azan_ozcan, userSelectable = true, storage = "ozcan", previewStartSec = 27),
    ZAHRANI(R.string.azan_zahrani, R.raw.azan_zahrani, userSelectable = true, storage = "zahrani", previewStartSec = 15),
    FAJR(R.string.azan_fajr, R.raw.azan_fajr, userSelectable = false, storage = "fajr", previewStartSec = 23);

    companion object {
        /** The voice used for the other four prayers when the user has not chosen. */
        val DEFAULT: AzanType = ALAFASY

        /** What a picker lists — never includes the Fajr-only entry. */
        val choices: List<AzanType> = AzanType.entries.filter { it.userSelectable }

        /**
         * Resolve a value carried in an alarm intent's `extra_azan_type`.
         *
         * May legitimately be ANY entry — a Fajr alarm was planned with FAJR — so this
         * one does not filter. Absent/legacy values ("DEFAULT"/"MAKKAH" from before
         * PHASE-10, or null) land on their nearest new homes.
         */
        fun fromStorage(raw: String?): AzanType {
            entries.firstOrNull { it.storage.equals(raw, ignoreCase = true) }?.let { return it }
            return legacy(raw)
        }

        /**
         * Resolve the user's STORED SELECTION from prefs.
         *
         * Unlike [fromStorage] this only ever returns a **user-selectable** voice. That
         * matters for legacy installs: before PHASE-10 the documented pref domain was
         * "default" | "fajr" | "makkah", so an existing user can have `"fajr"` stored.
         * Matching it against [FAJR] would play the "As-salatu khayrun min an-nawm" azan
         * at Dhuhr, Asr, Maghrib and Isha — forever, with nothing highlighted in the
         * picker. Falling back keeps that profile on a normal voice.
         */
        fun selectedFromStorage(raw: String?): AzanType {
            entries.firstOrNull {
                it.userSelectable && it.storage.equals(raw, ignoreCase = true)
            }?.let { return it }
            return legacy(raw)
        }

        /**
         * Pre-PHASE-10 stored values ("default" / "makkah") mapped to their nearest
         * new homes, so an existing user's choice survives the upgrade.
         */
        private fun legacy(raw: String?): AzanType = when (raw?.trim()?.lowercase()) {
            "makkah" -> ALI_MALA   // the old Makkah recording → a Makkah Haram muezzin
            else -> DEFAULT
        }
    }
}
