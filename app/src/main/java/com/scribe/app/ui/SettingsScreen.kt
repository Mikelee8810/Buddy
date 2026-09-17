package com.scribe.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.scribe.app.api.ScribeApiClient
import com.scribe.app.manager.CommandManager
import com.scribe.app.manager.HistoryManager
import com.scribe.app.manager.KeyManager
import com.scribe.app.manager.UsageManager
import com.scribe.app.ui.components.*
import com.scribe.app.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController? = null) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()

    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    val keyManager = remember { KeyManager(context) }
    val usageManager = remember { UsageManager(context) }
    val historyManager = remember { HistoryManager(context) }
    val commandManager = remember { CommandManager(context) }

    // State
    var keys by remember { mutableStateOf(keyManager.getKeys()) }
    var providerType by remember { mutableStateOf(prefs.getString("provider_type", "openrouter") ?: "openrouter") }
    var temperature by remember { mutableFloatStateOf(prefs.getFloat("temperature", 0.5f)) }

    // Models
    var selectedOpenRouterModel by remember {
        mutableStateOf(prefs.getString("openrouter_model", "google/gemma-4-31b-it:free") ?: "google/gemma-4-31b-it:free")
    }
    val openRouterModels = listOf(
        "google/gemma-4-31b-it:free",
        "meta-llama/llama-3.3-70b-instruct:free",
        "deepseek/deepseek-r1:free",
        "deepseek/deepseek-chat",
        "google/gemini-2.5-flash",
        "anthropic/claude-3.5-haiku"
    )

    var selectedModel by remember {
        mutableStateOf(prefs.getString("model", "gemini-2.5-flash-lite") ?: "gemini-2.5-flash-lite")
    }
    val geminiModels = listOf("gemini-2.5-flash-lite", "gemini-2.5-flash", "gemini-3.1-flash-lite-preview")

    var selectedGroqModel by remember {
        mutableStateOf(prefs.getString("groq_model", "llama-3.3-70b-versatile") ?: "llama-3.3-70b-versatile")
    }
    val groqModels = listOf(
        "llama-3.3-70b-versatile",
        "meta-llama/llama-4-scout-17b-16e-instruct",
        "llama-3.1-8b-instant"
    )

    var customEndpoint by remember { mutableStateOf(prefs.getString("custom_endpoint", "") ?: "") }
    var customModel by remember { mutableStateOf(prefs.getString("custom_model", "") ?: "") }

    // Trigger Prefix
    var triggerPrefix by remember { mutableStateOf(commandManager.getTriggerPrefix()) }
    var prefixError by remember { mutableStateOf<String?>(null) }

    // Ping State
    var isPinging by remember { mutableStateOf(false) }
    var pingLatencyMs by remember { mutableStateOf<Long?>(null) }
    var pingStatus by remember { mutableStateOf<String?>(null) }

    // Add Key BottomSheet
    var showAddKeySheet by remember { mutableStateOf(false) }
    var newKeyText by remember { mutableStateOf("") }
    var isTestingKey by remember { mutableStateOf(false) }
    var keyTestResult by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Backup / Restore State
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }
    var importError by remember { mutableStateOf<String?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }

    fun pingProvider() {
        if (keys.isEmpty()) {
            pingStatus = "Vault is empty"
            return
        }
        val targetKey = keys.first()
        isPinging = true
        pingStatus = null
        scope.launch {
            val startTime = System.currentTimeMillis()
            val result = when {
                targetKey.startsWith("AIza") -> ScribeApiClient.geminiValidateKey(targetKey)
                targetKey.startsWith("gsk_") -> ScribeApiClient.groqValidateKey(targetKey)
                targetKey.startsWith("sk-or-") || providerType == "openrouter" ->
                    ScribeApiClient.openaiValidateKey(targetKey, "https://openrouter.ai/api/v1")
                else -> {
                    val ep = if (customEndpoint.isNotBlank()) customEndpoint else "https://api.openai.com/v1"
                    ScribeApiClient.openaiValidateKey(targetKey, ep)
                }
            }
            val elapsed = System.currentTimeMillis() - startTime
            isPinging = false
            if (result.isSuccess) {
                pingLatencyMs = elapsed
                pingStatus = "Operational"
            } else {
                pingLatencyMs = null
                pingStatus = "Failed: ${result.exceptionOrNull()?.message?.take(25)}..."
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Brand Header
        item {
            ScribeBrandHeader(
                title = "Engine & Vault",
                subtitle = "API Keys, Providers & Settings"
            )
        }

        // Section 1: Key Vault Bento Tile
        item {
            ScribeBentoSectionHeader(title = "KEY VAULT & ENCRYPTION", icon = Icons.Outlined.Key)
            Spacer(modifier = Modifier.height(8.dp))
            ScribeCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ScribePulsePip(color = if (keys.isNotEmpty()) ScribeEmerald else ScribeAmber)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (keys.isNotEmpty()) "ACTIVE ENGINE ROTATION" else "VAULT STANDBY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = ScribeTextSecondary
                            )
                        }
                        ScribeChip(
                            text = providerType.uppercase(),
                            containerColor = ScribeCobalt.copy(alpha = 0.2f),
                            contentColor = ScribeIce,
                            borderColor = ScribeIce.copy(alpha = 0.35f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (keys.isNotEmpty()) "${keys.size} Key${if (keys.size == 1) "" else "s"} Active" else "No API Keys Configured",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = ScribeTextPrimary
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = if (keys.isNotEmpty())
                                    "Round-robin load-balanced on device"
                                else
                                    "Add a key below to begin processing text expansions",
                                fontSize = 12.sp,
                                color = ScribeTextSecondary
                            )
                        }

                        if (keys.isNotEmpty()) {
                            // Ping Button
                            FilledTonalButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    pingProvider()
                                },
                                enabled = !isPinging,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = ScribeIce.copy(alpha = 0.15f),
                                    contentColor = ScribeIce
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                if (isPinging) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = ScribeIce)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Testing...", fontSize = 12.sp)
                                } else {
                                    Icon(Icons.Outlined.Speed, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Ping", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (pingLatencyMs != null || pingStatus != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (pingLatencyMs != null) {
                                ScribeLatencyBadge(latencyMs = pingLatencyMs!!)
                            }
                            if (pingStatus != null) {
                                Text(
                                    text = pingStatus!!,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (pingStatus == "Operational") ScribeEmerald else ScribeAmber
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Dedicated clean Add Key button right inside the card!
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showAddKeySheet = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ScribeCobalt,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add API Key", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Stored Keys List
        if (keys.isNotEmpty()) {
            itemsIndexed(keys) { index, key ->
                UnifiedKeyRow(
                    index = index,
                    keyStr = key,
                    providerType = providerType,
                    onDelete = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        keyManager.removeKey(key)
                        usageManager.deleteStats(key)
                        keys = keyManager.getKeys()
                    },
                    onUsageClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        navController?.navigate("api_usage/$index")
                    }
                )
            }
        }

        // Section 2: AI Provider Engine Selection
        item {
            ScribeBentoSectionHeader(title = "AI PROVIDER ENGINE", icon = Icons.Outlined.CloudQueue)
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    WarmProviderCard(
                        title = "OpenRouter",
                        subtitle = "Multi-Model Router",
                        selected = providerType == "openrouter",
                        modifier = Modifier.weight(1f)
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        providerType = "openrouter"
                        prefs.edit().putString("provider_type", "openrouter").apply()
                    }
                    WarmProviderCard(
                        title = "Google Gemini",
                        subtitle = "2.0 / 2.5 Flash",
                        selected = providerType == "gemini",
                        modifier = Modifier.weight(1f)
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        providerType = "gemini"
                        prefs.edit().putString("provider_type", "gemini").apply()
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    WarmProviderCard(
                        title = "Groq",
                        subtitle = "Ultra-Fast LPU",
                        selected = providerType == "groq",
                        modifier = Modifier.weight(1f)
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        providerType = "groq"
                        prefs.edit().putString("provider_type", "groq").apply()
                    }
                    WarmProviderCard(
                        title = "Custom",
                        subtitle = "OpenAI Compatible",
                        selected = providerType == "custom",
                        modifier = Modifier.weight(1f)
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        providerType = "custom"
                        prefs.edit().putString("provider_type", "custom").apply()
                    }
                }
            }
        }

        // Model Selection Dropdown
        item {
            AnimatedVisibility(
                visible = providerType == "openrouter",
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                ScribeCard(modifier = Modifier.fillMaxWidth(), borderColor = ScribeOutline) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Curated OpenRouter Model", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ScribeParchment)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Includes Gemma 4, Llama 3.3, and DeepSeek R1", fontSize = 12.sp, color = ScribeParchmentMuted)
                        Spacer(modifier = Modifier.height(14.dp))

                        WarmDropdown(
                            options = openRouterModels,
                            selectedOption = selectedOpenRouterModel,
                            onOptionSelected = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                selectedOpenRouterModel = it
                                prefs.edit().putString("openrouter_model", it).apply()
                            }
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = providerType == "gemini" || providerType == "groq",
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                ScribeCard(modifier = Modifier.fillMaxWidth(), borderColor = ScribeOutline) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (providerType == "gemini") "Gemini Model Selection" else "Groq Model Selection",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = ScribeParchment
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        if (providerType == "gemini") {
                            WarmDropdown(
                                options = geminiModels,
                                selectedOption = selectedModel,
                                onOptionSelected = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    selectedModel = it
                                    prefs.edit().putString("model", it).apply()
                                }
                            )
                        } else {
                            WarmDropdown(
                                options = groqModels,
                                selectedOption = selectedGroqModel,
                                onOptionSelected = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    selectedGroqModel = it
                                    prefs.edit().putString("groq_model", it).apply()
                                }
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = providerType == "custom",
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                ScribeCard(modifier = Modifier.fillMaxWidth(), borderColor = ScribeOutline) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "API Base Endpoint", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ScribeParchment)
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = customEndpoint,
                            onValueChange = {
                                customEndpoint = it
                                prefs.edit().putString("custom_endpoint", it).apply()
                            },
                            placeholder = { Text("https://api.openai.com/v1", color = ScribeParchmentMuted) },
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

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(text = "Model Identifier", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ScribeParchment)
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = customModel,
                            onValueChange = {
                                customModel = it
                                prefs.edit().putString("custom_model", it).apply()
                            },
                            placeholder = { Text("e.g. gpt-4o", color = ScribeParchmentMuted) },
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
                    }
                }
            }
        }

        // Section 3: Activation Symbol & Case-Insensitive Notice
        item {
            ScribeBentoSectionHeader(title = "ACTIVATION & TRIGGER", icon = Icons.Outlined.Keyboard)
            Spacer(modifier = Modifier.height(8.dp))
            ScribeCard(modifier = Modifier.fillMaxWidth(), borderColor = ScribeOutline) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                            Text(text = "Trigger Character", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ScribeParchment)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Starts a command anywhere (e.g. ${triggerPrefix}fix or ${triggerPrefix}formal)",
                                fontSize = 12.sp,
                                color = ScribeParchmentMuted
                            )
                        }

                        OutlinedTextField(
                            value = triggerPrefix,
                            onValueChange = { input ->
                                val filtered = input.take(1)
                                triggerPrefix = filtered
                                prefixError = when {
                                    filtered.length != 1 -> "Must be 1 char"
                                    filtered[0].isWhitespace() -> "No whitespace"
                                    filtered[0].isLetterOrDigit() -> "No alphanumeric"
                                    else -> {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        commandManager.setTriggerPrefix(filtered)
                                        null
                                    }
                                }
                            },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                textAlign = TextAlign.Center,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ScribeGold
                            ),
                            modifier = Modifier.width(68.dp),
                            isError = prefixError != null,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ScribeGold,
                                unfocusedBorderColor = ScribeIce.copy(alpha = 0.35f),
                                focusedContainerColor = ScribeSurfaceVariant,
                                unfocusedContainerColor = ScribeSurfaceVariant,
                                focusedTextColor = ScribeGold,
                                unfocusedTextColor = ScribeGold
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = ScribeSage, modifier = Modifier.size(15.dp))
                        Text(
                            text = "Case-insensitive enabled: works with ${triggerPrefix}Fix, ${triggerPrefix}FIX, and ${triggerPrefix}fix seamlessly",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = ScribeSage
                        )
                    }
                }
            }
        }

        // Section 4: Temperature / Tone Slider
        item {
            ScribeBentoSectionHeader(title = "INFERENCE TONE", icon = Icons.Outlined.Tune)
            Spacer(modifier = Modifier.height(8.dp))
            ScribeCard(modifier = Modifier.fillMaxWidth(), borderColor = ScribeOutline) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Temperature", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ScribeParchment)
                        val tone = when {
                            temperature < 0.3f -> "Precise / Strict"
                            temperature < 0.7f -> "Balanced Editorial"
                            temperature < 1.1f -> "Expressive Rewrite"
                            else -> "Inventive"
                        }
                        ScribeChip(
                            text = "${String.format(java.util.Locale.US, "%.1f", temperature)} • $tone",
                            containerColor = ScribeCobalt.copy(alpha = 0.2f),
                            contentColor = ScribeGold,
                            borderColor = ScribeIce.copy(alpha = 0.35f)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Slider(
                        value = temperature,
                        onValueChange = { temperature = it },
                        onValueChangeFinished = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            prefs.edit().putFloat("temperature", temperature).apply()
                        },
                        valueRange = 0f..1.5f,
                        steps = 14,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = ScribeGold,
                            activeTrackColor = ScribeGold,
                            inactiveTrackColor = ScribeOutline
                        )
                    )
                }
            }
        }

        // Section 5: Data & Backup
        item {
            ScribeBentoSectionHeader(title = "PORTABILITY & BACKUP", icon = Icons.Outlined.Security)
            Spacer(modifier = Modifier.height(8.dp))
            ScribeCard(modifier = Modifier.fillMaxWidth(), borderColor = ScribeOutline) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Configuration Portability", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ScribeParchment)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "1-tap JSON export or import of all custom commands, prompt overrides, and trigger prefix.",
                        fontSize = 12.sp,
                        color = ScribeParchmentMuted
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val json = commandManager.exportConfigJson()
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Scribe Backup", json))
                                Toast.makeText(context, "Config copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = ScribeCobalt.copy(alpha = 0.2f),
                                contentColor = ScribeGold
                            )
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export JSON", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        FilledTonalButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                importError = null
                                importJsonText = ""
                                showImportDialog = true
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color(0x26F59E0B),
                                contentColor = ScribeAmber
                            )
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import JSON", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showResetDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0x33EF4444)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ScribeTerracotta)
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reset Built-In Commands to Default", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Section 6: About Scribe & Privacy
        item {
            ScribeBentoSectionHeader(title = "SYSTEM ARCHITECTURE", icon = Icons.Outlined.Shield)
            Spacer(modifier = Modifier.height(8.dp))
            ScribeCard(modifier = Modifier.fillMaxWidth(), borderColor = ScribeOutline) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ScribePulsePip(color = ScribeSage)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "100% On-Device Execution", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ScribeParchment)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Scribe operates strictly between your device and your chosen AI endpoint. Zero intermediary servers, zero cloud logging, and 16 KB page-aligned native runtime.",
                        fontSize = 12.sp,
                        color = ScribeParchmentMuted,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { uriHandler.openUri("https://github.com/Mikelee8810/Scribe") },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Star, contentDescription = null, tint = ScribeGold, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("github.com/Mikelee8810/Scribe", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ScribeGold)
                    }
                }
            }
        }
    }

    // Modal BottomSheet for Adding Key
    if (showAddKeySheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showAddKeySheet = false
                newKeyText = ""
                keyTestResult = null
            },
            sheetState = sheetState,
            containerColor = ScribeSurface,
            contentColor = ScribeParchment,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = { BottomSheetDefaults.DragHandle(color = ScribeOutline) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp)
            ) {
                Text(text = "Vault New API Key", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = ScribeParchment)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Encrypted on-device via Android KeyStore (AES-GCM)", fontSize = 12.sp, color = ScribeParchmentMuted)

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = newKeyText,
                    onValueChange = {
                        newKeyText = it
                        keyTestResult = null
                    },
                    label = { Text("API Key Token (e.g. sk-or-...)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ScribeGold,
                        unfocusedBorderColor = ScribeOutline,
                        focusedContainerColor = ScribeSurfaceVariant,
                        unfocusedContainerColor = ScribeSurfaceVariant,
                        focusedTextColor = ScribeParchment,
                        unfocusedTextColor = ScribeParchment
                    )
                )

                keyTestResult?.let { msg ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = msg,
                        color = if (msg.startsWith("Valid")) ScribeSage else ScribeTerracotta,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            showAddKeySheet = false
                            newKeyText = ""
                            keyTestResult = null
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ScribeOutline),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ScribeParchment)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val trimmedKey = newKeyText.trim()
                            if (trimmedKey.isNotBlank()) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                isTestingKey = true
                                keyTestResult = null
                                scope.launch {
                                    if (keys.contains(trimmedKey)) {
                                        isTestingKey = false
                                        keyTestResult = "Key already exists in vault"
                                        return@launch
                                    }
                                    val result = when {
                                        trimmedKey.startsWith("gsk_") -> ScribeApiClient.groqValidateKey(trimmedKey)
                                        trimmedKey.startsWith("AIza") -> ScribeApiClient.geminiValidateKey(trimmedKey)
                                        trimmedKey.startsWith("sk-or-") || providerType == "openrouter" ->
                                            ScribeApiClient.openaiValidateKey(trimmedKey, "https://openrouter.ai/api/v1")
                                        else -> {
                                            val ep = if (customEndpoint.isNotBlank()) customEndpoint else "https://api.openai.com/v1"
                                            ScribeApiClient.openaiValidateKey(trimmedKey, ep)
                                        }
                                    }
                                    isTestingKey = false
                                    if (result.isSuccess) {
                                        keyManager.addKey(trimmedKey)
                                        keys = keyManager.getKeys()
                                        newKeyText = ""
                                        keyTestResult = "Valid key vaulted!"
                                        showAddKeySheet = false
                                    } else {
                                        keyTestResult = result.exceptionOrNull()?.message ?: "Validation failed"
                                    }
                                }
                            }
                        },
                        enabled = newKeyText.isNotBlank() && !isTestingKey,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ScribeCobalt, contentColor = ScribeBackground)
                    ) {
                        if (isTestingKey) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = ScribeBackground)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Verifying...")
                        } else {
                            Text("Vault Key", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    WarmLinkChip("OpenRouter", "https://openrouter.ai/keys", uriHandler)
                    WarmLinkChip("Gemini", "https://aistudio.google.com/app/apikey", uriHandler)
                    WarmLinkChip("Groq", "https://console.groq.com/keys", uriHandler)
                }
            }
        }
    }

    // Import Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text(text = "Import JSON Backup", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = ScribeParchment) },
            text = {
                Column {
                    Text(text = "Paste your exported JSON configuration below:", fontSize = 13.sp, color = ScribeParchmentMuted)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = {
                            importJsonText = it
                            importError = null
                        },
                        placeholder = { Text("{\n  \"custom_commands\": [...]\n}", color = ScribeParchmentMuted) },
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth().height(140.dp),
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
                    importError?.let { msg ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = msg, color = ScribeTerracotta, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val result = commandManager.importConfigJson(importJsonText.trim())
                        if (result.isSuccess) {
                            val count = result.getOrNull() ?: 0
                            Toast.makeText(context, "Successfully restored $count custom commands!", Toast.LENGTH_SHORT).show()
                            triggerPrefix = commandManager.getTriggerPrefix()
                            showImportDialog = false
                        } else {
                            importError = "Invalid JSON: ${result.exceptionOrNull()?.message?.take(30)}"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ScribeCobalt, contentColor = ScribeBackground),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Restore", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showImportDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = ScribeParchmentMuted)
                ) {
                    Text("Cancel")
                }
            },
            containerColor = ScribeSurface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Reset Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(text = "Reset Default Commands?", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = ScribeParchment) },
            text = {
                Text(
                    text = "Restores all 9 original built-in shortcuts and clears prompt overrides. Your custom commands remain untouched.",
                    fontSize = 13.sp,
                    color = ScribeParchmentMuted,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        commandManager.resetBuiltInCommands()
                        Toast.makeText(context, "Default commands restored", Toast.LENGTH_SHORT).show()
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
private fun ScribeBentoSectionHeader(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = ScribeGold, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 1.sp,
            color = ScribeParchmentMuted
        )
    }
}

