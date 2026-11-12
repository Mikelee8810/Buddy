package com.scribe.app.service

import android.accessibilityservice.AccessibilityService
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import android.widget.Toast
import com.scribe.app.api.PythonBridge
import com.scribe.app.manager.CommandManager
import com.scribe.app.manager.HistoryManager
import com.scribe.app.manager.KeyManager
import com.scribe.app.manager.UsageManager
import com.scribe.app.model.Command
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class AssistantService : AccessibilityService() {

    private lateinit var keyManager: KeyManager
    private lateinit var usageManager: UsageManager
    private lateinit var commandManager: CommandManager
    private lateinit var historyManager: HistoryManager
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + Dispatchers.IO)
    @Volatile private var isProcessing = false
    private val handler = Handler(Looper.getMainLooper())
    private var triggerLastChars = setOf<Char>()
    private var cachedPrefix = CommandManager.DEFAULT_PREFIX
    private var currentJob: Job? = null
    @Volatile private var lastOriginalText: String? = null
    private var lastTriggerRefresh = 0L
    private var currentOverlayToast: View? = null
    private var dismissRunnable: Runnable? = null
    private var dismissAnimator: AnimatorSet? = null
    private var enterAnimator: AnimatorSet? = null
    private var voiceBubble: VoiceBubble? = null
    private var focusedField: AccessibilityNodeInfo? = null

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density + 0.5f).toInt()

    private companion object {
        const val TRIGGER_REFRESH_INTERVAL_MS = 5_000L
        const val DEFAULT_TEMPERATURE = 0.5
        val SPINNER_FRAMES = arrayOf("✦","✧","✦","✧")
        const val TOAST_BACKGROUND_COLOR = 0xE6323232.toInt()
        const val TOAST_DURATION_MS = 3500L
        const val TOAST_BOTTOM_MARGIN_DP = 64
        const val TOAST_ANIM_DURATION_MS = 300L
        const val TOAST_SLIDE_DISTANCE_DP = 40
        val BLOCKED_PACKAGE_PATTERNS = listOf(
            "password",
            "bitwarden",
            "onepassword",
            "keeper",
            "dashlane",
            "authenticator",
            "authy",
            "bank",
            "chase",
            "paypal",
            "venmo"
        )
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        keyManager = KeyManager(applicationContext)
        usageManager = UsageManager(applicationContext)
        commandManager = CommandManager(applicationContext)
        historyManager = HistoryManager(applicationContext)
        updateTriggers()
        voiceBubble = VoiceBubble(
            this,
            onTranscript = { spoken, editMode -> if (editMode) handleVoiceEdit(spoken) else handleDictation(spoken) },
            onError = { msg -> serviceScope.launch { showToast(msg) } }
        )
    }

    private fun isVoiceEnabled(): Boolean {
        val enabled = applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getBoolean("voice_bubble_enabled", false)
        val granted = checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        return enabled && granted
    }

    /**
     * Show the mic bubble whenever the keyboard is open on an editable text
     * box in another app; hide it otherwise. Checking the keyboard window is
     * more reliable than focus events, which many apps don't send.
     */
    private fun refreshBubble() {
        val bubble = voiceBubble ?: return
        if (bubble.isListening) return
        if (!isVoiceEnabled()) { focusedField = null; bubble.hide(); return }
        val imeOpen = try {
            windows.any { it.type == android.view.accessibility.AccessibilityWindowInfo.TYPE_INPUT_METHOD }
        } catch (_: Exception) { false }
        val field = if (imeOpen) findEditableFocus() else null
        val pkg = field?.packageName?.toString()?.lowercase() ?: ""
        val usable = field != null && !field.isPassword &&
            pkg != packageName && BLOCKED_PACKAGE_PATTERNS.none { pkg.contains(it) }
        if (usable) {
            focusedField = field
            bubble.show()
        } else {
            focusedField = null
            bubble.hide()
        }
    }

    /** The text box that currently has input focus, in any window. */
    private fun findEditableFocus(): AccessibilityNodeInfo? {
        val fromActive = try {
            rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        } catch (_: Exception) { null }
        if (fromActive != null && fromActive.isEditable) return fromActive
        return try {
            windows.asSequence()
                .filter { it.type == android.view.accessibility.AccessibilityWindowInfo.TYPE_APPLICATION }
                .mapNotNull { it.root?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) }
                .firstOrNull { it.isEditable }
        } catch (_: Exception) { null }
    }

    /**
     * "Talk to edit": the spoken words are an instruction for the text that's
     * already in the field (or just the highlighted part).
     */
    private fun handleVoiceEdit(instruction: String) {
        val field = findEditableFocus() ?: focusedField ?: run {
            serviceScope.launch { showToast("Tap a text box first") }
            return
        }
        if (isProcessing) return
        field.refresh()
        val showingHint = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && field.isShowingHintText
        val full = if (showingHint) "" else field.text?.toString().orEmpty()
        if (full.isBlank()) {
            serviceScope.launch { showToast("Nothing to edit yet. Tap the mic to dictate instead.") }
            return
        }
        val start = field.textSelectionStart
        val end = field.textSelectionEnd
        val hasSelection = start in 0 until end && end <= full.length
        val target = if (hasSelection) full.substring(start, end) else full
        val before = if (hasSelection) full.substring(0, start) else ""
        val after = if (hasSelection) full.substring(end) else ""
        val cmd = Command(
            "voice-edit",
            "Edit the text according to this spoken instruction: \"$instruction\". Keep everything the instruction doesn't ask to change. Keep the original language. Return only the edited text, with no explanations.",
            false
        )
        isProcessing = true
        currentJob?.cancel()
        processCommand(field, target, cmd, getClipboardContent(), keepPrefix = before, fallbackText = full, keepSuffix = after)
    }

    /** Clean up spoken text with the /ramble prompt and add it to the focused field. */
    private fun handleDictation(spoken: String) {
        val field = findEditableFocus() ?: focusedField ?: run {
            serviceScope.launch { showToast("Tap a text box first") }
            return
        }
        if (isProcessing) return
        field.refresh()
        val showingHint = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && field.isShowingHintText
        val existing = if (showingHint) "" else field.text?.toString().orEmpty()
        val prefix = if (existing.isBlank()) "" else existing.trimEnd() + " "
        isProcessing = true
        currentJob?.cancel()
        processCommand(
            field, spoken, commandManager.getRambleCommand(), getClipboardContent(),
            keepPrefix = prefix, fallbackText = prefix + spoken
        )
    }

    private fun updateTriggers() {
        cachedPrefix = commandManager.getTriggerPrefix()
        val cmds = commandManager.getCommands()
        triggerLastChars = cmds.flatMap { cmd ->
            val last = cmd.trigger.lastOrNull()
            if (last != null) listOf(last.lowercaseChar(), last.uppercaseChar()) else emptyList()
        }.toSet()
        lastTriggerRefresh = System.currentTimeMillis()
    }

    /**
     * Visible text on screen (for example the chat you're replying to),
     * skipping the field being edited and password fields. Capped so we
     * don't send a huge prompt.
     */
    private fun getScreenText(exclude: AccessibilityNodeInfo?): String {
        val root = try { rootInActiveWindow } catch (_: Exception) { null } ?: return ""
        val out = StringBuilder()
        fun walk(node: AccessibilityNodeInfo?, depth: Int) {
            if (node == null || depth > 40 || out.length > 4000) return
            if (node.isPassword || (exclude != null && node == exclude)) return
            val t = node.text?.toString()?.trim()
            if (!t.isNullOrEmpty() && !node.isEditable) out.append(t).append('\n')
            for (i in 0 until node.childCount) walk(node.getChild(i), depth + 1)
        }
        walk(root, 0)
        return out.toString().takeLast(4000).trim()
    }

    /**
     * Extra instructions that make rewrites sound like the user. Only used for
     * commands that write new wording, not for fixes, summaries or dictation.
     */
    private fun personalStyleBlock(command: Command, prefs: android.content.SharedPreferences): String {
        val name = command.trigger.removePrefix(cachedPrefix).lowercase()
        val noStyle = setOf("fix", "ai", "translate", "ramble", "undo", "todo", "bullets", "tldr", "title", "hashtags", "explain", "emoji")
        if (name in noStyle || name.startsWith("translate:") || command.isTextReplacer) return ""
        val sb = StringBuilder()
        if (prefs.getBoolean("match_my_style", true)) {
            val samples = historyManager.getStyleSamples()
            if (samples.size >= 2) {
                sb.append("\n\nWrite the way this user writes. Samples of their own writing:\n")
                samples.forEach { sb.append("- ").append(it.replace('\n', ' ')).append('\n') }
                sb.append("Match their vocabulary, sentence length, punctuation and capitalization habits, while still following the instruction above.")
            }
        }
        val aboutMe = prefs.getString("about_me", "").orEmpty().trim()
        if (aboutMe.isNotEmpty()) {
            sb.append("\n\nAbout the user (use only when relevant, never invent details): ").append(aboutMe.take(500))
        }
        return sb.toString()
    }

    /** Friendly name of the app being typed in, e.g. "WhatsApp" or "Gmail". */
    private fun getAppName(node: AccessibilityNodeInfo?): String {
        val pkg = node?.packageName?.toString() ?: return ""
        return try {
            val pm = packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        } catch (_: Exception) {
            pkg
        }
    }

    private fun getClipboardContent(): String {
        return try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val item = clipboard?.primaryClip?.getItemAt(0)
            item?.text?.toString() ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
            refreshBubble()
            return
        }
        if (event.eventType != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED || isProcessing) return
        val pkg = event.packageName?.toString()?.lowercase() ?: ""
        if (BLOCKED_PACKAGE_PATTERNS.any { pkg.contains(it) }) return
        val source = event.source ?: return
        val text = source.text?.toString() ?: return
        if (text.isEmpty()) return

        if (System.currentTimeMillis() - lastTriggerRefresh > TRIGGER_REFRESH_INTERVAL_MS) {
            updateTriggers()
        }

        val lastChar = text[text.length - 1]
        if (!triggerLastChars.contains(lastChar.lowercaseChar()) && !triggerLastChars.contains(lastChar.uppercaseChar())) {
            if (!lastChar.isLetterOrDigit() || !text.contains("${cachedPrefix}translate:", ignoreCase = true)) return
        }

        val command = commandManager.findCommand(text) ?: return
        val cleanText = text.substring(0, text.length - command.trigger.length).trim()

        if (command.trigger.endsWith("undo", ignoreCase = true) && command.isBuiltIn) {
            if (source.isPassword) return
            isProcessing = true
            currentJob?.cancel()
            handleUndo(source, cleanText)
            return
        }

        if (cleanText.isEmpty() && !command.isTextReplacer) return
        if (source.isPassword) return
        isProcessing = true
        currentJob?.cancel()

        val clip = getClipboardContent()

        if (command.isTextReplacer) {
            val prefixText = text.substring(0, text.length - command.trigger.length)
            val resolved = commandManager.resolveVariables(command.prompt, selection = prefixText, clipboardText = clip)
            val newText = prefixText + resolved
            handleTextReplacer(source, newText, text, command)
            return
        }

        processCommand(source, cleanText, command, clip)
    }

    private fun processCommand(
        source: AccessibilityNodeInfo,
        text: String,
        command: Command,
        clipboardText: String,
        keepPrefix: String = "",
        fallbackText: String? = null,
        keepSuffix: String = ""
    ) {
        val prefs = applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val providerType = prefs.getString("provider_type", "openrouter") ?: "openrouter"
        val model: String
        val endpoint: String

        if (providerType == "openrouter") {
            model = prefs.getString("openrouter_model", "google/gemma-4-31b-it:free") ?: "google/gemma-4-31b-it:free"
            endpoint = "https://openrouter.ai/api/v1"
        } else if (providerType == "custom") {
            model = prefs.getString("custom_model", "") ?: ""
            endpoint = prefs.getString("custom_endpoint", "") ?: ""
            if (model.isBlank() || endpoint.isBlank()) {
                serviceScope.launch { showToast("Custom provider not configured. Set endpoint and model in Settings.") }
                isProcessing = false
                return
            }
        } else if (providerType == "groq") {
            model = prefs.getString("groq_model", "llama-3.3-70b-versatile") ?: "llama-3.3-70b-versatile"
            endpoint = ""
        } else {
            model = prefs.getString("model", "gemini-3.1-flash-lite-preview") ?: "gemini-3.1-flash-lite-preview"
            endpoint = ""
        }
        
        val temperature = prefs.getFloat("temperature", 0.5f).toDouble()
        val screenText = if (command.prompt.contains("{screen}", ignoreCase = true)) getScreenText(source) else ""
        val basePrompt = commandManager.resolveVariables(command.prompt, selection = text, clipboardText = clipboardText, screenText = screenText, appName = getAppName(source))
        val promptToSend = basePrompt + personalStyleBlock(command, prefs)

        currentJob = serviceScope.launch {
            val originalText = text
            val restoreText = fallbackText ?: (keepPrefix + text + keepSuffix)
            var spinnerJob: Job? = null
            try {
                withTimeout(90_000) {
                    val maxAttempts = keyManager.getKeys().size.coerceAtLeast(1)
                    var lastErrorMsg: String? = null
                    var succeeded = false

                    for (attempt in 0 until maxAttempts) {
                        val key = keyManager.getNextKey() ?: break

                        if (spinnerJob == null) {
                            spinnerJob = startInlineSpinner(source, keepPrefix + originalText)
                        }

                        // ── Pure Kotlin ScribeApiClient HTTP Call ───────────
                        val result = when (providerType) {
                            "openrouter" -> com.scribe.app.api.ScribeApiClient.openaiGenerate(promptToSend, text, key, model, temperature, "https://openrouter.ai/api/v1")
                            "custom"     -> com.scribe.app.api.ScribeApiClient.openaiGenerate(promptToSend, text, key, model, temperature, endpoint)
                            "groq"       -> com.scribe.app.api.ScribeApiClient.groqGenerate(promptToSend, text, key, model, temperature)
                            else         -> com.scribe.app.api.ScribeApiClient.geminiGenerate(promptToSend, text, key, model, temperature)
                        }

                        usageManager.recordRequest(key, result.isSuccess)

                        if (result.isSuccess) {
                            spinnerJob?.cancel(); spinnerJob = null
                            lastOriginalText = originalText
                            val generatedText = result.getOrThrow()
                            replaceText(source, keepPrefix + generatedText + keepSuffix)
                            historyManager.addHistoryItem(originalText, generatedText, command.trigger)
                            performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            succeeded = true
                            break
                        }

                        val msg = result.exceptionOrNull()?.message ?: ""
                        lastErrorMsg = msg
                        val isRateLimit = msg.contains("Rate limit", ignoreCase = true)
                        val isInvalidKey = msg.contains("Invalid API key", ignoreCase = true) ||
                                           msg.contains("API key not valid", ignoreCase = true)

                        if (isRateLimit) {
                            val seconds = Regex("retry after (\\d+)s").find(msg)?.groupValues?.get(1)?.toLongOrNull() ?: 60
                            keyManager.reportRateLimit(key, seconds)
                        } else if (isInvalidKey) {
                            keyManager.markInvalid(key)
                        } else {
                            break
                        }
                    }

                    // Backup: the AI built into the phone, for no signal or no key.
                    var localError: String? = null
                    if (!succeeded && prefs.getBoolean("offline_fallback", true)) {
                        if (spinnerJob == null) spinnerJob = startInlineSpinner(source, keepPrefix + originalText)
                        val local = com.scribe.app.api.OnDeviceAi.generate(promptToSend, text)
                        if (local.isSuccess) {
                            spinnerJob?.cancel(); spinnerJob = null
                            lastOriginalText = originalText
                            val generatedText = local.getOrThrow()
                            replaceText(source, keepPrefix + generatedText + keepSuffix)
                            historyManager.addHistoryItem(originalText, generatedText, command.trigger)
                            performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            showToast("Done on your phone (offline)")
                            succeeded = true
                        } else {
                            localError = local.exceptionOrNull()?.message
                        }
                    }

                    if (!succeeded) {
                        spinnerJob?.cancel(); spinnerJob = null
                        replaceText(source, restoreText)
                        performHapticFeedback(HapticFeedbackConstants.REJECT)
                        when {
                            keyManager.getKeys().isEmpty() && localError != null -> showToast(localError)
                            lastErrorMsg != null -> showToast("Scribe Error: $lastErrorMsg")
                            else -> {
                                val waitMs = keyManager.getShortestWaitTimeMs()
                                if (waitMs != null) {
                                    val waitSec = ((waitMs + 999) / 1000).coerceAtLeast(1)
                                    showToast("API key rate limited. Try again in ${waitSec}s")
                                } else if (keyManager.getKeys().isEmpty()) {
                                    showToast("No API keys configured")
                                } else {
                                    showToast("All API keys are invalid. Please check your keys")
                                }
                            }
                        }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                spinnerJob?.cancel()
                try { replaceText(source, restoreText) } catch (_: Exception) {}
                showToast("Request timed out")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                spinnerJob?.cancel()
                try { replaceText(source, restoreText) } catch (_: Exception) {
                    showToast("Could not restore original text")
                }
                showToast("Scribe Error: ${e.message}")
            } finally {
                withContext(NonCancellable + Dispatchers.Main) {
                    spinnerJob?.cancel()
                    if (!handler.postDelayed({ isProcessing = false }, 500)) isProcessing = false
                }
            }
        }
    }

    private fun handleUndo(source: AccessibilityNodeInfo, currentText: String) {
        currentJob = serviceScope.launch {
            try {
                val previousText = lastOriginalText
                if (previousText == null) {
                    replaceText(source, currentText)
                    performHapticFeedback(HapticFeedbackConstants.REJECT)
                    showToast("Nothing to undo")
                } else {
                    lastOriginalText = currentText
                    replaceText(source, previousText)
                    performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                }
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) { showToast("Could not undo")
            } finally {
                withContext(NonCancellable + Dispatchers.Main) {
                    if (!handler.postDelayed({ isProcessing = false }, 500)) isProcessing = false
                }
            }
        }
    }

    private fun handleTextReplacer(source: AccessibilityNodeInfo, newText: String, originalText: String, command: Command) {
        currentJob = serviceScope.launch {
            try {
                lastOriginalText = originalText
                replaceText(source, newText)
                historyManager.addHistoryItem(originalText, newText, command.trigger)
                performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            } catch (e: Exception) {
                showToast("Scribe Error: ${e.message}")
            } finally {
                withContext(NonCancellable + Dispatchers.Main) {
                    if (!handler.postDelayed({ isProcessing = false }, 500)) isProcessing = false
                }
            }
        }
    }

    private suspend fun replaceText(source: AccessibilityNodeInfo, newText: String) = withContext(Dispatchers.Main) {
        source.refresh()
        val bundle = Bundle()
        bundle.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newText)
        val success = source.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
        if (!success) {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val oldClip = clipboard.primaryClip
            val clipData = ClipData.newPlainText("Scribe Result", newText).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    description.extras = PersistableBundle().apply {
                        putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
                    }
                }
            }
            clipboard.setPrimaryClip(clipData)
            val selectArgs = Bundle().apply {
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 0)
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, source.text?.length ?: 0)
            }
            source.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, selectArgs)
            source.performAction(AccessibilityNodeInfo.ACTION_PASTE)
            handler.postDelayed({ if (oldClip != null) clipboard.setPrimaryClip(oldClip) }, 500)
        }
    }

    private fun setFieldText(source: AccessibilityNodeInfo, text: String) {
        val bundle = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        source.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
    }

    private fun startInlineSpinner(source: AccessibilityNodeInfo, baseText: String): Job {
        return serviceScope.launch(Dispatchers.Main) {
            var frameIndex = 0
            while (isActive) {
                setFieldText(source, "$baseText ${SPINNER_FRAMES[frameIndex]}")
                frameIndex = (frameIndex + 1) % SPINNER_FRAMES.size
                delay(80)
            }
        }
    }

    private suspend fun showToast(msg: String) = withContext(Dispatchers.Main) {
        dismissOverlayToast()
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val textView = TextView(applicationContext).apply {
            text = msg
            setTextColor(Color.WHITE)
            textSize = 14f
            setPadding(dp(24), dp(12), dp(24), dp(12))
            maxWidth = (resources.displayMetrics.widthPixels * 0.85).toInt()
            background = GradientDrawable().apply {
                setColor(TOAST_BACKGROUND_COLOR)
                cornerRadius = dp(24).toFloat()
            }
            gravity = Gravity.CENTER
            alpha = 0f
            translationY = dp(TOAST_SLIDE_DISTANCE_DP).toFloat()
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = dp(TOAST_BOTTOM_MARGIN_DP)
            windowAnimations = 0
        }
        try {
            wm.addView(textView, params)
            currentOverlayToast = textView
            AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(textView, View.ALPHA, 0f, 1f),
                    ObjectAnimator.ofFloat(textView, View.TRANSLATION_Y, dp(TOAST_SLIDE_DISTANCE_DP).toFloat(), 0f)
                )
                duration = TOAST_ANIM_DURATION_MS
                interpolator = DecelerateInterpolator()
                start()
                enterAnimator = this
            }
            val runnable = Runnable { dismissOverlayToastAnimated() }
            dismissRunnable = runnable
            handler.postDelayed(runnable, TOAST_DURATION_MS)
        } catch (_: Exception) {
            Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun dismissOverlayToast() {
        dismissRunnable?.let { handler.removeCallbacks(it) }
        dismissRunnable = null; enterAnimator?.cancel(); enterAnimator = null
        dismissAnimator?.cancel(); dismissAnimator = null
        currentOverlayToast?.let { view ->
            try { view.visibility = View.GONE; (getSystemService(Context.WINDOW_SERVICE) as WindowManager).removeView(view) } catch (_: Exception) {}
            currentOverlayToast = null
        }
    }

    private fun dismissOverlayToastAnimated() {
        dismissRunnable?.let { handler.removeCallbacks(it) }
        dismissRunnable = null; enterAnimator?.cancel(); enterAnimator = null; dismissAnimator?.cancel()
        currentOverlayToast?.let { view ->
            try {
                val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
                dismissAnimator = AnimatorSet().apply {
                    playTogether(
                        ObjectAnimator.ofFloat(view, View.ALPHA, view.alpha, 0f),
                        ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, view.translationY, dp(TOAST_SLIDE_DISTANCE_DP).toFloat())
                    )
                    duration = TOAST_ANIM_DURATION_MS; interpolator = DecelerateInterpolator()
                    addListener(object : android.animation.AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: android.animation.Animator) {
                            view.visibility = View.GONE
                            try { wm.removeView(view) } catch (_: Exception) {}
                            dismissAnimator = null
                        }
                    })
                    start()
                }
            } catch (_: Exception) {}
            currentOverlayToast = null
        }
    }

    @Suppress("DEPRECATION")
    private fun performHapticFeedback(feedbackType: Int) {
        handler.post {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    val v = vm.defaultVibrator
                    when (feedbackType) {
                        HapticFeedbackConstants.CONFIRM -> v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                        HapticFeedbackConstants.REJECT  -> v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    when (feedbackType) {
                        HapticFeedbackConstants.CONFIRM -> v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                        HapticFeedbackConstants.REJECT  -> v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
                    }
                } else {
                    (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(50)
                }
            } catch (_: Exception) {}
        }
    }

    override fun onInterrupt() { isProcessing = false; currentJob?.cancel() }
    override fun onDestroy() { super.onDestroy(); voiceBubble?.destroy(); dismissOverlayToast(); serviceScope.cancel() }
}
