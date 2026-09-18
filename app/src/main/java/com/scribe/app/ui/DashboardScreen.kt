package com.scribe.app.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.scribe.app.api.ScribeApiClient
import com.scribe.app.manager.CommandManager
import com.scribe.app.manager.HistoryManager
import com.scribe.app.manager.KeyManager
import com.scribe.app.ui.components.ScribeBrandHeader
import com.scribe.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun formatModelTitle(raw: String): String {
    val clean = raw.substringAfterLast("/").removeSuffix(":free")
    return when {
        clean.contains("gemma-4-31b", ignoreCase = true) -> "Gemma 4 · 31B"
        clean.contains("gemma-2-9b", ignoreCase = true) -> "Gemma 2 · 9B"
        clean.contains("llama-3.3-70b", ignoreCase = true) -> "Llama 3.3 · 70B"
        clean.contains("deepseek-r1", ignoreCase = true) -> "DeepSeek R1"
        clean.contains("gemini-2.0-flash", ignoreCase = true) -> "Gemini 2.0"
        clean.contains("gemini-1.5-flash", ignoreCase = true) -> "Gemini 1.5"
        clean.length > 18 -> clean.take(16) + "…"
        else -> clean
    }
}

private fun checkServiceEnabled(context: Context): Boolean {
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
    return enabledServices.any {
        it.resolveInfo.serviceInfo.packageName == context.packageName
    }
}

