package com.scribe.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ── Scribe Liquid Glass Palette (Apple iOS 26 / visionOS Tier) ───────────────
// Semi-translucent frosted glass surfaces floating on a rich aurora gradient.
// Crisp white typography, electric cobalt accents, specular hairline highlights.

val ScribeBackground = Color.Transparent           // Background is handled by AuroraBackground
val ScribeSurface = Color(0x30FFFFFF)              // 19% Frosted Glass Surface
val ScribeSurfaceVariant = Color(0x1EFFFFFF)       // 12% Recessed Glass Input / Container
val ScribeSurfaceHighlight = Color(0x40FFFFFF)     // 25% Pressed State
val ScribeOutline = Color(0x3DFFFFFF)              // 24% Specular Frosted Hairline Border
val ScribeOutlineAccent = Color(0x8060A5FA)        // Sapphire Glass Focus Ring

// Precision Accents
val ScribeCobalt = Color(0xFF60A5FA)               // Electric Sapphire Cobalt Blue
val ScribeIce = Color(0xFF38BDF8)                  // Sky Cyan
val ScribeCyan = Color(0xFF22D3EE)                 // Secondary Cyan
val ScribeEmerald = Color(0xFF34D399)              // Mint Emerald
val ScribeRose = Color(0xFFF87171)                 // Crimson Danger
val ScribeAmber = Color(0xFFFBBF24)                // Warm Amber

// High-Legibility Glass Typography
val ScribeTextPrimary = Color(0xFFFFFFFF)          // Pure White Ink
val ScribeTextSecondary = Color(0xB8FFFFFF)        // 72% White
val ScribeTextTertiary = Color(0x7AFFFFFF)         // 48% Muted White

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
val ScribeSurfaceDark = Color(0xFF0F1535)
val ScribeElevatedCard = ScribeSurface
val ScribePureWhite = ScribeTextPrimary
val ScribeMutedText = ScribeTextSecondary

private val ScribeGlassColorScheme = darkColorScheme(
    background = Color(0xFF0A0F2E),
    surface = Color(0xFF101633),
    surfaceVariant = Color(0xFF171E42),
    surfaceContainerHigh = Color(0xFF1F2856),
    onBackground = ScribeTextPrimary,
    onSurface = ScribeTextPrimary,
    onSurfaceVariant = ScribeTextSecondary,
    outline = ScribeOutline,
    outlineVariant = ScribeOutlineAccent,
    primary = ScribeCobalt,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = ScribeIce,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF0369A1),
    onSecondaryContainer = Color(0xFFE0F2FE),
    error = ScribeRose,
    onError = Color.White,
    errorContainer = Color(0xFF991B1B),
    onErrorContainer = Color(0xFFFEE2E2),
    tertiary = ScribeEmerald,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF065F46),
    onTertiaryContainer = Color(0xFFD1FAE5)
)

@Composable
fun ScribeTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = ScribeGlassColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            // Dark aurora background = light icons (white) on status & nav bars!
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}