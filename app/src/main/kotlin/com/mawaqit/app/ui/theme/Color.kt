package com.mawaqit.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Mawaqit color tokens — mirrors res/values/colors.xml (design_tokens.xml).
 * Rule 4: never hardcode colors in composables; reference these via the theme
 * (MaterialTheme.colorScheme) or these named app tokens for surfaces the M3
 * scheme has no slot for (e.g. the dark hero card).
 */
val PrimaryGold = Color(0xFFE38F33)   // CTAs, active nav pill (DESIGN.md §1 #2)
val PrimaryBlue = Color(0xFF0C65A5)   // brand blue, links, chip text
val SurfaceDeep = Color(0xFF0C2B45)   // dark hero card (Next Prayer, splash cards)
val BgOffwhite  = Color(0xFFE3E9EE)   // page background
val SurfaceWhite = Color(0xFFFFFFFF)  // content cards, list rows
val TextPrimary = Color(0xFF12181F)   // headlines, body-strong
val TextMuted   = Color(0xFF5A6475)   // subtitles, helper text, unselected icons
val ChipBg      = Color(0xFFE8F2FB)   // light blue chip fill
val ChipText    = Color(0xFF0C65A5)   // chip text
val SuccessGreen = Color(0xFF2E7D32)  // prayed checkmarks, streaks
val ErrorRed     = Color(0xFFC62828)  // genuine errors — NEVER for CTAs (DESIGN.md §9)

// Splash video failure fallback gradient (ASSETS.md §2)
val SplashGradientStart = Color(0xFF4A90D4)
val SplashGradientEnd   = Color(0xFF0C65A5)
