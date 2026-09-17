package com.scribe.app.model

import java.util.UUID

data class HistoryItem(
    val id: String = UUID.randomUUID().toString(),
    val originalText: String,
    val newText: String,
    val commandTrigger: String,
    val timestamp: Long = System.currentTimeMillis()
)
