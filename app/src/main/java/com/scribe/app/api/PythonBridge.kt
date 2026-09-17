package com.scribe.app.api

/**
 * Backward-compatible bridge pointing to the pure Kotlin [ScribeApiClient].
 * Eliminates all Chaquopy native dependencies and fixes 16 KB page-size alignment.
 */
object PythonBridge {

    suspend fun geminiValidateKey(apiKey: String): Result<String> =
        ScribeApiClient.geminiValidateKey(apiKey)

    suspend fun geminiGenerate(
        prompt: String,
        text: String,
        apiKey: String,
        model: String,
        temperature: Double
    ): Result<String> =
        ScribeApiClient.geminiGenerate(prompt, text, apiKey, model, temperature)

    suspend fun openaiValidateKey(apiKey: String, endpoint: String): Result<String> =
        ScribeApiClient.openaiValidateKey(apiKey, endpoint)

    suspend fun openaiGenerate(
        prompt: String,
        text: String,
        apiKey: String,
        model: String,
        temperature: Double,
        endpoint: String
    ): Result<String> =
        ScribeApiClient.openaiGenerate(prompt, text, apiKey, model, temperature, endpoint)

    suspend fun groqValidateKey(apiKey: String): Result<String> =
        ScribeApiClient.groqValidateKey(apiKey)

    suspend fun groqGenerate(
        prompt: String,
        text: String,
        apiKey: String,
        model: String,
        temperature: Double
    ): Result<String> =
        ScribeApiClient.groqGenerate(prompt, text, apiKey, model, temperature)
}
