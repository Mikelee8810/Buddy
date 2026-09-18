package com.scribe.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.format.DateUtils
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scribe.app.manager.HistoryManager
import com.scribe.app.model.HistoryItem
import com.scribe.app.ui.components.ScribeBrandHeader
import com.scribe.app.ui.components.glassCard
import com.scribe.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val historyManager = remember { HistoryManager(context) }
    var historyItems by remember { mutableStateOf(historyManager.getHistory()) }
    var showClearDialog by remember { mutableStateOf(false) }

    var selectedItemForDetail by remember { mutableStateOf<HistoryItem?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp)
        ) {
            // ── Editorial Header ──────────────────────────────────────────────
            ScribeBrandHeader(
                title = "History",
                subtitle = if (historyItems.isNotEmpty()) "${historyItems.size} logged events" else "Audit Log",
                trailingContent = {
                    if (historyItems.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showClearDialog = true
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.14f))
                                .border(1.dp, Color.White.copy(alpha = 0.30f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear History",
                                tint = ScribeGlassRose,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            )

            if (historyItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp),
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
                                .background(Color.White.copy(alpha = 0.14f))
                                .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = ScribeGlassTextSecondary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No history recorded yet",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ScribeGlassTextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Transformations executed in any app\nwill be logged here automatically.",
                            fontSize = 13.sp,
                            color = ScribeGlassTextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .glassCard(cornerRadius = 24.dp, fillAlpha = 0.17f)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)
                    ) {
                        itemsIndexed(historyItems, key = { _, item -> item.id }) { index, item ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedItemForDetail = item
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp)
                            ) {
                                // Top row: trigger badge + timestamp + chevron
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Glass Cobalt Monospace Trigger Badge
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(ScribeGlassCobalt.copy(alpha = 0.22f))
                                                .border(1.dp, ScribeGlassCobalt.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = item.commandTrigger,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = ScribeGlassCobalt
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
                                            color = ScribeGlassTextTertiary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.Outlined.ChevronRight,
                                        contentDescription = "View detail",
                                        tint = ScribeGlassTextTertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Transformed preview text
                                Text(
                                    text = item.newText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = ScribeGlassTextPrimary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 18.sp
                                )
                            }

                            if (index < historyItems.size - 1) {
                                HorizontalDivider(
                                    thickness = 0.5.dp,
                                    color = ScribeGlassDivider,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Tap-to-Inspect Detail Bottom Sheet (Liquid Glass) ──────────────────
        if (selectedItemForDetail != null) {
            val item = selectedItemForDetail!!
            val dateFormatted = remember(item.timestamp) {
                SimpleDateFormat("MMM dd, yyyy • h:mm:ss a", Locale.getDefault()).format(Date(item.timestamp))
            }

            ModalBottomSheet(
                onDismissRequest = { selectedItemForDetail = null },
                sheetState = sheetState,
                containerColor = Color(0xFF101633),
                contentColor = ScribeGlassTextPrimary,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(top = 12.dp, bottom = 8.dp)
                            .size(width = 36.dp, height = 4.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.35f))
                    )
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 36.dp)
                ) {
                    // Sheet Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ScribeGlassCobalt.copy(alpha = 0.25f))
                                    .border(1.dp, ScribeGlassCobalt.copy(alpha = 0.50f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = item.commandTrigger,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = ScribeGlassCobalt
                                )
                            }
                            Text(
                                text = "Audit Record",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = ScribeGlassTextPrimary
                            )
                        }

                        Text(
                            text = dateFormatted,
                            fontSize = 11.sp,
                            color = ScribeGlassTextTertiary
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Original Input Section
                    Text(
                        text = "ORIGINAL INPUT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = ScribeGlassTextTertiary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassCard(cornerRadius = 14.dp, fillAlpha = 0.12f)
                            .padding(14.dp)
                    ) {
                        Text(
                            text = item.originalText,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            color = ScribeGlassTextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Transformed Result Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TRANSFORMED OUTPUT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = ScribeGlassEmerald
                        )

                        TextButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Scribe Output", item.newText)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = ScribeGlassEmerald,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy", fontSize = 12.sp, color = ScribeGlassEmerald, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(ScribeGlassEmerald.copy(alpha = 0.10f))
                            .border(1.dp, ScribeGlassEmerald.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = item.newText,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = ScribeGlassTextPrimary,
                            fontWeight = FontWeight.Normal
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Action Buttons (Copy Output + Safe Delete)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                historyManager.deleteItem(item.id)
                                historyItems = historyManager.getHistory()
                                selectedItemForDetail = null
                                Toast.makeText(context, "Record removed", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, ScribeGlassRose.copy(alpha = 0.45f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = ScribeGlassRose
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delete Record", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Scribe Output", item.newText)
                                clipboard.setPrimaryClip(clip)
                                selectedItemForDetail = null
                                Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ScribeGlassCobalt.copy(alpha = 0.85f),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Output", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // ── Clear All Confirmation Dialog ──────────────────────────────────────
        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                containerColor = Color(0xFF101633),
                title = {
                    Text(
                        "Clear Transformation History?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = ScribeGlassTextPrimary
                    )
                },
                text = {
                    Text(
                        "Are you sure you want to delete all audit logs? This action cannot be undone.",
                        fontSize = 14.sp,
                        color = ScribeGlassTextSecondary
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
                        Text("Clear All", color = ScribeGlassRose, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) {
                        Text("Cancel", color = ScribeGlassTextTertiary)
                    }
                },
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}
