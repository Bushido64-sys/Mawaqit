package com.mawaqit.app.ui.quran

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mawaqit.app.data.db.AyahEntity
import com.mawaqit.app.data.db.SurahEntity
import com.mawaqit.app.ui.theme.AmiriFontFamily
import com.mawaqit.app.ui.theme.InterFontFamily
import com.mawaqit.app.ui.theme.NotoNaskhArabicFamily
import com.mawaqit.app.util.QuranText

/**
 * PHASE-6.2 "The Perfect Page" — measures REAL rendered text heights with a
 * Compose [TextMeasurer] and packs ayahs into pages that exactly fill the
 * visible card. The user swipes; the page never scrolls (except the rare
 * single-ayah-taller-than-a-page safety valve, e.g. Ayat al-Kursi at XL).
 *
 * Re-fits automatically for font size and translation mode: bigger font ⇒
 * fewer verses per page, translations on ⇒ fewer verses per page.
 *
 * The layout constants below MUST stay in sync with MushafPage's paddings —
 * MushafPage consumes these same constants, so drift is impossible.
 */
object FitPages {

    // ── Layout chrome (shared with SurahDetailScreen.MushafPage) ──────────
    val PAGE_PAD_H = 16.dp   // gutter outside the card
    val PAGE_PAD_V = 8.dp
    val CARD_PAD_H = 20.dp   // inside the gold-framed card
    val CARD_PAD_V = 16.dp

    /**
     * PHASE-6.3 — the bottom strip inside the card (Mark-page button + page
     * indicator). MushafPage renders it with this exact height and consumes
     * it from its ayah budget, so FitPages reserves the same amount.
     */
    val MUSHAF_FOOTER_H = 64.dp

    private val EXTRA_SAFETY = 8.dp            // final breathing margin
    private val AYAH_BLOCK_VPAD = 10.dp        // AyahBlock vertical padding (each side)
    private val TRANSLATION_GAP = 4.dp         // gap between Arabic and its translation
    private val BISMILLAH_GAP = 12.dp
    private val HEADER_NAME_SUB_GAP = 2.dp
    private val HEADER_DIVIDER_SPACE = 10.dp   // spacer above AND below the divider
    private val FOOTER_TEXT_GAP = 8.dp
    private val FOOTER_DOTS_SPACE = 10.dp
    private val DOT_HEIGHT = 6.dp
    private const val FOOTER_LINES = 2         // reserved lines for "Page X of Y · Verses a–b"

    // ── Type styles — MUST mirror the renderers in SurahDetailScreen ──────
    private const val HEADER_NAME_SP = 24f
    private const val HEADER_NAME_LINE_SP = 0f  // header name renders without explicit lineHeight
    private const val HEADER_SUB_SP = 12f
    private const val HEADER_SUB_LINE_SP = 16f
    private const val ARABIC_SP = 22f
    private const val ARABIC_LINE_SP = 38f
    private const val ENGLISH_SP = 14f
    private const val ENGLISH_LINE_SP = 20f
    private const val URDU_SP = 16f
    private const val URDU_LINE_SP = 26f
    private const val BISMILLAH_SP = 20f
    private const val BISMILLAH_LINE_SP = 34f
    private const val FOOTER_LINE_SP = 16f

    /** One fitted page: its ayahs + whether that page may scroll internally. */
    data class FittedPage(
        val ayahs: List<AyahEntity>,
        val allowScroll: Boolean = false
    )

