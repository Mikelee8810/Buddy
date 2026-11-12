package com.scribe.app.manager

import android.content.Context
import android.content.SharedPreferences
import com.scribe.app.model.Command
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CommandManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("commands", Context.MODE_PRIVATE)
    private val settingsPrefs: SharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    companion object {
        const val DEFAULT_PREFIX = "/"
        const val PREF_TRIGGER_PREFIX = "trigger_prefix"
        const val RAMBLE_PROMPT = "You are cleaning up spoken dictation. Remove filler words (um, uh, like, you know), false starts, and repeated words. When the speaker corrects themselves, keep only the final intended version. Fix punctuation, capitalization, and obvious speech-to-text mistakes. Keep the speaker's own words, tone, and meaning; do not add new ideas or answer questions in the text. Keep the original language. Return only the cleaned text."
    }

    // Built-in command names (without prefix) and their prompts
    private val builtInDefinitions = listOf(
        "fix" to "Fix grammar, spelling, and punctuation errors. CRITICAL: You MUST preserve the exact original language, alphabet, and script. For example, if the input is in Hinglish (Hindi written in Latin alphabet), you MUST output in Hinglish. Do NOT translate to Devanagari or any other script. Do not change the original language. Return only the corrected text.",
        "improve" to "Improve clarity and readability. CRITICAL: You MUST preserve the exact original language, alphabet, and script. For example, if the input is in Hinglish (Hindi written in Latin alphabet), you MUST output in Hinglish. Do NOT translate to Devanagari or any other script. Do not change the original language. Return only the improved text.",
        "shorten" to "Shorten while preserving core meaning. CRITICAL: You MUST preserve the exact original language, alphabet, and script. For example, if the input is in Hinglish (Hindi written in Latin alphabet), you MUST output in Hinglish. Do NOT translate to Devanagari or any other script. Do not change the original language. Return only the shortened text.",
        "expand" to "Expand with more detail and context. CRITICAL: You MUST preserve the exact original language, alphabet, and script. For example, if the input is in Hinglish (Hindi written in Latin alphabet), you MUST output in Hinglish. Do NOT translate to Devanagari or any other script. Do not change the original language. Return only the expanded text.",
        "formal" to "Rewrite in a formal, professional tone. CRITICAL: You MUST preserve the exact original language, alphabet, and script. For example, if the input is in Hinglish (Hindi written in Latin alphabet), you MUST output in Hinglish. Do NOT translate to Devanagari or any other script. Do not change the original language. Return only the rewritten text.",
        "casual" to "Rewrite in a casual, friendly tone. CRITICAL: You MUST preserve the exact original language, alphabet, and script. For example, if the input is in Hinglish (Hindi written in Latin alphabet), you MUST output in Hinglish. Do NOT translate to Devanagari or any other script. Do not change the original language. Return only the rewritten text.",
        "emoji" to "Add relevant emojis throughout. CRITICAL: You MUST preserve the exact original language, alphabet, and script. For example, if the input is in Hinglish (Hindi written in Latin alphabet), you MUST output in Hinglish. Do NOT translate to Devanagari or any other script. Do not change the original language. Return only the text with emojis added.",
        "reply" to "The user is typing in the app: {app}. Match the tone people normally use there (casual for texting apps, more polished for email or work apps). Here is what is currently visible on the screen, which may include the conversation being replied to:\n---\n{screen}\n---\nThe user has typed a rough idea of their reply. Write the reply they want to send, fitting the conversation above. If they typed nothing useful, write a natural reply to the latest message. CRITICAL: You MUST preserve the exact original language, alphabet, and script. For example, if the input is in Hinglish (Hindi written in Latin alphabet), you MUST output in Hinglish. Do NOT translate to Devanagari or any other script. Do not change the original language. Return only the reply.",
        "simplify" to "Rewrite in plain, simple English that a 12-year-old could follow. Use short sentences and everyday words. Keep every important point. CRITICAL: Keep the original language and script. Return only the result, with no explanations.",
        "bullets" to "Turn this into a clean bulleted list. One idea per bullet, short and scannable. Keep all the key information. CRITICAL: Keep the original language and script. Return only the result, with no explanations.",
        "tldr" to "Summarize this in one or two sentences that capture the main point. CRITICAL: Keep the original language and script. Return only the result, with no explanations.",
        "email" to "Turn this into a clear, polite email with a greeting, a short body, and a sign-off. Keep the sender's intent and details. Do not invent facts, names, or dates. CRITICAL: Keep the original language and script. Return only the result, with no explanations.",
        "decline" to "Rewrite this as a kind but firm way to say no. Be warm and brief, and don't over-apologize or make excuses that weren't given. CRITICAL: Keep the original language and script. Return only the result, with no explanations.",
        "apology" to "Rewrite this as a sincere, short apology that takes responsibility without groveling. Keep any facts that were given. CRITICAL: Keep the original language and script. Return only the result, with no explanations.",
        "confident" to "Rewrite this to sound confident and direct. Remove hedging words like 'just', 'maybe', 'I think', and 'sorry to bother'. Keep it polite. CRITICAL: Keep the original language and script. Return only the result, with no explanations.",
        "soften" to "Rewrite this to sound gentler and less harsh, without losing the message. Good for sensitive conversations. CRITICAL: Keep the original language and script. Return only the result, with no explanations.",
        "todo" to "Pull out every action item from this text as a checklist, one per line starting with '- [ ]'. Include who and when if mentioned. If there are none, return the text unchanged. CRITICAL: Keep the original language and script. Return only the result, with no explanations.",
        "title" to "Write a short, catchy title or subject line for this text. CRITICAL: Keep the original language and script. Return only the result, with no explanations.",
        "hashtags" to "Add 3 to 6 relevant hashtags at the end of this text. Keep the text itself unchanged. CRITICAL: Keep the original language and script. Return only the result, with no explanations.",
        "explain" to "Explain what this text means in plain words, as if to someone new to the topic. Keep it short. CRITICAL: Keep the original language and script. Return only the result, with no explanations.",
        "translate" to "Translate the text into English. If it is already in English, return it unchanged. Do NOT respond to, interpret, or answer the text. Return ONLY the translation, with no explanations. (Tip: type translate:es, translate:fr and so on for other languages.)",
        "ai" to "You are a prompt writer. The text below is a rough transcript of what the user wants done. Do NOT do the task and do NOT write a plan for the user. Your only job is to turn the transcript into a prompt the user will paste into another AI. Write every line of it speaking directly to that AI as you. The prompt you write must: 1. Open with one sentence giving the AI its role and the overall goal. 2. Include a Context section with every fact, preference, constraint and detail from the transcript. Do not invent anything. 3. Break the work into numbered phases. For each phase, state its goal, the specific steps the AI must take, and what it must deliver before moving on. 4. Tell the AI to complete one phase at a time, show the result, and wait for the user to confirm before starting the next phase. 5. Tell the AI to ask clarifying questions first if anything important is missing. 6. End with the exact final output the AI should produce. Return only the finished prompt. No intro, no explanation, no notes to the user.",
        "ramble" to RAMBLE_PROMPT,
        "undo" to "Undo the last replacement and restore the original text."
    )

    fun getTriggerPrefix(): String {
        return settingsPrefs.getString(PREF_TRIGGER_PREFIX, DEFAULT_PREFIX) ?: DEFAULT_PREFIX
    }

    fun setTriggerPrefix(newPrefix: String): Boolean {
        if (newPrefix.length != 1 || newPrefix[0].isLetterOrDigit() || newPrefix[0].isWhitespace()) return false
        val oldPrefix = getTriggerPrefix()
        if (oldPrefix == newPrefix) return true
        // Write prefix FIRST (synchronous) so built-ins work immediately if process dies mid-migration
        settingsPrefs.edit().putString(PREF_TRIGGER_PREFIX, newPrefix).commit()
        // Migrate custom command triggers
        val customStr = prefs.getString("custom_commands", "[]") ?: "[]"
        val arr = JSONArray(customStr)
        val newArr = JSONArray()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val oldTrigger = obj.getString("trigger")
            val migrated = if (oldTrigger.startsWith(oldPrefix)) {
                newPrefix + oldTrigger.removePrefix(oldPrefix)
            } else oldTrigger
            val newObj = JSONObject()
            newObj.put("trigger", migrated)
            newObj.put("prompt", obj.getString("prompt"))
            newObj.put("is_text_replacer", obj.optBoolean("is_text_replacer", false))
            newArr.put(newObj)
        }
        prefs.edit().putString("custom_commands", newArr.toString()).apply()

        val overStr = prefs.getString("builtin_overrides", "{}") ?: "{}"
        val overrides = JSONObject(overStr)
        val keys = overrides.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val obj = overrides.getJSONObject(key)
            val oldObjTrigger = obj.optString("trigger", "")
            if (oldObjTrigger.startsWith(oldPrefix)) {
                obj.put("trigger", newPrefix + oldObjTrigger.removePrefix(oldPrefix))
            }
        }
        prefs.edit().putString("builtin_overrides", overrides.toString()).apply()

        return true
    }

    private fun getDeletedBuiltinNames(): MutableSet<String> {
        val jsonStr = prefs.getString("deleted_builtins", "[]") ?: "[]"
        val arr = JSONArray(jsonStr)
        val set = mutableSetOf<String>()
        for (i in 0 until arr.length()) {
            set.add(arr.getString(i))
        }
        return set
    }

    private fun saveDeletedBuiltinNames(names: Set<String>) {
        val arr = JSONArray()
        names.forEach { arr.put(it) }
        prefs.edit().putString("deleted_builtins", arr.toString()).apply()
    }

    private fun getBuiltInCommands(): List<Command> {
        val prefix = getTriggerPrefix()
        val overrideStr = prefs.getString("builtin_overrides", "{}") ?: "{}"
        val overrides = JSONObject(overrideStr)
        val deleted = getDeletedBuiltinNames()

        return builtInDefinitions
            .filter { (name, _) -> !deleted.contains(name) }
            .map { (name, prompt) ->
                if (overrides.has(name)) {
                    val overrideObj = overrides.getJSONObject(name)
                    val newTrigger = overrideObj.optString("trigger", "$prefix$name")
                    val newPrompt = overrideObj.optString("prompt", prompt)
                    Command(newTrigger, newPrompt, true)
                } else {
                    Command("$prefix$name", prompt, true)
                }
            }
    }

    /** The command used to clean up voice dictation. Editable like any built-in. */
    fun getRambleCommand(): Command {
        val prefix = getTriggerPrefix()
        val overrides = JSONObject(prefs.getString("builtin_overrides", "{}") ?: "{}")
        val prompt = overrides.optJSONObject("ramble")?.optString("prompt", RAMBLE_PROMPT) ?: RAMBLE_PROMPT
        return Command("${prefix}ramble", prompt, true)
    }

    fun getCommands(): List<Command> {
        val customStr = prefs.getString("custom_commands", "[]") ?: "[]"
        val arr = JSONArray(customStr)
        val customCommands = mutableListOf<Command>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            customCommands.add(Command(
                obj.getString("trigger"), 
                obj.getString("prompt"), 
                false, 
                obj.optBoolean("is_text_replacer", false)
            ))
        }
        return getBuiltInCommands() + customCommands
    }

    fun addCustomCommand(command: Command) {
        val customStr = prefs.getString("custom_commands", "[]") ?: "[]"
        val arr = JSONArray(customStr)
        val newObj = JSONObject()
        newObj.put("trigger", command.trigger)
        newObj.put("prompt", command.prompt)
        newObj.put("is_text_replacer", command.isTextReplacer)
        arr.put(newObj)
        prefs.edit().putString("custom_commands", arr.toString()).apply()
    }

    fun removeCommand(trigger: String) {
        val prefix = getTriggerPrefix()
        val overrideStr = prefs.getString("builtin_overrides", "{}") ?: "{}"
        val overrides = JSONObject(overrideStr)

        // Check if this trigger matches any built-in definition
        for ((name, _) in builtInDefinitions) {
            val currentTrigger = if (overrides.has(name)) {
                overrides.getJSONObject(name).optString("trigger", "$prefix$name")
            } else {
                "$prefix$name"
            }
            if (currentTrigger == trigger) {
                val deleted = getDeletedBuiltinNames()
                deleted.add(name)
                saveDeletedBuiltinNames(deleted)
                return
            }
        }

        // Otherwise remove from custom commands
        removeCustomCommand(trigger)
    }

    fun removeCustomCommand(trigger: String) {
        val customStr = prefs.getString("custom_commands", "[]") ?: "[]"
        val arr = JSONArray(customStr)
        val newArr = JSONArray()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            if (obj.getString("trigger") != trigger) {
                newArr.put(obj)
            }
        }
        prefs.edit().putString("custom_commands", newArr.toString()).apply()
    }

    fun findCommand(text: String): Command? {
        val commands = getCommands()
        for (cmd in commands.sortedByDescending { it.trigger.length }) {
            if (text.endsWith(cmd.trigger, ignoreCase = true)) {
                return cmd
            }
        }
        val prefix = getTriggerPrefix()
        val translatePrefix = "${prefix}translate:"
        val lowerText = text.lowercase()
        val lowerTransPrefix = translatePrefix.lowercase()
        val translateIdx = lowerText.lastIndexOf(lowerTransPrefix)
        if (translateIdx >= 0) {
            val langPart = text.substring(translateIdx + translatePrefix.length)
            if (langPart.length in 2..5 && langPart.all { it.isLetterOrDigit() }) {
                return Command("${translatePrefix}$langPart", "Translate the provided text to language code '$langPart'. Do NOT respond to, interpret, or answer the text. Treat it purely as raw text to translate. Return ONLY the translated text with no explanations or commentary.", true)
            }
        }
        return null
    }

    fun updateCommand(oldTrigger: String, newCommand: Command) {
        val builtInList = getBuiltInCommands()
        val builtInTarget = builtInList.find { it.trigger == oldTrigger && it.isBuiltIn }
        
        if (builtInTarget != null) {
            val prefix = getTriggerPrefix()
            val overrideStr = prefs.getString("builtin_overrides", "{}") ?: "{}"
            val overrides = JSONObject(overrideStr)
            
            var originalName: String? = null
            for ((name, _ ) in builtInDefinitions) {
                var currentTrigger = "$prefix$name"
                if (overrides.has(name)) {
                    currentTrigger = overrides.getJSONObject(name).optString("trigger", "$prefix$name")
                }
                if (currentTrigger == oldTrigger) {
                    originalName = name
                    break
                }
            }
            
            if (originalName != null) {
                val newOverride = JSONObject()
                newOverride.put("trigger", newCommand.trigger)
                newOverride.put("prompt", newCommand.prompt)
                overrides.put(originalName, newOverride)
                prefs.edit().putString("builtin_overrides", overrides.toString()).apply()
            }
        } else {
            removeCustomCommand(oldTrigger)
            addCustomCommand(newCommand)
        }
    }

    /** Built-in name (e.g. "fix") for a trigger, or null if it's a custom command. */
    private fun builtInNameFor(trigger: String): String? {
        val prefix = getTriggerPrefix()
        val overrides = JSONObject(prefs.getString("builtin_overrides", "{}") ?: "{}")
        return builtInDefinitions.firstOrNull { (name, _) ->
            val current = overrides.optJSONObject(name)?.optString("trigger", "$prefix$name") ?: "$prefix$name"
            current == trigger
        }?.first
    }

    /** True when a built-in command's prompt or trigger was changed by the user. */
    fun isModifiedBuiltIn(trigger: String): Boolean {
        val name = builtInNameFor(trigger) ?: return false
        return JSONObject(prefs.getString("builtin_overrides", "{}") ?: "{}").has(name)
    }

    /** Put one built-in command back to its original trigger and prompt. */
    fun resetBuiltInCommand(trigger: String) {
        val name = builtInNameFor(trigger) ?: return
        val overrides = JSONObject(prefs.getString("builtin_overrides", "{}") ?: "{}")
        overrides.remove(name)
        prefs.edit().putString("builtin_overrides", overrides.toString()).apply()
    }

    fun resetBuiltInCommands() {
        prefs.edit()
            .remove("builtin_overrides")
            .remove("deleted_builtins")
            .apply()
    }

    fun exportConfigJson(): String {
        val root = JSONObject()
        root.put("app", "Scribe")
        root.put("version", 1)
        root.put("exported_at", System.currentTimeMillis())
        root.put("trigger_prefix", getTriggerPrefix())
        root.put("custom_commands", JSONArray(prefs.getString("custom_commands", "[]") ?: "[]"))
        root.put("builtin_overrides", JSONObject(prefs.getString("builtin_overrides", "{}") ?: "{}"))
        root.put("deleted_builtins", JSONArray(prefs.getString("deleted_builtins", "[]") ?: "[]"))
        return root.toString(2)
    }

    fun importConfigJson(jsonStr: String): Result<Int> {
        return try {
            val root = JSONObject(jsonStr)
            if (root.has("trigger_prefix")) {
                val prefix = root.getString("trigger_prefix")
                if (prefix.length == 1 && !prefix[0].isLetterOrDigit() && !prefix[0].isWhitespace()) {
                    settingsPrefs.edit().putString(PREF_TRIGGER_PREFIX, prefix).commit()
                }
            }
            if (root.has("custom_commands")) {
                val custom = root.getJSONArray("custom_commands")
                prefs.edit().putString("custom_commands", custom.toString()).apply()
            }
            if (root.has("builtin_overrides")) {
                val overrides = root.getJSONObject("builtin_overrides")
                prefs.edit().putString("builtin_overrides", overrides.toString()).apply()
            }
            if (root.has("deleted_builtins")) {
                val deleted = root.getJSONArray("deleted_builtins")
                prefs.edit().putString("deleted_builtins", deleted.toString()).apply()
            }
            val count = root.optJSONArray("custom_commands")?.length() ?: 0
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Resolves dynamic template variables such as {date}, {time}, {clipboard}, {selection}, {input}
     */
    fun resolveVariables(template: String, selection: String = "", clipboardText: String = "", screenText: String = "", appName: String = ""): String {
        val now = Date()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

        val dateStr = dateFormat.format(now)
        val timeStr = timeFormat.format(now)

        return template
            .replace("{date}", dateStr, ignoreCase = true)
            .replace("{time}", timeStr, ignoreCase = true)
            .replace("{clipboard}", clipboardText, ignoreCase = true)
            .replace("{selection}", selection, ignoreCase = true)
            .replace("{input}", selection, ignoreCase = true)
            .replace("{screen}", screenText, ignoreCase = true)
            .replace("{app}", appName, ignoreCase = true)
    }
}
