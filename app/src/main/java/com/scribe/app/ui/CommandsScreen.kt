package com.scribe.app.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scribe.app.manager.CommandManager
import com.scribe.app.model.Command
import com.scribe.app.ui.components.ScribeBrandHeader
import com.scribe.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandsScreen() {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val clipboard = LocalClipboardManager.current
    val commandManager = remember { CommandManager(context) }
    var commands by remember { mutableStateOf(commandManager.getCommands()) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }

    var selectedCommandForDetail by remember { mutableStateOf<Command?>(null) }
    var showEditSheet by remember { mutableStateOf(false) }
    var isCreatingNew by remember { mutableStateOf(false) }

    var editingTrigger by remember { mutableStateOf("") }
    var editingPrompt by remember { mutableStateOf("") }
    var editingIsReplacer by remember { mutableStateOf(false) }
    var originalTrigger by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf<Command?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val currentPrefix = remember(commands) { commandManager.getTriggerPrefix() }

    val filteredCommands = remember(commands, searchQuery, selectedFilter) {
        commands.filter { cmd ->
            val matchesSearch = searchQuery.isBlank() ||
                cmd.trigger.contains(searchQuery, ignoreCase = true) ||
                cmd.prompt.contains(searchQuery, ignoreCase = true)
            
            val matchesFilter = when (selectedFilter) {
                "AI" -> !cmd.isTextReplacer
                "Instant" -> cmd.isTextReplacer
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    fun openCreateSheet() {
        errorMessage = null
        isCreatingNew = true
        originalTrigger = null
        editingTrigger = ""
        editingPrompt = ""
        editingIsReplacer = false
        showEditSheet = true
    }

    fun openEditSheet(cmd: Command) {
        errorMessage = null
        isCreatingNew = false
        originalTrigger = cmd.trigger
        editingTrigger = cmd.trigger.removePrefix(currentPrefix)
        editingPrompt = cmd.prompt
        editingIsReplacer = cmd.isTextReplacer
        showEditSheet = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScribeBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp)
        ) {
            // ── Editorial Header ──────────────────────────────────────────────
            ScribeBrandHeader(
                title = "Snippets",
                subtitle = "${commands.size} active shortcuts",
                trailingContent = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showResetDialog = true
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(ScribeSurfaceVariant)
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = "Reset Defaults",
                                tint = ScribeTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Luxury pill CTA button
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                openCreateSheet()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF0F1535)
                            ),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New",
                                tint = Color(0xFF0F1535),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "New",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F1535)
                            )
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // ── Search Bar (Spotlight / Apple Style) ───────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ScribeSurfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = ScribeTextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                "Search triggers or instructions...",
                                fontSize = 14.sp,
                                color = ScribeTextTertiary
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            cursorColor = ScribeCobalt,
                            focusedTextColor = ScribeTextPrimary,
                            unfocusedTextColor = ScribeTextPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = ScribeTextTertiary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Filter Segmented Tabs ─────────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val filters = listOf(
                    "All" to commands.size,
                    "AI" to commands.count { !it.isTextReplacer },
                    "Instant" to commands.count { it.isTextReplacer }
                )
                filters.forEach { (filterName, count) ->
                    val isSelected = selectedFilter == filterName
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) ScribeCobalt.copy(alpha = 0.1f) else ScribeSurfaceVariant)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedFilter = filterName
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = filterName,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (isSelected) ScribeCobalt else ScribeTextSecondary
                            )
                            Text(
                                text = count.toString(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal,
                                color = if (isSelected) ScribeCobalt.copy(alpha = 0.8f) else ScribeTextTertiary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── Snippets List (Open Apple Inset Group) ─────────────────────────
            if (filteredCommands.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(ScribeSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = ScribeTextTertiary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Text(
                            text = "No snippets found",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ScribeTextPrimary
                        )
                        Text(
                            text = if (searchQuery.isNotBlank()) "No shortcuts matching \"$searchQuery\"" else "Add your first snippet to get started",
                            fontSize = 13.sp,
                            color = ScribeTextSecondary
                        )
                    }
                }
            } else {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = ScribeSurface,
                    border = BorderStroke(0.5.dp, ScribeOutline),
                    shadowElevation = 0.5.dp
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 6.dp, bottom = 120.dp)
                    ) {
                        itemsIndexed(filteredCommands, key = { _, cmd -> cmd.trigger }) { index, cmd ->
                            RaycastCommandRow(
                                command = cmd,
                                onTap = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedCommandForDetail = cmd
                                }
                            )
                            if (index < filteredCommands.size - 1) {
                                HorizontalDivider(
                                    thickness = 0.5.dp,
                                    color = ScribeOutline,
                                    modifier = Modifier.padding(start = 48.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Snippet Detail / Inspector Bottom Sheet ───────────────────────────────
    selectedCommandForDetail?.let { cmd ->
        ModalBottomSheet(
            onDismissRequest = { selectedCommandForDetail = null },
            sheetState = sheetState,
            containerColor = ScribeBottomSheetContainer,
            contentColor = ScribeTextPrimary,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
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
                // Header with Badge & Type
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(ScribeTextPrimary)
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = cmd.trigger,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (cmd.isTextReplacer) ScribeSurfaceVariant else ScribeCobalt.copy(alpha = 0.1f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (cmd.isTextReplacer) Icons.Default.TextFields else Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = if (cmd.isTextReplacer) ScribeTextSecondary else ScribeCobalt,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (cmd.isTextReplacer) "Instant Replace" else "AI Rewrite",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (cmd.isTextReplacer) ScribeTextSecondary else ScribeCobalt
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Prompt Container
                Text(
                    text = if (cmd.isTextReplacer) "REPLACEMENT CONTENT" else "AI INSTRUCTION PROMPT",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = ScribeTextTertiary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(ScribeSurfaceVariant)
                        .border(1.dp, ScribeOutline, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = cmd.prompt,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        color = ScribeTextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Actions: Copy, Edit, Delete
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            clipboard.setText(AnnotatedString(cmd.prompt))
                        },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, ScribeOutline),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ScribeTextPrimary),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            selectedCommandForDetail = null
                            openEditSheet(cmd)
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ScribeCobalt,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    IconButton(
                        onClick = {
                            showDeleteConfirmDialog = cmd
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(ScribeRose.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = ScribeRose,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    // ── Edit / Create Modal Sheet ─────────────────────────────────────────────
    if (showEditSheet) {
        ModalBottomSheet(
            onDismissRequest = { showEditSheet = false },
            sheetState = sheetState,
            containerColor = ScribeBottomSheetContainer,
            contentColor = ScribeTextPrimary,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
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
                Text(
                    text = if (isCreatingNew) "New Snippet" else "Edit Snippet",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = ScribeTextPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Mode Selector Segmented Pill
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(ScribeSurfaceVariant)
                        .padding(4.dp)
                ) {
                    val isAI = !editingIsReplacer
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isAI) ScribeSurface else Color.Transparent)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                editingIsReplacer = false
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = if (isAI) ScribeCobalt else ScribeTextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "AI Rewrite",
                                fontSize = 12.sp,
                                fontWeight = if (isAI) FontWeight.Bold else FontWeight.Medium,
                                color = if (isAI) ScribeTextPrimary else ScribeTextSecondary
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (!isAI) ScribeSurface else Color.Transparent)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                editingIsReplacer = true
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TextFields,
                                contentDescription = null,
                                tint = if (!isAI) ScribeCobalt else ScribeTextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Instant Replace",
                                fontSize = 12.sp,
                                fontWeight = if (!isAI) FontWeight.Bold else FontWeight.Medium,
                                color = if (!isAI) ScribeTextPrimary else ScribeTextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Trigger Input
                Text(
                    text = "TRIGGER SHORTCUT",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = ScribeTextTertiary
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = editingTrigger,
                    onValueChange = {
                        editingTrigger = it
                        errorMessage = null
                    },
                    prefix = {
                        Text(
                            currentPrefix,
                            fontWeight = FontWeight.Bold,
                            color = ScribeTextPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 15.sp
                        )
                    },
                    placeholder = { Text("fix, email, reply", color = ScribeTextTertiary) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ScribeCobalt,
                        unfocusedBorderColor = ScribeOutline,
                        focusedContainerColor = ScribeSurfaceVariant,
                        unfocusedContainerColor = ScribeSurfaceVariant,
                        cursorColor = ScribeCobalt,
                        focusedTextColor = ScribeTextPrimary,
                        unfocusedTextColor = ScribeTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Prompt Input
                Text(
                    text = if (editingIsReplacer) "REPLACEMENT TEXT" else "PROMPT INSTRUCTION",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = ScribeTextTertiary
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = editingPrompt,
                    onValueChange = {
                        editingPrompt = it
                        errorMessage = null
                    },
                    placeholder = {
                        Text(
                            if (editingIsReplacer) "Text to automatically replace trigger..."
                            else "How the AI should rewrite or transform the text...",
                            color = ScribeTextTertiary
                        )
                    },
                    minLines = 3,
                    maxLines = 6,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ScribeCobalt,
                        unfocusedBorderColor = ScribeOutline,
                        focusedContainerColor = ScribeSurfaceVariant,
                        unfocusedContainerColor = ScribeSurfaceVariant,
                        cursorColor = ScribeCobalt,
                        focusedTextColor = ScribeTextPrimary,
                        unfocusedTextColor = ScribeTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Dynamic Token Chips
                Column {
                    Text(
                        text = "DYNAMIC TOKENS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = ScribeTextTertiary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val tokens = listOf(
                            "{date}" to "Current Date",
                            "{time}" to "Current Time",
                            "{clipboard}" to "Clipboard Text",
                            "{selection}" to "Input Text"
                        )
                        tokens.forEach { (token, _) ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = ScribeSurfaceVariant,
                                border = BorderStroke(1.dp, ScribeOutline),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        editingPrompt = if (editingPrompt.isEmpty()) token else "$editingPrompt $token"
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "+",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = ScribeCobalt
                                    )
                                    Text(
                                        text = token,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = ScribeTextPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = ScribeRose,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Save / Cancel Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showEditSheet = false },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, ScribeOutline),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ScribeTextPrimary),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                    }

                    val canSave = editingTrigger.isNotBlank() && editingPrompt.isNotBlank()
                    Button(
                        onClick = {
                            val raw = editingTrigger.trim()
                            val fullTrigger = if (raw.startsWith(currentPrefix)) raw else "$currentPrefix$raw"
                            val prompt = editingPrompt.trim()

                            if (raw.isBlank() || prompt.isBlank()) {
                                errorMessage = "Trigger and content cannot be empty"
                                return@Button
                            }

                            val isDuplicate = commands.any {
                                it.trigger.equals(fullTrigger, ignoreCase = true) && it.trigger != originalTrigger
                            }
                            if (isDuplicate) {
                                errorMessage = "Shortcut with trigger \"$fullTrigger\" already exists"
                                return@Button
                            }

                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val newCmd = Command(
                                trigger = fullTrigger,
                                prompt = prompt,
                                isBuiltIn = false,
                                isTextReplacer = editingIsReplacer
                            )

                            if (originalTrigger != null) {
                                commandManager.updateCommand(originalTrigger!!, newCmd)
                            } else {
                                commandManager.addCustomCommand(newCmd)
                            }

                            commands = commandManager.getCommands()
                            showEditSheet = false
                        },
                        enabled = canSave,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF0F1535)
                        ),
                        modifier = Modifier.weight(1.5f).height(48.dp)
                    ) {
                        Text(
                            text = if (isCreatingNew) "Create Snippet" else "Save Changes",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F1535)
                        )
                    }
                }
            }
        }
    }

    // ── Delete Confirmation Dialog ────────────────────────────────────────────
    showDeleteConfirmDialog?.let { cmd ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = {
                Text(
                    text = "Delete \"${cmd.trigger}\"?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = ScribeGlassTextPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove this snippet? This action cannot be undone.",
                    fontSize = 13.sp,
                    color = ScribeGlassTextSecondary,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        commandManager.removeCommand(cmd.trigger)
                        commands = commandManager.getCommands()
                        showDeleteConfirmDialog = null
                        selectedCommandForDetail = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ScribeRose,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmDialog = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = ScribeGlassTextSecondary)
                ) {
                    Text("Cancel", color = ScribeGlassTextSecondary)
                }
            },
            containerColor = Color(0xF4101633),
            shape = RoundedCornerShape(20.dp)
        )
    }

    // ── Reset Defaults Confirmation Dialog ────────────────────────────────────
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text(
                    text = "Reset Built-in Snippets?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = ScribeGlassTextPrimary
                )
            },
            text = {
                Text(
                    text = "This will restore the original built-in shortcuts while keeping your custom ones intact.",
                    fontSize = 13.sp,
                    color = ScribeGlassTextSecondary,
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF0F1535)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Reset", fontWeight = FontWeight.Bold, color = Color(0xFF0F1535))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = ScribeGlassTextSecondary)
                ) {
                    Text("Cancel", color = ScribeGlassTextSecondary)
                }
            },
            containerColor = Color(0xF4101633),
            shape = RoundedCornerShape(20.dp)
        )
    }
}