@Composable
private fun WarmProviderCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val containerColor = if (selected) ScribeCobalt.copy(alpha = 0.18f) else ScribeSurfaceVariant
    val borderColor = if (selected) ScribeIce else ScribeOutline

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(containerColor)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(vertical = 14.dp, horizontal = 12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (selected) ScribeTextPrimary else ScribeTextSecondary
                )
                if (selected) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(ScribeIce),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = ScribeBackground, modifier = Modifier.size(11.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, fontSize = 11.sp, color = ScribeTextTertiary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WarmDropdown(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ScribeIce,
                unfocusedBorderColor = ScribeOutline,
                focusedContainerColor = ScribeSurfaceVariant,
                unfocusedContainerColor = ScribeSurfaceVariant,
                focusedTextColor = ScribeTextPrimary,
                unfocusedTextColor = ScribeTextPrimary
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(ScribeSurface)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            color = if (option == selectedOption) ScribeIce else ScribeTextPrimary,
                            fontWeight = if (option == selectedOption) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun UnifiedKeyRow(
    index: Int,
    keyStr: String,
    providerType: String,
    onDelete: () -> Unit,
    onUsageClick: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val providerName = when {
        keyStr.startsWith("AIza")   -> "Gemini"
        keyStr.startsWith("gsk_")   -> "Groq"
        keyStr.startsWith("sk-or-") -> "OpenRouter"
        keyStr.startsWith("sk-")    -> "OpenAI"
        else -> if (providerType == "openrouter") "OpenRouter" else if (providerType == "custom") "Custom" else "Hardware Token"
    }

    val maskedKey = remember(keyStr) {
        if (keyStr.length > 10) "${keyStr.take(7)}••••••${keyStr.takeLast(4)}" else "••••••••"
    }

    ScribeCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = ScribeOutline
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ScribePulsePip(color = ScribeSage)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$providerName Slot #${index + 1}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = ScribeParchment
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = maskedKey,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = ScribeParchmentMuted
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Scribe Key", keyStr))
                        Toast.makeText(context, "Key copied", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = ScribeParchmentMuted, modifier = Modifier.size(16.dp))
                }

                IconButton(
                    onClick = onUsageClick,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(Icons.Outlined.BarChart, contentDescription = "Usage", tint = ScribeGold, modifier = Modifier.size(17.dp))
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ScribeTerracotta, modifier = Modifier.size(17.dp))
                }
            }
        }
    }
}

@Composable
private fun WarmLinkChip(label: String, url: String, uriHandler: UriHandler) {
    Surface(
        onClick = { uriHandler.openUri(url) },
        shape = RoundedCornerShape(8.dp),
        color = ScribeCobalt.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, ScribeOutline)
    ) {
        Text(
            text = "$label ↗",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = ScribeGold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}
