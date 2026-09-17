package com.scribe.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ── Scribe Clean Cobalt & Ice Blue Palette ──────────────────────────────────
// Obsidian slate base, elevated dark glass, crisp white typography, razor-thin hairline borders,
// and radiant Cobalt / Ice Blue precision accents.

val ScribeBackground = Color(0xFF090B10)        // Pure Midnight Obsidian Slate
val ScribeSurface = Color(0xFF11141D)           // Elevated Dark Slate Glass
val ScribeSurfaceVariant = Color(0xFF171B26)    // Interactive Cards / Containers
val ScribeSurfaceHighlight = Color(0xFF22283A)  // Active / Focused Container
val ScribeOutline = Color(0x1FFFFFFF)           // Delicate 12% White Hairline
val ScribeOutlineAccent = Color(0x4D38BDF8)     // Subtle Ice Blue Glow (30%)

// Precision Jewel Accents
val ScribeCobalt = Color(0xFF2563EB)            // Deep Vibrant Cobalt Blue
val ScribeIce = Color(0xFF38BDF8)               // Crisp Electric Sky / Ice Blue
val ScribeCyan = Color(0xFF06B6D4)              // Secondary Cyan
val ScribeEmerald = Color(0xFF10B981)           // Active System Status Green
val ScribeRose = Color(0xFFF43F5E)              // Danger / Delete Crimson
val ScribeAmber = Color(0xFFF59E0B)             // Warning Amber

// Typography
val ScribeTextPrimary = Color(0xFFF8FAFC)       // Crisp Ice White
val ScribeTextSecondary = Color(0xFF94A3B8)     // Cool Slate Gray
val ScribeTextTertiary = Color(0xFF64748B)      // Muted Slate

// ── Backward Compatibility Aliases ──────────────────────────────────────────
val ScribeGold = ScribeIce
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

private val ScribeColorScheme = darkColorScheme(
    background = ScribeBackground,
    surface = ScribeSurface,
    surfaceVariant = ScribeSurfaceVariant,
    surfaceContainerHigh = ScribeSurfaceHighlight,
    onBackground = ScribeTextPrimary,
    onSurface = ScribeTextPrimary,
    onSurfaceVariant = ScribeTextSecondary,
    outline = ScribeOutline,
    outlineVariant = ScribeOutlineAccent,
    primary = ScribeIce,
    onPrimary = Color(0xFF090B10),
    primaryContainer = Color(0x2638BDF8),
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = ScribeCobalt,
    onSecondary = Color.White,
    secondaryContainer = Color(0x262563EB),
    onSecondaryContainer = Color(0xFFDBEAFE),
    error = ScribeRose,
    tertiary = ScribeEmerald,
    tertiaryContainer = Color(0x2610B981)
)

@Composable
fun ScribeTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = ScribeColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}