package com.scribe.app

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/**
 * Shown after Scribe crashes, so the user sees what happened and can copy
 * the details instead of the app just closing. Built with plain Android
 * views so it still works if the crash was in the Compose UI.
 */
class CrashActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val details = intent.getStringExtra(EXTRA_DETAILS) ?: "No details"
        val pad = (16 * resources.displayMetrics.density).toInt()

        val title = TextView(this).apply {
            text = "Scribe ran into a problem"
            textSize = 20f
            setTextColor(Color.WHITE)
        }
        val hint = TextView(this).apply {
            text = "Tap Copy and send this to whoever is fixing Scribe."
            textSize = 14f
            setTextColor(Color.LTGRAY)
            setPadding(0, pad / 2, 0, pad)
        }
        val copy = Button(this).apply {
            text = "Copy error details"
            setOnClickListener {
                (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                    .setPrimaryClip(ClipData.newPlainText("Scribe crash", details))
                Toast.makeText(this@CrashActivity, "Copied", Toast.LENGTH_SHORT).show()
            }
        }
        val body = TextView(this).apply {
            text = details
            textSize = 11f
            setTextColor(Color.LTGRAY)
            setTextIsSelectable(true)
            setPadding(0, pad, 0, 0)
        }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 3, pad, pad)
            addView(title); addView(hint); addView(copy); addView(body)
        }
        setContentView(ScrollView(this).apply {
            setBackgroundColor(Color.BLACK)
            addView(column)
        })
    }

    companion object {
        const val EXTRA_DETAILS = "details"
    }
}
