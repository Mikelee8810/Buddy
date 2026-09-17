package com.scribe.app.manager

import android.content.Context
import android.content.SharedPreferences
import com.scribe.app.model.HistoryItem
import org.json.JSONArray
import org.json.JSONObject

class HistoryManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("history", Context.MODE_PRIVATE)
    
    companion object {
        const val PREF_HISTORY = "history_items"
        const val MAX_HISTORY_ITEMS = 100 // Keep memory and performance reasonable
    }

    fun getHistory(): List<HistoryItem> {
        val historyStr = prefs.getString(PREF_HISTORY, "[]") ?: "[]"
        val arr = JSONArray(historyStr)
        val items = mutableListOf<HistoryItem>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            items.add(
                HistoryItem(
                    id = obj.getString("id"),
                    originalText = obj.getString("originalText"),
                    newText = obj.getString("newText"),
                    commandTrigger = obj.getString("commandTrigger"),
                    timestamp = obj.getLong("timestamp")
                )
            )
        }
        return items
    }

    fun addHistoryItem(originalText: String, newText: String, commandTrigger: String) {
        val currentHistory = getHistory().toMutableList()
        val newItem = HistoryItem(
            originalText = originalText,
            newText = newText,
            commandTrigger = commandTrigger
        )
        currentHistory.add(0, newItem) // Add to the top
        
        // Trim if necessary
        val trimmed = if (currentHistory.size > MAX_HISTORY_ITEMS) {
            currentHistory.subList(0, MAX_HISTORY_ITEMS)
        } else {
            currentHistory
        }
        
        saveHistory(trimmed)
    }

    fun deleteItem(id: String) {
        val currentHistory = getHistory().filter { it.id != id }
        saveHistory(currentHistory)
    }

    fun clearHistory() {
        prefs.edit().remove(PREF_HISTORY).apply()
    }

    private fun saveHistory(items: List<HistoryItem>) {
        val arr = JSONArray()
        for (item in items) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("originalText", item.originalText)
            obj.put("newText", item.newText)
            obj.put("commandTrigger", item.commandTrigger)
            obj.put("timestamp", item.timestamp)
            arr.put(obj)
        }
        prefs.edit().putString(PREF_HISTORY, arr.toString()).apply()
    }
}
