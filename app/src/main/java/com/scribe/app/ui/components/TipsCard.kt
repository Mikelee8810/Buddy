package com.scribe.app.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scribe.app.ui.theme.*

/** Short how-to tips. `%p` is replaced with the user's trigger character. */
private val TIPS = listOf(
    "Type your message, add %pfix at the end, and Scribe cleans up spelling and grammar right there.",
    "Made a mistake? Type %pundo at the end of the text to get your original back.",
    "%preply reads the conversation on screen. Type a rough idea, like \"tell him I'm late\", then %preply.",
    "Tap the mic bubble and just talk. Pauses are fine. Tap again when you're done.",
    "Long-press the mic to edit by voice: say \"make this nicer\" or \"shorter, add a thank you\".",
    "Highlight part of your text before a voice edit to change only that part.",
    "Translate anything: add %ptranslate:es (or fr, de, ja…) to the end of your text.",
    "Tap any command in the Hub to see its prompt, then Edit to make it your own.",
    "Changed a built-in prompt and regret it? Open it and tap \"Reset to original prompt\".",
    "Make your own commands with the + button. Use {clipboard}, {screen} or {app} to give the AI more context.",
    "Add \"About me\" in Settings so emails get your name and sign-off right.",
    "Scribe learns your writing style from what you've typed, so rewrites sound like you.",
    "Set up Keep Scribe Alive in Settings once, and Android can't quietly switch Scribe off.",
    "%pbullets turns a wall of text into a list. %ptldr shrinks it to one line.",
    "%pdecline writes a kind \"no\". %pconfident cuts the \"just\", \"maybe\" and \"sorry\".",
    "%ptodo pulls the action items out of a long message as a checklist.",
    "Instant Replace commands don't use AI at all. Great for your address or email signature.",
    "Don't like the slash? Change the trigger character in Settings."
)

@Composable
fun TipsCard(prefix: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    var hidden by remember { mutableStateOf(prefs.getBoolean("tips_hidden", false)) }
    var index by remember {
        // Advance once per screen visit so each visit shows something new.
        val next = (prefs.getInt("tip_index", -1) + 1) % TIPS.size
        prefs.edit().putInt("tip_index", next).apply()
        mutableIntStateOf(next)
    }

    AnimatedVisibility(visible = !hidden, modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(cornerRadius = 20.dp, fillAlpha = 0.18f)
                .clickable {
                    index = (index + 1) % TIPS.size
                    prefs.edit().putInt("tip_index", index).apply()
                }
                .semantics { contentDescription = "Tip. Tap for the next tip." }
        ) {
            Row(
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Outlined.Lightbulb,
                    contentDescription = null,
                    tint = ScribeCobalt,
                    modifier = Modifier.padding(top = 2.dp).size(18.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "TIP ${index + 1} OF ${TIPS.size} · TAP FOR NEXT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = ScribeTextTertiary
                    )
                    Spacer(Modifier.height(4.dp))
                    AnimatedContent(targetState = index, label = "tip") { i ->
                        Text(
                            TIPS[i].replace("%p", prefix),
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            color = ScribeTextPrimary
                        )
                    }
                }
                IconButton(onClick = {
                    hidden = true
                    prefs.edit().putBoolean("tips_hidden", true).apply()
                }) {
                    Icon(Icons.Outlined.Close, contentDescription = "Hide tips", tint = ScribeTextSecondary, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