/**
 * Raycast Command Palette Row
 * Dense, tactile row with human-curated titles, monospace hotkey chips, and subtle metadata.
 */
@Composable
private fun RaycastCommandRow(
    command: Command,
    onTap: () -> Unit
) {
    val friendlyTitle = when (command.trigger.removePrefix("/").lowercase()) {
        "fix" -> "Proofread & Fix Grammar"
        "improve" -> "Clarity & Readability"
        "shorten" -> "Make Concise & Punchy"
        "expand" -> "Elaborate with Detail"
        "formal" -> "Executive & Professional"
        "casual" -> "Conversational & Friendly"
        "emoji" -> "Add Expressive Emojis"
        "reply" -> "Contextual Reply"
        "undo" -> "Restore Original Text"
        else -> command.trigger.removePrefix("/")
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
    }

    val subtitle = when {
        command.isTextReplacer -> command.prompt
        command.isBuiltIn -> when (command.trigger.removePrefix("/").lowercase()) {
            "fix" -> "Correct spelling, typos & grammar while preserving tone"
            "improve" -> "Elevate flow and clarity without changing meaning"
            "shorten" -> "Condense core thoughts into punchy sentences"
            "expand" -> "Add thoughtful context, examples, and depth"
            "formal" -> "Authoritative, polished executive prose"
            "casual" -> "Warm, friendly, everyday conversational tone"
            "emoji" -> "Tastefully sprinkle fitting emojis"
            "reply" -> "Draft a natural response to the message"
            "undo" -> "Revert the previous text transformation"
            else -> command.prompt.take(80)
        }
        else -> command.prompt.take(80)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTap() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Refined Monospace Token Tag (Calm, subtle porcelain chip)
        Box(
            modifier = Modifier
                .padding(end = 12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(ScribeSurfaceVariant)
                .padding(horizontal = 7.dp, vertical = 3.dp)
        ) {
            Text(
                text = command.trigger,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                color = ScribeCobalt
            )
        }

        // Title and description
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = friendlyTitle,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = ScribeTextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = ScribeTextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Instant replacer indicator (only if text replacer, zero noise for AI)
        if (command.isTextReplacer) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = ScribeSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp)
            ) {
                Text(
                    text = "EXPAND",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ScribeTextTertiary,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = ScribeTextTertiary,
            modifier = Modifier.size(15.dp)
        )
    }
}