    /**
     * Packs [ayahs] into pages that fit one screen. Page 0 carries the
     * Bismillah (slightly smaller budget); later pages don't.
     */
    fun fit(
        ayahs: List<AyahEntity>,
        surah: SurahEntity?,
        mode: DisplayMode,
        fontScale: Float,
        bismillahText: String,
        showBismillah: Boolean,
        measurer: TextMeasurer,
        density: Density,
        maxWidthPx: Int,
        maxHeightPx: Int,
        footerExtraHeight: Int = 0
    ): List<FittedPage> {
        if (ayahs.isEmpty()) return emptyList()
        if (maxWidthPx <= 0 || maxHeightPx <= 0) {
            return listOf(FittedPage(ayahs, allowScroll = true))
        }

        val textWidth = with(density) {
            maxWidthPx - (PAGE_PAD_H * 2).roundToPx() - (CARD_PAD_H * 2).roundToPx()
        }
        val cardInnerHeight = with(density) {
            maxHeightPx - (PAGE_PAD_V * 2).roundToPx() - (CARD_PAD_V * 2).roundToPx()
        }
        if (textWidth <= 0 || cardInnerHeight <= 0) {
            return listOf(FittedPage(ayahs, allowScroll = true))
        }

        // Chrome that repeats on every page: surah header + footer.
        val headerPx = if (surah != null) {
            val namePx = measurePx(
                text = surah.nameArabic,
                family = AmiriFontFamily,
                fontSizeSp = HEADER_NAME_SP,
                lineHeightSp = HEADER_NAME_LINE_SP,
                measurer = measurer,
                widthPx = textWidth
            )
            val subPx = measurePx(
                text = "${surah.nameEnglish} · ${surah.nameMeaning}",
                family = InterFontFamily,
                fontSizeSp = HEADER_SUB_SP,
                lineHeightSp = HEADER_SUB_LINE_SP,
                measurer = measurer,
                widthPx = textWidth
            )
            with(density) {
                namePx + HEADER_NAME_SUB_GAP.roundToPx() + subPx +
                    HEADER_DIVIDER_SPACE.roundToPx() + 1 + HEADER_DIVIDER_SPACE.roundToPx()
            }
        } else 0

        val footerPx = with(density) {
            (FOOTER_LINE_SP * FOOTER_LINES).sp.roundToPx() +
                FOOTER_TEXT_GAP.roundToPx() +
                FOOTER_DOTS_SPACE.roundToPx() +
                DOT_HEIGHT.roundToPx()
        }

        val bismillahPx = if (showBismillah) {
            with(density) {
                measurePx(
                    text = bismillahText,
                    family = NotoNaskhArabicFamily,
                    fontSizeSp = BISMILLAH_SP * fontScale,
                    lineHeightSp = BISMILLAH_LINE_SP * fontScale,
                    measurer = measurer,
                    widthPx = textWidth
                ) + BISMILLAH_GAP.roundToPx()
            }
        } else 0

        val safety = with(density) { EXTRA_SAFETY.roundToPx() }
        // footerExtraHeight = the mark-page strip (PHASE-6.3) — same Box the
        // renderer reserves via Modifier.height(MUSHAF_FOOTER_H).
        val baseBudget = cardInnerHeight - headerPx - footerPx - safety - footerExtraHeight
        if (baseBudget <= 0) return listOf(FittedPage(ayahs, allowScroll = true))

        // Measure every ayah once, then pack greedily.
        val heights = ayahs.map { ayahHeightPx(it, mode, fontScale, measurer, density, textWidth) }

        val pages = mutableListOf<FittedPage>()
        var current = mutableListOf<AyahEntity>()
        var used = 0
        // Page 0 carries the Bismillah → smaller first-page budget.
        var budget = baseBudget - bismillahPx

        for (i in ayahs.indices) {
            val h = heights[i]
            if (current.isNotEmpty() && used + h > budget) {
                pages.add(FittedPage(current))
                current = mutableListOf()
                used = 0
                budget = baseBudget
            }
            val oversized = current.isEmpty() && h > budget
            current.add(ayahs[i])
            used += h
            if (oversized) {
                // One ayah taller than a whole page → its own scrollable page.
                pages.add(FittedPage(current, allowScroll = true))
                current = mutableListOf()
                used = 0
                budget = baseBudget
            }
        }
        if (current.isNotEmpty()) pages.add(FittedPage(current))
        return pages
    }

    /** Rendered height of one ayah block (Arabic + translation if the mode shows it). */
    private fun ayahHeightPx(
        ayah: AyahEntity,
        mode: DisplayMode,
        fontScale: Float,
        measurer: TextMeasurer,
        density: Density,
        widthPx: Int
    ): Int {
        var h = measurePx(
            text = "${ayah.arabicText} ${QuranText.arabicMarker(ayah.ayahNumber)}",
            family = NotoNaskhArabicFamily,
            fontSizeSp = ARABIC_SP * fontScale,
            lineHeightSp = ARABIC_LINE_SP * fontScale,
            measurer = measurer,
            widthPx = widthPx
        )
        val gap = with(density) { TRANSLATION_GAP.roundToPx() }
        when (mode) {
            DisplayMode.ARABIC_ONLY -> Unit
            DisplayMode.ARABIC_ENGLISH -> h += gap + measurePx(
                text = QuranText.stripFootnoteMarkers(ayah.translationEn),
                family = InterFontFamily,
                fontSizeSp = ENGLISH_SP,
                lineHeightSp = ENGLISH_LINE_SP,
                measurer = measurer,
                widthPx = widthPx
            )
            DisplayMode.ARABIC_URDU -> h += gap + measurePx(
                text = QuranText.stripFootnoteMarkers(ayah.translationUr),
                family = NotoNaskhArabicFamily,
                fontSizeSp = URDU_SP * fontScale,
                lineHeightSp = URDU_LINE_SP * fontScale,
                measurer = measurer,
                widthPx = widthPx
            )
        }
        h += with(density) { (AYAH_BLOCK_VPAD * 2).roundToPx() }
        return h
    }

    /** One-line measurement helper — mirrors the reader's centered text style. */
    private fun measurePx(
        text: String,
        family: FontFamily,
        fontSizeSp: Float,
        lineHeightSp: Float,
        measurer: TextMeasurer,
        widthPx: Int
    ): Int {
        val style = TextStyle(
            fontFamily = family,
            fontSize = fontSizeSp.sp,
            lineHeight = if (lineHeightSp > 0f) lineHeightSp.sp else TextUnit.Unspecified,
            textAlign = TextAlign.Center
        )
        return measurer.measure(
            text = text,
            style = style,
            constraints = Constraints(maxWidth = widthPx)
        ).size.height
    }
}
