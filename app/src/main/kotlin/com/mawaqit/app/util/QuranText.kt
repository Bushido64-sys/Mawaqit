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

    /**
     * PHASE-6.1 mushaf paging: chunk a surah's ayahs into card-sized pages for
     * the HorizontalPager. Never splits an ayah; a page holds roughly
     * [targetChars] Arabic characters (~1/3 of a phone screen at reader size)
     * so long surahs become a handful of swipeable cards.
     */
    fun chunkIntoPages(
        ayahs: List<AyahEntity>,
        targetChars: Int = 750
    ): List<List<AyahEntity>> {
        if (ayahs.isEmpty()) return emptyList()
        val pages = mutableListOf<List<AyahEntity>>()
        var current = mutableListOf<AyahEntity>()
        var charCount = 0
        for (ayah in ayahs) {
            val len = ayah.arabicText.length
            if (current.isNotEmpty() && charCount + len > targetChars) {
                pages.add(current)
                current = mutableListOf()
                charCount = 0
            }
            current.add(ayah)
            charCount += len
        }
        if (current.isNotEmpty()) pages.add(current)
        return pages
    }
}
