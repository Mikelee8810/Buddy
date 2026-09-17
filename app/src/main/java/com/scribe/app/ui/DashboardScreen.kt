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
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.scribe.app.api.ScribeApiClient
import com.scribe.app.manager.CommandManager
import com.scribe.app.manager.KeyManager
import com.scribe.app.ui.components.*
import com.scribe.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

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
    var isServiceEnabled by remember { mutableStateOf(checkServiceEnabled(context)) }
    var keyCount by remember { mutableIntStateOf(keyManager.getKeys().size) }
    var currentPrefix by remember { mutableStateOf(commandManager.getTriggerPrefix()) }
    var latestVersion by remember { mutableStateOf<String?>(null) }
    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        } catch (e: Exception) { "1.0.0" }
    }
    val uriHandler = LocalUriHandler.current

    // Live Scratchpad State
    var testInputText by remember { mutableStateOf("Hey, can we sync up real quick tomorrow morning to finalize that proposal?") }
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

        launch(Dispatchers.IO) {
            try {
                val url = URL("https://api.github.com/repos/Mikelee8810/Scribe/releases/latest")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val json = JSONObject(response)
                    latestVersion = json.getString("tag_name").removePrefix("v")
                }
            } catch (_: Exception) {}
        }

        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                isServiceEnabled = checkServiceEnabled(context)
                keyCount = keyManager.getKeys().size
                currentPrefix = commandManager.getTriggerPrefix()
                delay(2500)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScribeBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        // Luxury Scribe Brand Header
        ScribeBrandHeader(
            title = "Scribe",
            subtitle = "AI Keyboard Co-Pilot",
            trailingContent = {
                latestVersion?.let { latest ->
                    val isLatest = versionName == latest || versionName >= latest
                    val badgeColor = if (isLatest) ScribeEmerald else ScribeIndigo
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(ScribeSurfaceVariant)
                            .border(1.dp, ScribeOutline, RoundedCornerShape(12.dp))
                            .clickable {
                                uriHandler.openUri("https://github.com/Mikelee8810/Scribe")
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(badgeColor)
                        )
                        Text(
                            text = "v$versionName",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ScribeTextPrimary
                        )
                    }
                }
            }
        )

        // ── 1. BENTO HERO TILE: Live Engine & Active Model ────────────────────────────
        ScribeCard(
            border = BorderStroke(
                1.dp,
                if (isServiceEnabled) Color(0x3310B981) else Color(0x4DF59E0B)
            ),
            backgroundColor = ScribeSurface,
            contentPadding = PaddingValues(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isServiceEnabled) Color(0x1F10B981) else Color(0x1FF59E0B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isServiceEnabled) Icons.Outlined.CheckCircle else Icons.Outlined.Bolt,
                            contentDescription = null,
                            tint = if (isServiceEnabled) ScribeEmerald else ScribeAmber,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ScribePulsePip(isActive = isServiceEnabled)
                            Text(
                                text = "SCRIBE ENGINE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = ScribeTextTertiary
                            )
                        }
                        Text(
                            text = if (isServiceEnabled) "Active & Listening" else "Setup Required",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ScribeTextPrimary
                        )
                    }
                }

                ScribeStatusBadge(
                    label = if (isServiceEnabled) "Online" else "Pending",
                    isActive = isServiceEnabled
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Active Provider & Model Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(ScribeSurfaceVariant)
                    .border(1.dp, ScribeOutline, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Psychology,
                        contentDescription = null,
                        tint = ScribeCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = providerType.replaceFirstChar { it.uppercase() },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ScribeTextPrimary
                    )
                }
                Text(
                    text = activeModelName.take(24) + if (activeModelName.length > 24) "..." else "",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = ScribeTextSecondary
                )
            }

            if (!isServiceEnabled) {
                Spacer(modifier = Modifier.height(16.dp))
                ScribeButton(
                    text = "Enable Scribe Engine",
                    icon = Icons.AutoMirrored.Outlined.ArrowForward,
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // ── 2. BENTO TILE: Live In-App Test Scratchpad ────────────────────────────────
        ScribeCard(
            border = BorderStroke(1.dp, Brush.linearGradient(listOf(ScribeCobalt.copy(alpha = 0.4f), ScribeIce.copy(alpha = 0.2f)))),
            backgroundColor = ScribeSurface,
            contentPadding = PaddingValues(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ScribeCobalt.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Science,
                            contentDescription = null,
                            tint = ScribeGold,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "LIVE SCRATCHPAD",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = ScribeParchmentMuted
                    )
                }

                if (transformLatencyMs > 0) {
                    ScribeLatencyBadge(latencyMs = transformLatencyMs)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Test Text Input Field
            OutlinedTextField(
                value = testInputText,
                onValueChange = { testInputText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 90.dp),
                placeholder = {
                    Text(
                        "Type or paste text to test Scribe...",
                        color = ScribeTextTertiary,
                        fontSize = 13.sp
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ScribeIndigo,
                    unfocusedBorderColor = ScribeOutline,
                    focusedContainerColor = ScribeSurfaceVariant,
                    unfocusedContainerColor = ScribeSurfaceVariant,
                    focusedTextColor = ScribeTextPrimary,
                    unfocusedTextColor = ScribeTextPrimary,
                    cursorColor = ScribeCyan
                ),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Command Chips
            Text(
                text = "SELECT SHORTCUT:",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = ScribeTextTertiary
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val sampleCommands = listOf("${currentPrefix}fix", "${currentPrefix}formal", "${currentPrefix}casual", "${currentPrefix}shorten", "${currentPrefix}expand")
                sampleCommands.forEach { trigger ->
                    ScribeChip(
                        text = trigger,
                        isSelected = selectedCommandTrigger == trigger,
                        onClick = { selectedCommandTrigger = trigger }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Run Transform Button
            ScribeButton(
                text = if (isTransforming) "Synthesizing with AI..." else "Run $selectedCommandTrigger Transform",
                icon = if (isTransforming) null else Icons.Outlined.AutoAwesome,
                onClick = {
                    if (keyCount == 0) {
                        Toast.makeText(context, "Add an API key in the Keys tab first!", Toast.LENGTH_SHORT).show()
                        return@ScribeButton
                    }
                    if (testInputText.isBlank()) {
                        Toast.makeText(context, "Please enter some text to test!", Toast.LENGTH_SHORT).show()
                        return@ScribeButton
                    }

                    isTransforming = true
                    transformError = null
                    transformResult = null

                    coroutineScope.launch {
                        val key = keyManager.getNextKey() ?: ""
                        val prompt = commandManager.getCommands().find { it.trigger == selectedCommandTrigger }?.prompt
                            ?: "Transform this text into clean, professional writing."
                        val temperature = prefs.getFloat("temperature", 0.5f).toDouble()
                        val startTime = System.currentTimeMillis()

                        val result = withContext(Dispatchers.IO) {
                            when (providerType) {
                                "openrouter" -> ScribeApiClient.openaiGenerate(
                                    prompt = prompt,
                                    text = testInputText,
                                    apiKey = key,
                                    model = activeModelName,
                                    temperature = temperature,
                                    endpoint = "https://openrouter.ai/api/v1"
                                )
                                "groq" -> ScribeApiClient.groqGenerate(
                                    prompt = prompt,
                                    text = testInputText,
                                    apiKey = key,
                                    model = activeModelName,
                                    temperature = temperature
                                )
                                "custom" -> ScribeApiClient.openaiGenerate(
                                    prompt = prompt,
                                    text = testInputText,
                                    apiKey = key,
                                    model = activeModelName,
                                    temperature = temperature,
                                    endpoint = prefs.getString("custom_endpoint", "") ?: ""
                                )
                                else -> ScribeApiClient.geminiGenerate(
                                    prompt = prompt,
                                    text = testInputText,
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
                            transformResult = result.getOrThrow()
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        } else {
                            transformError = result.exceptionOrNull()?.message ?: "Unknown error"
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    }
                }
            )

            // Result Display Card
            AnimatedVisibility(
                visible = transformResult != null || transformError != null,
                enter = fadeIn() + androidx.compose.animation.expandVertically(spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
                exit = fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    if (transformResult != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x2E10B981))
                                .border(1.dp, Color(0x4D10B981), RoundedCornerShape(12.dp))
                                .padding(14.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "TRANSFORMED RESULT",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        color = ScribeEmerald
                                    )
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("Scribe Output", transformResult)
                                            clipboard.setPrimaryClip(clip)
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.ContentCopy,
                                            contentDescription = "Copy",
                                            tint = ScribeEmerald,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = transformResult!!,
                                    fontSize = 14.sp,
                                    color = ScribeTextPrimary,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    } else if (transformError != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x2EF43F5E))
                                .border(1.dp, Color(0x4DF43F5E), RoundedCornerShape(12.dp))
                                .padding(14.dp)
                        ) {
                            Text(
                                text = "Error: $transformError",
                                fontSize = 12.sp,
                                color = ScribeRose
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // ── 3. BENTO 2-COLUMN TELEMETRY GRID ──────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Metric Tile 1: API Keys
            ScribeCard(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "API POOL",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = ScribeTextTertiary
                    )
                    Icon(
                        imageVector = Icons.Outlined.Key,
                        contentDescription = null,
                        tint = ScribeIndigo,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$keyCount Keys",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ScribeTextPrimary
                )
                Text(
                    text = if (keyCount > 0) "Multi-key rotation" else "Key required",
                    fontSize = 11.sp,
                    color = if (keyCount > 0) ScribeEmerald else ScribeRose,
                    fontWeight = FontWeight.Medium
                )
            }

            // Metric Tile 2: Trigger Prefix
            ScribeCard(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TRIGGER",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = ScribeTextTertiary
                    )
                    Icon(
                        imageVector = Icons.Outlined.Terminal,
                        contentDescription = null,
                        tint = ScribeCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$currentPrefix<cmd>",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    color = ScribeTextPrimary
                )
                Text(
                    text = "Shortcut trigger",
                    fontSize = 11.sp,
                    color = ScribeTextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // ── 4. BENTO TILE: Security & Privacy Isolation ──────────────────────────────
        ScribeCard(
            border = BorderStroke(1.dp, Color(0x2E10B981)),
            contentPadding = PaddingValues(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x1F10B981)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = ScribeEmerald,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = "Vault Protection Active",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = ScribeTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Password managers (1Password, Bitwarden) and banking apps are isolated from Scribe's text buffer.",
                        fontSize = 12.sp,
                        color = ScribeTextSecondary,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // ── 5. BENTO TILE: Tactile Command Palette ───────────────────────────────────
        ScribeCard(
            contentPadding = PaddingValues(18.dp)
        ) {
            Text(
                text = "POPULAR SHORTCUTS (TAP TO TEST)",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = ScribeTextTertiary
            )
            Spacer(modifier = Modifier.height(12.dp))

            val quickTriggers = listOf(
                Pair("${currentPrefix}fix", "Fix grammar, spelling, punctuation"),
                Pair("${currentPrefix}casual", "Conversational, human & relaxed"),
                Pair("${currentPrefix}formal", "Polished, executive prose"),
                Pair("${currentPrefix}shorten", "Condense into punchy summary"),
                Pair("${currentPrefix}undo", "Restore previous pre-rewrite text")
            )

            quickTriggers.forEachIndexed { i, (cmd, desc) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            selectedCommandTrigger = cmd
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedCommandTrigger == cmd) ScribeCobalt.copy(alpha = 0.25f) else ScribeSurfaceVariant)
                            .border(1.dp, if (selectedCommandTrigger == cmd) ScribeGold else ScribeOutline, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = cmd,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (selectedCommandTrigger == cmd) ScribeGold else ScribeParchmentMuted
                        )
                    }
                    Text(
                        text = desc,
                        fontSize = 12.sp,
                        color = ScribeParchmentMuted,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f).padding(start = 12.dp)
                    )
                }
                if (i < quickTriggers.lastIndex) {
                    HorizontalDivider(
                        color = ScribeOutline,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}