@Composable
fun DashboardScreen() {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val keyManager = remember { KeyManager(context) }
    val commandManager = remember { CommandManager(context) }
    val historyManager = remember { HistoryManager(context) }

    var isServiceEnabled by remember { mutableStateOf(checkServiceEnabled(context)) }
    var keyCount by remember { mutableIntStateOf(keyManager.getKeys().size) }
    var currentPrefix by remember { mutableStateOf(commandManager.getTriggerPrefix()) }

    // Sample drafts for quick testing
    val sampleDrafts = listOf(
        "hey can we sync up real quick tomorrow morning to finalize that quarterly pitch deck and budget numbers?",
        "I was thinking about the product roadmap and we need to cut down technical debt before shipping the next big feature.",
        "Your order has been shipped and will arrive in 2 business days. Thank you for your business."
    )
    var sampleIndex by remember { mutableIntStateOf(0) }

    // Live Canvas State
    var canvasText by remember { mutableStateOf(sampleDrafts[0]) }
    var selectedCommandTrigger by remember { mutableStateOf("${currentPrefix}formal") }
    var isTransforming by remember { mutableStateOf(false) }
    var transformResult by remember { mutableStateOf<String?>(null) }
    var transformLatencyMs by remember { mutableLongStateOf(0L) }
    var transformError by remember { mutableStateOf<String?>(null) }

    val activityLifecycle = (context as? ComponentActivity)?.lifecycle

    // Provider settings
    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    val providerType = remember(keyCount) { prefs.getString("provider_type", "openrouter") ?: "openrouter" }
    val activeModelName = remember(providerType) {
        when (providerType) {
            "openrouter" -> prefs.getString("openrouter_model", "google/gemma-4-31b-it:free") ?: "google/gemma-4-31b-it:free"
            "groq"       -> prefs.getString("groq_model", "llama-3.3-70b-versatile") ?: "llama-3.3-70b-versatile"
            "custom"     -> prefs.getString("custom_model", "Custom Model") ?: "Custom Model"
            else         -> prefs.getString("model", "gemini-2.0-flash") ?: "gemini-2.0-flash"
        }
    }

    LaunchedEffect(activityLifecycle) {
        val lifecycle = activityLifecycle ?: return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                isServiceEnabled = checkServiceEnabled(context)
                keyCount = keyManager.getKeys().size
                currentPrefix = commandManager.getTriggerPrefix()
                delay(2500)
            }
        }
    }

    fun executeTransform(trigger: String) {
        if (keyCount == 0) {
            Toast.makeText(context, "Add an API key in the Engine tab first!", Toast.LENGTH_SHORT).show()
            return
        }
        if (canvasText.isBlank()) {
            Toast.makeText(context, "Please enter some text in the studio!", Toast.LENGTH_SHORT).show()
            return
        }

        isTransforming = true
        transformError = null
        transformResult = null
        selectedCommandTrigger = trigger

        coroutineScope.launch {
            val key = keyManager.getNextKey() ?: ""
            val rawPrompt = commandManager.getCommands().find { it.trigger.equals(trigger, ignoreCase = true) }?.prompt
                ?: when {
                    trigger.endsWith("formal") -> "Rewrite the following text into polished, authoritative, executive-ready prose. Output only the transformed text."
                    trigger.endsWith("fix") -> "Fix all spelling, grammar, punctuation, and typographical errors while preserving the original tone. Output only the fixed text."
                    trigger.endsWith("casual") -> "Rewrite the following text into friendly, natural, conversational prose. Output only the transformed text."
                    trigger.endsWith("shorten") -> "Condense the following text into punchy, clear sentences without losing key information. Output only the shortened text."
                    trigger.endsWith("expand") -> "Elaborate and provide vivid, thoughtful detail to the following text. Output only the expanded text."
                    else -> "Transform this text with precision."
                }
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clipText = try { clipboard?.primaryClip?.getItemAt(0)?.text?.toString() ?: "" } catch (_: Exception) { "" }
            val prompt = commandManager.resolveVariables(rawPrompt, selection = canvasText, clipboardText = clipText)
            val temperature = prefs.getFloat("temperature", 0.5f).toDouble()
            val startTime = System.currentTimeMillis()

            val result = withContext(Dispatchers.IO) {
                when (providerType) {
                    "openrouter" -> ScribeApiClient.openaiGenerate(
                        prompt = prompt,
                        text = canvasText,
                        apiKey = key,
                        model = activeModelName,
                        temperature = temperature,
                        endpoint = "https://openrouter.ai/api/v1"
                    )
                    "groq" -> ScribeApiClient.groqGenerate(
                        prompt = prompt,
                        text = canvasText,
                        apiKey = key,
                        model = activeModelName,
                        temperature = temperature
                    )
                    "custom" -> ScribeApiClient.openaiGenerate(
                        prompt = prompt,
                        text = canvasText,
                        apiKey = key,
                        model = activeModelName,
                        temperature = temperature,
                        endpoint = prefs.getString("custom_endpoint", "") ?: ""
                    )
                    else -> ScribeApiClient.geminiGenerate(
                        prompt = prompt,
                        text = canvasText,
                        apiKey = key,
                        model = activeModelName,
                        temperature = temperature
                    )
                }
            }

            val elapsed = System.currentTimeMillis() - startTime
            isTransforming = false
            transformLatencyMs = elapsed

            if (result.isSuccess) {
                val output = result.getOrThrow()
                transformResult = output
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                // Log to history audit
                historyManager.addHistoryItem(
                    originalText = canvasText,
                    newText = output,
                    commandTrigger = trigger
                )
            } else {
                transformError = result.exceptionOrNull()?.message ?: "Transformation failed"
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScribeBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 120.dp)
    ) {
        // ── 1. Top Identity & Workspace Header ─────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ScribeSurfaceVariant,
                    border = BorderStroke(0.5.dp, ScribeOutline),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.EditNote,
                            contentDescription = "Scribe",
                            tint = ScribeCobalt,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "Scribe",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        letterSpacing = (-0.3).sp,
                        color = ScribeTextPrimary
                    )
                    Text(
                        text = "Writing Studio",
                        fontSize = 12.sp,
                        color = ScribeTextSecondary
                    )
                }
            }

            // Subtle Model Chip (Calm, pill style, zero truncation)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ScribeSurfaceVariant,
                border = BorderStroke(0.5.dp, ScribeOutline)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (keyCount > 0) ScribeEmerald else ScribeTextTertiary)
                    )
                    Text(
                        text = formatModelTitle(activeModelName),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = ScribeTextPrimary
                    )
                }
            }
        }

        // ── 2. Calm Inline Service Notice (Only when paused) ───────────────────
        if (!isServiceEnabled) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                    .padding(vertical = 4.dp, horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(ScribeTextTertiary)
                    )
                    Text(
                        text = "Keyboard assistant service paused",
                        fontSize = 12.sp,
                        color = ScribeTextSecondary
                    )
                }
                Text(
                    text = "Turn on ↗",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ScribeCobalt
                )
            }
        }

        // ── 3. Draft Editor (Apple Notes Editorial Paper Canvas) ───────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = ScribeSurface,
            border = BorderStroke(0.5.dp, ScribeOutline),
            shadowElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Top Editor Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val words = if (canvasText.isBlank()) 0 else canvasText.trim().split(Regex("\\s+")).size
                    Text(
                        text = "$words words  ·  ${canvasText.length} chars",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = ScribeTextTertiary
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Sample",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = ScribeCobalt,
                            modifier = Modifier
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    sampleIndex = (sampleIndex + 1) % sampleDrafts.size
                                    canvasText = sampleDrafts[sampleIndex]
                                }
                        )

                        if (canvasText.isNotEmpty()) {
                            Text(
                                text = "Clear",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal,
                                color = ScribeTextTertiary,
                                modifier = Modifier
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        canvasText = ""
                                    }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Spacious Editor Input
                OutlinedTextField(
                    value = canvasText,
                    onValueChange = { canvasText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp),
                    placeholder = {
                        Text(
                            "What would you like to write or refine?",
                            color = ScribeTextTertiary,
                            fontSize = 17.sp,
                            lineHeight = 26.sp
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = ScribeTextPrimary,
                        unfocusedTextColor = ScribeTextPrimary,
                        cursorColor = ScribeCobalt
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 17.sp,
                        lineHeight = 26.sp,
                        color = ScribeTextPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── 4. iOS Segmented Command Bar ──────────────────────────────────────
        val modes = listOf(
            Triple("${currentPrefix}fix", "Proofread", Icons.Outlined.AutoAwesome),
            Triple("${currentPrefix}formal", "Professional", Icons.Outlined.WorkOutline),
            Triple("${currentPrefix}casual", "Friendly", Icons.Outlined.ChatBubbleOutline),
            Triple("${currentPrefix}shorten", "Concise", Icons.Outlined.Compress)
        )

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = ScribeSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(3.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                modes.forEach { (trigger, label, icon) ->
                    val isSelected = selectedCommandTrigger == trigger
                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = if (isSelected) ScribeSurface else Color.Transparent,
                        shadowElevation = if (isSelected) 1.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedCommandTrigger = trigger
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) ScribeCobalt else ScribeTextSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (isSelected) ScribeTextPrimary else ScribeTextSecondary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ── 5. Primary Action Button ──────────────────────────────────────────
        val selectedLabel = modes.find { it.first == selectedCommandTrigger }?.second ?: "Transform"
        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                executeTransform(selectedCommandTrigger)
            },
            enabled = !isTransforming && canvasText.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ScribeCobalt,
                contentColor = Color.White,
                disabledContainerColor = ScribeSurfaceVariant,
                disabledContentColor = ScribeTextTertiary
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
        ) {
            if (isTransforming) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Refining text...",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (canvasText.isNotBlank()) Color.White else ScribeTextTertiary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Rewrite as $selectedLabel",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // ── 6. Transformation Result Review ───────────────────────────────────
        AnimatedVisibility(
            visible = transformResult != null || transformError != null,
            enter = fadeIn() + androidx.compose.animation.expandVertically(spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
            exit = fadeOut()
        ) {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                if (transformResult != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = ScribeSurface,
                        border = BorderStroke(1.dp, ScribeOutline),
                        shadowElevation = 1.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "REFINED DRAFT",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                    color = ScribeTextSecondary
                                )

                                if (transformLatencyMs > 0) {
                                    Text(
                                        text = "${transformLatencyMs}ms",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = ScribeTextTertiary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = transformResult!!,
                                fontSize = 15.sp,
                                lineHeight = 23.sp,
                                color = ScribeTextPrimary
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        canvasText = transformResult!!
                                        Toast.makeText(context, "Applied to editor!", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ScribeTextPrimary,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier.weight(1f).height(40.dp)
                                ) {
                                    Text("Apply to Editor", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Scribe Output", transformResult)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, ScribeOutline),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ScribeTextPrimary),
                                    modifier = Modifier.weight(1f).height(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Copy", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                } else if (transformError != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = ScribeSurfaceVariant,
                        border = BorderStroke(1.dp, ScribeRose.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "Error: $transformError",
                            fontSize = 13.sp,
                            color = ScribeRose,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }
        }

        // ── 7. Studio Presets Shelf (Elevated Apple Workspace) ────────────────
        if (transformResult == null && transformError == null) {
            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "STUDIO PRESETS",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                color = ScribeTextTertiary,
                modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
            )

            val presets = listOf(
                Triple("Executive Summary", "Condense thoughts for leadership & investors", "${currentPrefix}shorten"),
                Triple("Grammar & Tone Polish", "Fix spelling, punctuation, and flow", "${currentPrefix}fix"),
                Triple("Conversational & Warm", "Friendly phrasing for team chats & DMs", "${currentPrefix}casual")
            )

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = ScribeSurface,
                border = BorderStroke(0.5.dp, ScribeOutline),
                shadowElevation = 0.5.dp
            ) {
                Column {
                    presets.forEachIndexed { idx, (title, desc, trigger) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedCommandTrigger = trigger
                                    if (canvasText.isBlank()) {
                                        canvasText = sampleDrafts[idx % sampleDrafts.size]
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ScribeTextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = desc,
                                    fontSize = 12.sp,
                                    color = ScribeTextSecondary
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = ScribeTextTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        if (idx < presets.size - 1) {
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = ScribeOutline,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

