package com.scribe.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scribe.app.R
import com.scribe.app.ui.theme.*

@Composable
fun ScribeBrandHeader(
    title: String,
    subtitle: String? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f, fill = false)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Scribe glowing fountain pen nib emblem
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ScribeSurfaceVariant)
                        .border(
                            1.dp,
                            ScribeIce.copy(alpha = 0.35f),
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "Scribe Logo",
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp,
                    color = ScribeTextPrimary
                )
            }

            if (subtitle != null) {
                Text(
                    text = subtitle.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp,
                    color = ScribeTextSecondary,
                    modifier = Modifier.padding(start = 48.dp, top = 2.dp)
                )
            }
        }

        if (trailingContent != null) {
            Spacer(modifier = Modifier.width(12.dp))
            trailingContent()
        }
    }
}

@Composable
fun ScribeCard(
    modifier: Modifier = Modifier,
    border: BorderStroke = BorderStroke(1.dp, ScribeOutline),
    contentPadding: PaddingValues = PaddingValues(18.dp),
    backgroundColor: Color = ScribeSurface,
    borderColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val finalBorder = if (borderColor != null) BorderStroke(1.dp, borderColor) else border
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = backgroundColor,
        border = finalBorder,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

// Backward compatibility alias for SlateCard
@Composable
fun SlateCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) = ScribeCard(modifier = modifier, contentPadding = contentPadding, content = content)

@Composable
fun ScreenTitle(title: String) {
    ScribeBrandHeader(title = title)
}

@Composable
fun ScribeStatusBadge(
    label: String,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val dotColor = if (isActive) ScribeSage else ScribeAmber
    val bgColor = if (isActive) Color(0x1F10B981) else Color(0x1FF59E0B)
    val borderColor = if (isActive) Color(0x3310B981) else Color(0x33F59E0B)

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(bgColor)
            .border(1.dp, borderColor, CircleShape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = label.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            color = dotColor
        )
    }
}

@Composable
fun ScribeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isSecondary: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
    val bgBrush = if (isSecondary) {
        Brush.linearGradient(listOf(ScribeSurfaceVariant, ScribeSurfaceVariant))
    } else {
        Brush.linearGradient(listOf(ScribeCobalt, ScribeIce))
    }
    val borderStroke = if (isSecondary) BorderStroke(1.dp, ScribeOutline) else null
    val textColor = if (isSecondary) ScribeTextPrimary else Color.White

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .then(if (borderStroke != null) Modifier.border(borderStroke.width, borderStroke.brush, RoundedCornerShape(14.dp)) else Modifier)
            .background(bgBrush)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .padding(vertical = 15.dp, horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = text,
                color = textColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.2.sp
            )
        }
    }
}

@Composable
fun ScribePulsePip(
    isActive: Boolean = true,
    color: Color? = null,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val targetColor = color ?: if (isActive) ScribeEmerald else ScribeAmber
    val liveAlpha = if (isActive) alpha else 1.0f

    Box(
        modifier = modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(targetColor.copy(alpha = liveAlpha))
    )
}

@Composable
fun ScribeLatencyBadge(
    latencyMs: Long,
    modifier: Modifier = Modifier
) {
    val color = when {
        latencyMs < 500 -> ScribeEmerald
        latencyMs < 1200 -> ScribeIce
        else -> ScribeAmber
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "⚡",
            fontSize = 11.sp
        )
        Text(
            text = "${latencyMs}ms",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = color
        )
    }
}

@Composable
fun ScribeChip(
    text: String,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val bg = if (isSelected) ScribeCobalt.copy(alpha = 0.25f) else ScribeSurfaceVariant
    val border = if (isSelected) ScribeIce else ScribeOutline
    val textColor = if (isSelected) ScribeIce else ScribeTextSecondary

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(10.dp))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = textColor
        )
    }
}

@Composable
fun ScribeChip(
    text: String,
    containerColor: Color,
    contentColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(containerColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = contentColor
        )
    }
}
