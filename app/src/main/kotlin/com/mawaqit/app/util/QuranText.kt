package com.mawaqit.app.util

import com.mawaqit.app.data.db.AyahEntity

/**
 * Quran text rendering helpers (PHASE_6).
 *
 * API_REFERENCE.md Endpoint 2.2 known quirk: translations sometimes embed
 * footnote digit markers directly after a word/comma ("Allāh,1 the Entirely
 * Merciful"). They are stripped at the RENDER layer ONLY — cached DB text
 * stays raw, per the guidebook rule.
 */
object QuranText {

    /** Digits glued to a preceding letter or comma (Latin or Arabic ،). */
    private val FOOTNOTE_MARKER = Regex("(?<=[\\p{L},،])\\d+")

    fun stripFootnoteMarkers(text: String): String = text.replace(FOOTNOTE_MARKER, "")

    private val ARABIC_INDIC = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')

    /** Ayah number in Quranic ornament parens with Arabic-Indic digits: ﴿١٢﴾. */
    fun arabicMarker(number: Int): String {
        val digits = number.toString().map { ARABIC_INDIC[it - '0'] }.joinToString("")
        return "﴿$digits﴾"
    }

    // PHASE-6.2: the old ~750-char guessing chunker is retired — page packing
    // now happens on-screen via FitPages (ui/quran/FitPages.kt), which measures
    // real rendered text so pages exactly fill the visible card.
}
