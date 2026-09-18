package com.mawaqit.app.alarm

import androidx.annotation.RawRes
import com.mawaqit.app.R

/**
 * Which bundled Azan file to play (ASSETS.md Section 1, RULES.md Rule 14).
 * Fajr ALWAYS plays azan_fajr.mp3 (includes "As-salatu khayrun min an-nawm");
 * the user's Settings choice (PHASE_8) selects default vs makkah for the rest.
 */
enum class AzanType { DEFAULT, FAJR, MAKKAH }

/** Raw resource for this azan type — file names fixed by ASSETS.md Section 1. */
@RawRes
fun AzanType.audioResId(): Int = when (this) {
    AzanType.FAJR -> R.raw.azan_fajr
    AzanType.MAKKAH -> R.raw.azan_makkah
    AzanType.DEFAULT -> R.raw.azan_default
}
