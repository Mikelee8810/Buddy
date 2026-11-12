package com.scribe.app.api

import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Gemini Nano running on the phone itself (through Android's AICore).
 * Used as a backup when there's no internet or no API key. Only some
 * phones support it; [isAvailable] says whether this one does.
 */
object OnDeviceAi {
    private val model by lazy { Generation.getClient() }

    suspend fun isAvailable(): Boolean = try {
        model.checkStatus() == FeatureStatus.AVAILABLE
    } catch (_: Throwable) {
        false
    }

    /** Human-readable status for the Settings screen. */
    suspend fun statusLabel(): String = try {
        when (model.checkStatus()) {
            FeatureStatus.AVAILABLE -> "Ready on this phone"
            FeatureStatus.DOWNLOADABLE -> "Supported. Downloads the first time it's needed."
            FeatureStatus.DOWNLOADING -> "Downloading to this phone…"
            else -> "Not supported on this phone"
        }
    } catch (_: Throwable) {
        "Not supported on this phone"
    }

    /** Starts the model download if the phone supports it but doesn't have it yet. */
    suspend fun downloadIfNeeded() {
        try {
            if (model.checkStatus() == FeatureStatus.DOWNLOADABLE) model.download().collect { }
        } catch (_: Throwable) {
        }
    }

    suspend fun generate(instruction: String, text: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            when (model.checkStatus()) {
                FeatureStatus.AVAILABLE -> Unit
                FeatureStatus.DOWNLOADABLE -> {
                    downloadIfNeeded()
                    if (model.checkStatus() != FeatureStatus.AVAILABLE) return@withContext Result.failure(
                        Exception("The phone's AI is downloading. Try again in a few minutes.")
                    )
                }
                FeatureStatus.DOWNLOADING -> return@withContext Result.failure(
                    Exception("The phone's AI is still downloading. Try again in a few minutes.")
                )
                else -> return@withContext Result.failure(
                    Exception("This phone doesn't support on-device AI. Add an API key in Settings.")
                )
            }
            val prompt = "$instruction\n\nText:\n$text"
            val out = model.generateContent(prompt).candidates.firstOrNull()?.text?.trim().orEmpty()
            if (out.isEmpty()) Result.failure(Exception("On-device AI returned nothing"))
            else Result.success(out)
        } catch (e: Throwable) {
            Result.failure(Exception(e.message ?: "On-device AI failed"))
        }
    }
}
