package com.scribe.app.ui

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scribe.app.manager.HistoryManager
import com.scribe.app.ui.components.ScribeBrandHeader
import com.scribe.app.ui.components.ScribeCard
import com.scribe.app.ui.theme.*

@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val historyManager = remember { HistoryManager(context) }
    var historyItems by remember { mutableStateOf(historyManager.getHistory()) }
    var showClearDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp)
    ) {
        ScribeBrandHeader(
            title = "History",
            subtitle = if (historyItems.isNotEmpty()) "${historyItems.size} logged events" else "Audit Log",
            trailingContent = {
                if (historyItems.isNotEmpty()) {
                    FilledTonalButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showClearDialog = true
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = ScribeRose.copy(alpha = 0.1f),
                            contentColor = ScribeRose
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear History",
                            tint = ScribeRose,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear All", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        )

        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                containerColor = ScribeSurface,
                title = {
                    Text(
                        "Clear Transformation History?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = ScribeTextPrimary
                    )
                },
                text = {
                    Text(
                        "Are you sure you want to delete all history logs? This action cannot be undone.",
                        fontSize = 14.sp,
                        color = ScribeTextSecondary
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            historyManager.clearHistory()
                            historyItems = historyManager.getHistory()
                            showClearDialog = false
                        }
                    ) {
                        Text("Clear", color = ScribeRose, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) {
                        Text("Cancel", color = ScribeTextTertiary)
                    }
                },
                shape = RoundedCornerShape(18.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (historyItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(ScribeSurfaceVariant)
                            .border(1.dp, ScribeOutline, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = ScribeTextTertiary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No history recorded yet",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ScribeTextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Shortcuts and transforms you run in any app\nwill be logged here automatically.",
                        fontSize = 13.sp,
                        color = ScribeTextTertiary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                items(historyItems, key = { it.id }) { item ->
                    ScribeCard(
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(ScribeCobalt.copy(alpha = 0.1f))
                                            .border(1.dp, ScribeCobalt.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = item.commandTrigger,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = ScribeCobalt
                                        )
                                    }

                                    val timeAgo = DateUtils.getRelativeTimeSpanString(
                                        item.timestamp,
                                        System.currentTimeMillis(),
                                        DateUtils.MINUTE_IN_MILLIS
                                    )
                                    Text(
                                        text = timeAgo.toString(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = ScribeTextSecondary
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Input box preview
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(ScribeSurfaceVariant)
                                        .border(1.dp, ScribeOutline, RoundedCornerShape(10.dp))
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = "INPUT",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp,
                                        color = ScribeTextTertiary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = item.originalText,
                                        fontSize = 13.sp,
                                        color = ScribeTextSecondary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Output box preview
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(ScribeEmerald.copy(alpha = 0.08f))
                                        .border(1.dp, ScribeEmerald.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = "RESULT",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp,
                                        color = ScribeEmerald
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = item.newText,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = ScribeTextPrimary
                                    )
                                }
                            }

                            // Delete individual item button
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    historyManager.deleteItem(item.id)
                                    historyItems = historyManager.getHistory()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete entry",
                                    tint = ScribeTextTertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
