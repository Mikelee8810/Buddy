package com.scribe.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.scribe.app.ui.theme.*

// ── Aurora Background ─────────────────────────────────────────────────────────
// Renders the full-screen aurora mesh gradient (navy + cobalt + purple + teal).
// Wrap the root of ScribeMainScreen in this to paint once behind all screens.

@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                // Base: deep navy
                drawRect(ScribeAuroraBase)

                // Cobalt orb — left-center
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ScribeAuroraCobalt.copy(alpha = 0.82f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.14f, size.height * 0.43f),
                        radius = size.width * 0.78f
                    ),
                    radius  = size.width * 0.78f,
                    center  = Offset(size.width * 0.14f, size.height * 0.43f)
                )

                // Purple orb — right-center
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ScribeAuroraPurple.copy(alpha = 0.70f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.86f, size.height * 0.47f),
                        radius = size.width * 0.62f
                    ),
                    radius = size.width * 0.62f,
                    center = Offset(size.width * 0.86f, size.height * 0.47f)
                )

                // Teal orb — bottom
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ScribeAuroraTeal.copy(alpha = 0.68f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.50f, size.height * 0.87f),
                        radius = size.width * 0.88f
                    ),
                    radius = size.width * 0.88f,
                    center = Offset(size.width * 0.50f, size.height * 0.87f)
                )
            },
        content = content
    )
}

// ── Glass Card Modifier ───────────────────────────────────────────────────────
// Applies frosted-glass look: semi-transparent white fill + specular top border.
// Use on any Box/Surface that should appear as a glass panel floating on aurora.

fun Modifier.glassCard(
    cornerRadius: Dp = 24.dp,
    fillAlpha: Float = 0.19f
): Modifier = this
    .background(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = fillAlpha),
                Color.White.copy(alpha = fillAlpha * 0.58f)
            )
        ),
        shape = RoundedCornerShape(cornerRadius)
    )
    .border(
        width = 1.dp,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.54f),   // specular top highlight
                Color.White.copy(alpha = 0.07f)    // fades to near-invisible bottom
            )
        ),
        shape = RoundedCornerShape(cornerRadius)
    )

// ── Glass Surface ─────────────────────────────────────────────────────────────
// Convenience composable that wraps content in a glass panel with correct padding.

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    fillAlpha: Float = 0.19f,
    innerPadding: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .glassCard(cornerRadius = cornerRadius, fillAlpha = fillAlpha)
            .padding(innerPadding),
        content = content
    )
}

// ── Glass Pill ────────────────────────────────────────────────────────────────
// Fully-rounded glass pill — for trigger badges, model labels, small chips.

fun Modifier.glassPill(fillAlpha: Float = 0.22f): Modifier =
    glassCard(cornerRadius = 100.dp, fillAlpha = fillAlpha)

// ── Cobalt Pill ───────────────────────────────────────────────────────────────
// Solid cobalt-tinted glass pill for active/selected states.

fun Modifier.cobaltPill(): Modifier = this
    .background(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF3B82F6).copy(alpha = 0.55f),
                Color(0xFF1D4ED8).copy(alpha = 0.45f)
            )
        ),
        shape = RoundedCornerShape(100.dp)
    )
    .border(
        width = 1.dp,
        color = Color(0xFF60A5FA).copy(alpha = 0.60f),
        shape = RoundedCornerShape(100.dp)
    )
