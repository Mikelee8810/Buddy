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
        "reply" to "Generate a contextual reply to this message. CRITICAL: You MUST preserve the exact original language, alphabet, and script. For example, if the input is in Hinglish (Hindi written in Latin alphabet), you MUST output in Hinglish. Do NOT translate to Devanagari or any other script. Do not change the original language. Return only the reply.",
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
    fun resolveVariables(template: String, selection: String = "", clipboardText: String = ""): String {
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
    }
}
