package com.scribe.app.service

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView

/**
 * Floating mic bubble shown over a focused text field.
 *
 * Tap once to start listening, tap again to stop. Android's recognizer
 * stops on its own after a pause, so while the bubble is "on" we keep
 * restarting it and stitch the pieces together. That lets the user
 * ramble for as long as they want, with pauses.
 */
class VoiceBubble(
    private val context: Context,
    private val onTranscript: (text: String, editMode: Boolean) -> Unit,
    private val onError: (String) -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())
    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var view: TextView? = null
    private var recognizer: SpeechRecognizer? = null
    private val buffer = StringBuilder()
    private var delivered = true

    var isListening = false
        private set
    private var editMode = false

    private fun dp(value: Int): Int =
        (value * context.resources.displayMetrics.density + 0.5f).toInt()

    fun show() {
        if (view != null) return
        val bubble = TextView(context).apply {
            text = "🎙"
            textSize = 20f
            gravity = Gravity.CENTER
            contentDescription = "Scribe voice input"
            setOnClickListener { editMode = false; toggle() }
            setOnLongClickListener {
                if (!isListening) { editMode = true; start() }
                true
            }
        }
        styleBubble(bubble, listening = false)
        val size = dp(48)
        val params = WindowManager.LayoutParams(
            size, size,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            x = dp(8)
        }
        try {
            wm.addView(bubble, params)
            view = bubble
        } catch (_: Exception) {
        }
    }

    fun hide() {
        if (isListening) return
        view?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        view = null
    }

    fun destroy() {
        isListening = false
        recognizer?.destroy()
        recognizer = null
        view?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        view = null
    }

    private fun styleBubble(v: TextView, listening: Boolean) {
        v.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(
                if (!listening) 0xE62F4FD8.toInt()
                else if (editMode) 0xFF8B5CF6.toInt()
                else 0xFFE5484D.toInt()
            )
            setStroke(dp(2), Color.WHITE)
        }
        v.text = if (!listening) "🎙" else if (editMode) "✎" else "■"
        v.setTextColor(Color.WHITE)
    }

    private fun toggle() {
        if (isListening) stop() else start()
    }

    private fun start() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Speech recognition isn't available on this phone")
            return
        }
        buffer.clear()
        delivered = false
        isListening = true
        view?.let { styleBubble(it, true) }
        listenOnce()
    }

    private fun stop() {
        isListening = false
        view?.let { styleBubble(it, false) }
        recognizer?.stopListening()
        // If the recognizer never calls back, deliver what we have anyway.
        handler.postDelayed({ deliver() }, 2_500)
    }

    private fun listenOnce() {
        val r = recognizer ?: SpeechRecognizer.createSpeechRecognizer(context).also {
            it.setRecognitionListener(listener)
            recognizer = it
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 4_000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 4_000L)
        }
        try {
            r.startListening(intent)
        } catch (e: Exception) {
            isListening = false
            view?.let { styleBubble(it, false) }
            onError("Couldn't start the mic: ${e.message}")
        }
    }

    private fun deliver() {
        if (delivered) return
        delivered = true
        val text = buffer.toString().trim()
        buffer.clear()
        if (text.isNotEmpty()) onTranscript(text, editMode)
        editMode = false
    }

    private val listener = object : RecognitionListener {
        override fun onResults(results: Bundle?) {
            val best = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
            if (!best.isNullOrBlank()) {
                if (buffer.isNotEmpty()) buffer.append(' ')
                buffer.append(best)
            }
            if (isListening) listenOnce() else deliver()
        }

        override fun onError(error: Int) {
            val keepGoing = error == SpeechRecognizer.ERROR_NO_MATCH ||
                error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
            if (isListening && keepGoing) {
                listenOnce()
                return
            }
            if (isListening) {
                isListening = false
                view?.let { styleBubble(it, false) }
                if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                    onError("Scribe needs microphone permission. Turn it on in Scribe Settings.")
                }
            }
            deliver()
        }

        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }
}
