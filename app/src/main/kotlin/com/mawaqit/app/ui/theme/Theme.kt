package com.mawaqit.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Light mode ONLY (RULES.md Rule 5). No dark scheme, no isSystemInDarkTheme().
 *
 * M3 role mapping per DESIGN.md §1:
 *  - primary = GOLD (#e38f33): all CTAs, active nav pill — the kit's warm accent role.
 *  - secondary/tertiary = brand blue.
 *  - background = off-white page; surface = white cards (flat hierarchy, elevation 0).
 *  - error = true red — DESIGN.md §9: never collapse error into the CTA accent.
 *
 * Colors without M3 slots (SurfaceDeep hero, SuccessGreen) are referenced directly
 * from Color.kt tokens in composables: com.mawaqit.app.ui.theme.SurfaceDeep, etc.
 */
private val MawaqitLightColors = lightColorScheme(
    primary = PrimaryGold,
    onPrimary = Color.White,
    primaryContainer = ChipBg,
    onPrimaryContainer = PrimaryBlue,
    secondary = PrimaryBlue,
    onSecondary = Color.White,
    secondaryContainer = ChipBg,
    onSecondaryContainer = PrimaryBlue,
    tertiary = PrimaryBlue,
    onTertiary = Color.White,
    background = BgOffwhite,
    onBackground = TextPrimary,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    surfaceVariant = ChipBg,
    onSurfaceVariant = TextMuted,
    error = ErrorRed,
    onError = Color.White,
    outline = TextMuted,
)

@Composable
fun MawaqitTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MawaqitLightColors,
        typography = MawaqitTypography,
        content = content,
    )
}
