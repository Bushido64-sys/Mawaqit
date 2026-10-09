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
) {
    ALAFASY(R.string.azan_alafasy, R.raw.azan_alafasy, userSelectable = true, storage = "alafasy"),
    ABDULBASIT(R.string.azan_abdulbasit, R.raw.azan_abdulbasit, userSelectable = true, storage = "abdulbasit"),
    ALI_MALA(R.string.azan_ali_mala, R.raw.azan_ali_mala, userSelectable = true, storage = "ali_mala"),
    NAFEES(R.string.azan_nafees, R.raw.azan_nafees, userSelectable = true, storage = "nafees"),
    OZCAN(R.string.azan_ozcan, R.raw.azan_ozcan, userSelectable = true, storage = "ozcan"),
    ZAHRANI(R.string.azan_zahrani, R.raw.azan_zahrani, userSelectable = true, storage = "zahrani"),
    FAJR(R.string.azan_fajr, R.raw.azan_fajr, userSelectable = false, storage = "fajr");

    companion object {
        /** The voice used for the other four prayers when the user has not chosen. */
        val DEFAULT: AzanType = ALAFASY

        /** What a picker lists — never includes the Fajr-only entry. */
        val choices: List<AzanType> = AzanType.entries.filter { it.userSelectable }

        /**
         * Maps a stored prefs value back to an entry.
         *
         * Legacy installs wrote `"default"` / `"makkah"` before the voices were named
         * (see DATA_SCHEMA.md), so those map to their nearest new homes rather than
         * silently resetting an existing user's choice.
         */
        fun fromStorage(raw: String?): AzanType {
            AzanType.entries.firstOrNull { it.storage.equals(raw, ignoreCase = true) }?.let { return it }
            return when (raw?.trim()?.lowercase()) {
                "makkah" -> ALI_MALA   // the old Makkah recording → a Makkah Haram muezzin
                else -> DEFAULT
            }
        }
    }
}
