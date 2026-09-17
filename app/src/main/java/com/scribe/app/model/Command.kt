package com.scribe.app.model

data class Command(
    val trigger: String,
    val prompt: String,
    val isBuiltIn: Boolean = false,
    val isTextReplacer: Boolean = false
)
