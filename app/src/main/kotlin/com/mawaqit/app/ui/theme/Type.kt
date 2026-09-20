package com.mawaqit.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.mawaqit.app.R

/**
 * Downloadable Google Fonts (DEPENDENCIES.md "Font Setup").
 * Requires res/values/font_certs.xml — present in this project.
 * Fonts load at runtime; if offline on first ever launch, Android falls back to
 * default sans-serif automatically (graceful degradation).
 */
private val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val interFont = GoogleFont("Inter")
private val notoNaskhArabicFont = GoogleFont("Noto Naskh Arabic")

val InterFontFamily = androidx.compose.ui.text.font.FontFamily(
    Font(googleFont = interFont, fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = interFont, fontProvider = fontProvider, weight = FontWeight.Medium),
    Font(googleFont = interFont, fontProvider = fontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = interFont, fontProvider = fontProvider, weight = FontWeight.Bold),
    Font(googleFont = interFont, fontProvider = fontProvider, weight = FontWeight.ExtraBold),
)

/**
 * PHASE-6.1: Amiri — the classic Naskh Quran typeface — is BUNDLED in
 * res/font so the reader renders true calligraphy even fully offline
 * (Google-Downloadable-Fonts need internet on first use).
 */
val AmiriFontFamily = androidx.compose.ui.text.font.FontFamily(
    Font(R.font.amiri_regular, FontWeight.Normal),
    Font(R.font.amiri_bold, FontWeight.Bold),
)

/**
 * Arabic/Urdu text — Quran reader + Ayah card (PHASE_6, RULES.md Rule 13).
 * Resolution order: bundled Amiri first (always available, Quran-grade
 * Naskh); Noto Naskh Arabic (downloadable) as fallback; system default last.
 */
val NotoNaskhArabicFamily = androidx.compose.ui.text.font.FontFamily(
    Font(R.font.amiri_regular, FontWeight.Normal),
    Font(R.font.amiri_bold, FontWeight.Bold),
    Font(googleFont = notoNaskhArabicFont, fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = notoNaskhArabicFont, fontProvider = fontProvider, weight = FontWeight.Bold),
)

/**
 * Type scale from design_tokens.xml + DESIGN.md §2:
 * hierarchy comes from weight + colour, not size jumps; buttons are Title Case semibold.
 * Single family (Inter) — Arabic gets NotoNaskhArabicFamily applied per-composable.
 */
val MawaqitTypography = Typography(
    // Hero numbers (next prayer countdown, big stats) — ~34–40sp extrabold, tight
    headlineLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 36.sp,
        lineHeight = 40.sp,
    ),
    // Display headlines (onboarding, splash) — 26–30sp bold, tight leading
    headlineMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 32.sp,
    ),
    // Top bar / screen titles — 17–18sp semibold
    titleMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 24.sp,
    ),
    // Card / section titles — 15–16sp
    titleSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    // Body — 14sp medium
    bodyMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    // Body-large variant for emphasized body text
    bodyLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    // Muted captions / helper text — 12–13sp regular
    bodySmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    // Button labels — 14–15sp semibold, Title Case (never ALL CAPS)
    labelLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
)
