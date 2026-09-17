package com.scribe.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ── Scribe Warm Editorial Studio Light Palette ──────────────────────────────
// Fusion of Apple/Linear Modern Studio precision + Warm Editorial Linen luxury notebook.
// Soft warm linen paper canvas, crisp pure white elevated cards, jet charcoal ink typography,
// razor-sharp warm paper borders, and refined studio ink blue & brass accents.

val ScribeBackground = Color(0xFFF9F8F6)        // Warm Editorial Linen Canvas
val ScribeSurface = Color(0xFFFFFFFF)           // Pure White Floating Cards
val ScribeSurfaceVariant = Color(0xFFF2EFEA)    // Interactive Containers & Input Fields
val ScribeSurfaceHighlight = Color(0xFFEAE5DC)  // Active / Selected Containers
val ScribeOutline = Color(0xFFE2DDD4)           // Delicate Warm Paper Hairline
val ScribeOutlineAccent = Color(0xFF93C5FD)     // Subtle Studio Blue Accent Border

// Precision Accents
val ScribeCobalt = Color(0xFF1D4ED8)            // Studio Ink Blue
val ScribeIce = Color(0xFF0284C7)               // Electric Cerulean / Sky
val ScribeCyan = Color(0xFF0891B2)              // Secondary Cyan Ink
val ScribeEmerald = Color(0xFF059669)           // Forest Emerald (Status Active)
val ScribeRose = Color(0xFFDC2626)              // Crimson Red (Destructive / Danger)
val ScribeAmber = Color(0xFFD97706)             // Warm Editorial Brass / Amber

// High-Contrast Ink Typography
val ScribeTextPrimary = Color(0xFF191715)       // Jet Charcoal Ink (Headings & Body)
val ScribeTextSecondary = Color(0xFF5E574E)     // Graphite Ink (Subtitles & Labels)
val ScribeTextTertiary = Color(0xFF8C8377)      // Muted Paper Stone (Hints & Captions)

// Backward Compatibility Aliases
val ScribeGold = ScribeAmber
val ScribeCopper = ScribeCobalt
val ScribeTerracotta = ScribeRose
val ScribeSage = ScribeEmerald
val ScribeParchment = ScribeTextPrimary
val ScribeParchmentMuted = ScribeTextSecondary
val ScribeParchmentDim = ScribeTextTertiary
val ScribeIndigo = ScribeCobalt
val ScribeIndigoAccent = ScribeIce
val ScribePulseGreen = ScribeEmerald
val ScribeRed = ScribeRose
val ScribeSurfaceDark = ScribeSurfaceVariant
val ScribeElevatedCard = ScribeSurface
val ScribePureWhite = ScribeTextPrimary
val ScribeMutedText = ScribeTextSecondary

private val ScribeLightColorScheme = lightColorScheme(
    background = ScribeBackground,
    surface = ScribeSurface,
    surfaceVariant = ScribeSurfaceVariant,
    surfaceContainerHigh = ScribeSurfaceHighlight,
    onBackground = ScribeTextPrimary,
    onSurface = ScribeTextPrimary,
    onSurfaceVariant = ScribeTextSecondary,
    outline = ScribeOutline,
    outlineVariant = ScribeOutlineAccent,
    primary = ScribeCobalt,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = ScribeIce,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0369A1),
    error = ScribeRose,
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B),
    tertiary = ScribeEmerald,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD1FAE5),
    onTertiaryContainer = Color(0xFF065F46)
)

@Composable
fun ScribeTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = ScribeLightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            // Light status bars & navigation bars = dark icons on light background!
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}