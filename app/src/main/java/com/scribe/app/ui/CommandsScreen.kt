package com.scribe.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.RestartAlt
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scribe.app.manager.CommandManager
import com.scribe.app.model.Command
import com.scribe.app.ui.components.ScribeBrandHeader
import com.scribe.app.ui.components.ScribeCard
import com.scribe.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandsScreen() {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val commandManager = remember { CommandManager(context) }
    var commands by remember { mutableStateOf(commandManager.getCommands()) }

    var showSheet by remember { mutableStateOf(false) }
    var editingCommandTrigger by remember { mutableStateOf<String?>(null) }
    var triggerInput by remember { mutableStateOf("") }
    var promptInput by remember { mutableStateOf("") }
    var isTextReplacer by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val currentPrefix = remember(commands) { commandManager.getTriggerPrefix() }

    fun openSheet(command: Command? = null) {
        errorMessage = null
        if (command != null) {
            editingCommandTrigger = command.trigger
            triggerInput = command.trigger.removePrefix(currentPrefix)
            promptInput = command.prompt
            isTextReplacer = command.isTextReplacer
        } else {
            editingCommandTrigger = null
            triggerInput = ""
            promptInput = ""
            isTextReplacer = false
        }
        showSheet = true
    }

    fun closeSheet() {
        showSheet = false
        editingCommandTrigger = null
        triggerInput = ""
        promptInput = ""
        isTextReplacer = false
        errorMessage = null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp)
        ) {
            ScribeBrandHeader(
                title = "Commands",
                subtitle = "${commands.size} active shortcut${if (commands.size == 1) "" else "s"}",
                trailingContent = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showResetDialog = true
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color(0x26EF4444),
                                contentColor = ScribeTerracotta
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = "Reset Defaults",
                                tint = ScribeTerracotta,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        FilledTonalButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                openSheet()
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = ScribeCobalt.copy(alpha = 0.2f),
                                contentColor = ScribeGold
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Command",
                                tint = ScribeGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(6.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                items(commands) { cmd ->
                    CommandCard(
                        cmd = cmd,
                        onEdit = { openSheet(cmd) },
                        onDelete = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            commandManager.removeCommand(cmd.trigger)
                            commands = commandManager.getCommands()
                        }
                    )
                }
            }
        }
    }

    // ── Bottom Sheet ──────────────────────────────────────────────────────────
    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { closeSheet() },
            sheetState = sheetState,
            containerColor = ScribeSurface,
            contentColor = ScribeParchment,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .size(width = 40.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(ScribeOutline)
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (editingCommandTrigger != null) "Edit Shortcut" else "New Shortcut",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        letterSpacing = (-0.3).sp,
                        color = ScribeParchment
                    )
                }

                // Type selector
                Text(
                    text = "COMMAND TYPE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = ScribeParchmentMuted
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TypeSelectChip(
                        label = "AI Transformation",
                        selected = !isTextReplacer,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            isTextReplacer = false
                        },
                        modifier = Modifier.weight(1f)
                    )
                    TypeSelectChip(
                        label = "Text Snippet",
                        selected = isTextReplacer,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            isTextReplacer = true
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Trigger field
                Text(
                    text = "TRIGGER SHORTCUT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = ScribeParchmentMuted
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = triggerInput,
                    onValueChange = {
                        triggerInput = it
                        errorMessage = null
                    },
                    placeholder = { Text("e.g. fix, polish, formal", color = ScribeParchmentMuted) },
                    prefix = {
                        Text(
                            currentPrefix,
                            fontWeight = FontWeight.Bold,
                            color = ScribeGold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 16.sp
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ScribeGold,
                        unfocusedBorderColor = ScribeOutline,
                        focusedContainerColor = ScribeSurfaceVariant,
                        unfocusedContainerColor = ScribeSurfaceVariant,
                        focusedTextColor = ScribeParchment,
                        unfocusedTextColor = ScribeParchment
                    )
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Prompt / Replacement field
                Text(
                    text = if (isTextReplacer) "SNIPPET TEXT" else "AI INSTRUCTION PROMPT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = ScribeParchmentMuted
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = promptInput,
                    onValueChange = {
                        promptInput = it
                        errorMessage = null
                    },
                    placeholder = {
                        Text(
                            if (isTextReplacer) "Text to automatically replace the trigger with..."
                            else "Instructions for how the AI should rewrite the text...",
                            color = ScribeParchmentMuted
                        )
                    },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ScribeGold,
                        unfocusedBorderColor = ScribeOutline,
                        focusedContainerColor = ScribeSurfaceVariant,
                        unfocusedContainerColor = ScribeSurfaceVariant,
                        focusedTextColor = ScribeParchment,
                        unfocusedTextColor = ScribeParchment
                    )
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = ScribeTerracotta,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { closeSheet() },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ScribeOutline),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = ScribeParchment
                        )
                    ) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                    }

                    val canSave = triggerInput.isNotBlank() && promptInput.isNotBlank()
                    Button(
                        onClick = {
                            val rawTrigger = triggerInput.trim()
                            val fullTrigger = if (rawTrigger.startsWith(currentPrefix)) rawTrigger else "$currentPrefix$rawTrigger"
                            val prompt = promptInput.trim()

                            if (rawTrigger.isBlank()) {
                                errorMessage = "Trigger cannot be empty"
                                return@Button
                            }
                            if (prompt.isBlank()) {
                                errorMessage = if (isTextReplacer) "Replacement text cannot be empty" else "Prompt cannot be empty"
                                return@Button
                            }

                            val isDuplicate = commands.any {
                                it.trigger.equals(fullTrigger, ignoreCase = true) && it.trigger != editingCommandTrigger
                            }
                            if (isDuplicate) {
                                errorMessage = "A command with this trigger already exists"
                                return@Button
                            }

                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val newCmd = Command(
                                trigger = fullTrigger,
                                prompt = prompt,
                                isBuiltIn = false,
                                isTextReplacer = isTextReplacer
                            )

                            if (editingCommandTrigger != null) {
                                commandManager.updateCommand(editingCommandTrigger!!, newCmd)
                            } else {
                                commandManager.addCustomCommand(newCmd)
                            }

                            commands = commandManager.getCommands()
                            closeSheet()
                        },
                        enabled = canSave,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ScribeGold,
                            contentColor = ScribeBackground,
                            disabledContainerColor = ScribeSurfaceVariant,
                            disabledContentColor = ScribeParchmentDim
                        )
                    ) {
                        Text(
                            if (editingCommandTrigger != null) "Update" else "Save Shortcut",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // ── Reset Defaults Confirmation Dialog ──────────────────────────────────
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text(
                    text = "Reset Default Commands?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = ScribeParchment
                )
            },
            text = {
                Text(
                    text = "This will restore all 9 original built-in shortcuts and reset any prompt customizations. Your custom commands will be preserved.",
                    fontSize = 13.sp,
                    color = ScribeParchmentMuted,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        commandManager.resetBuiltInCommands()
                        commands = commandManager.getCommands()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ScribeTerracotta, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Reset Defaults", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = ScribeParchmentMuted)
                ) {
                    Text("Cancel")
                }
            },
            containerColor = ScribeSurface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun CommandCard(
    cmd: Command,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = if (cmd.isBuiltIn) "Delete Default Shortcut?" else "Delete Shortcut?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = ScribeParchment
                )
            },
            text = {
                Text(
                    text = if (cmd.isBuiltIn)
                        "You are deleting the default \"${cmd.trigger}\" command. You can restore it anytime using the \"Reset Defaults\" button."
                    else
                        "Are you sure you want to delete \"${cmd.trigger}\"? This action cannot be undone.",
                    fontSize = 13.sp,
                    color = ScribeParchmentMuted,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ScribeTerracotta, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = ScribeParchmentMuted)
                ) {
                    Text("Cancel")
                }
            },
            containerColor = ScribeSurface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    ScribeCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                isExpanded = !isExpanded
            },
        border = BorderStroke(
            1.dp,
            if (isExpanded) ScribeIce.copy(alpha = 0.5f) else ScribeOutline
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Monospace trigger pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isExpanded) ScribeOutline else ScribeSurfaceVariant)
                        .border(1.dp, if (isExpanded) ScribeGold else ScribeOutline, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = cmd.trigger,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = ScribeGold
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Action buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!(cmd.isBuiltIn && cmd.trigger.endsWith("undo", ignoreCase = true))) {
                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onEdit()
                        }) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = ScribeParchmentMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showDeleteDialog = true
                    }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = ScribeTerracotta.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        isExpanded = !isExpanded
                    }) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = if (isExpanded) ScribeGold else ScribeParchmentDim,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ScribeSurfaceVariant)
                        .border(1.dp, ScribeOutline, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = cmd.prompt,
                        fontSize = 13.sp,
                        color = ScribeParchmentMuted,
                        lineHeight = 19.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            when {
                                cmd.isBuiltIn -> TypeBadge(
                                    label = "Built-in",
                                    containerColor = Color(0x2610B981),
                                    contentColor = ScribeSage
                                )
                                cmd.isTextReplacer -> TypeBadge(
                                    label = "Text Snippet",
                                    containerColor = Color(0x26F59E0B),
                                    contentColor = ScribeAmber
                                )
                                else -> TypeBadge(
                                    label = "AI Prompt",
                                    containerColor = ScribeCobalt.copy(alpha = 0.2f),
                                    contentColor = ScribeGold
                                )
                            }
                        }

                        // Copy prompt button
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("Scribe Prompt", cmd.prompt)
                                clipboard.setPrimaryClip(clip)
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                android.widget.Toast.makeText(context, "Prompt copied!", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copy Prompt",
                                tint = ScribeParchmentDim,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TypeBadge(
    label: String,
    containerColor: Color,
    contentColor: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(containerColor)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = contentColor
        )
    }
}

@Composable
private fun TypeSelectChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (selected) ScribeCobalt.copy(alpha = 0.2f) else ScribeSurfaceVariant
    val borderColor = if (selected) ScribeGold else ScribeOutline
    val textColor = if (selected) ScribeGold else ScribeParchmentMuted

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = textColor
        )
    }
}