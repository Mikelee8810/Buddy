<div align="center">

<img src="logo.png" width="160" alt="Scribe logo" style="border-radius: 22px;" />

# Scribe

**An AI writing helper that works in any text box, in any Android app.**

Type a command like `/fix` at the end of your text, or just talk, and Scribe rewrites it in place.

[![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=flat-square&logo=android&logoColor=white)](#requirements)
[![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack_Compose-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](#tech-stack)
[![Release](https://img.shields.io/badge/Release-v2.0-blueviolet?style=flat-square)](#download)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square)](LICENSE)

[Download](#download) · [Getting started](#getting-started) · [Commands](#commands) · [Report a bug](https://github.com/Mikelee8810/Buddy/issues)

</div>

---

## What Scribe does

Scribe is an Android accessibility service. It watches the text box you are typing in, and when your text ends with a command (for example `/formal`), it sends the text to the AI provider you chose and replaces it with the result. Nothing to copy and paste, and no special keyboard.

## What's new in 2.0

- **Voice dictation.** A mic bubble appears when you tap into a text box. Tap it and talk; pauses are fine. Scribe removes filler words and cleans up the result.
- **Edit by voice.** Long-press the mic and say what to change ("make this friendlier", "shorter, add a thank you"). Only the highlighted text changes if you highlight some first.
- **Screen-aware replies.** `/reply` reads the visible conversation and the app you are in, so replies fit the chat and match the app's usual tone.
- **Sounds like you.** Rewrites borrow from text you have typed before, plus an optional "About me" note (name, sign-off, words to avoid).
- **Keep alive with Shizuku.** A one-time setup lets Scribe turn its own accessibility service back on if Android switches it off.
- **12 new built-in commands**, editable prompts with a per-command reset, tips for learning the app, and a full backup that includes keys and settings.
- **New look.** Material 3 Expressive theme, larger touch targets, plain-language labels.

## Download

Download the latest APK from [Releases](https://github.com/Mikelee8810/Buddy/releases/latest), or from the **Scribe** artifact on the latest successful [Actions build](https://github.com/Mikelee8810/Buddy/actions).

Android only installs an update over an existing copy when both are signed with the same key. If Android says "App not installed", export a backup in **Settings → Backup & Restore**, uninstall the old copy, install the new one, and restore.

### Requirements

- Android 8.0 (API 26) or newer
- An API key from Google Gemini, Groq, OpenRouter, or any OpenAI-compatible provider

## Getting started

1. Install the APK.
2. Open Scribe → **Settings → API keys** and add a key. Free keys are available from [Google AI Studio](https://aistudio.google.com/app/apikey) and [Groq](https://console.groq.com/keys).
3. On the Hub screen, tap the status card and turn on **Scribe** in Android's Accessibility settings.
4. In any app, type some text, add `/fix` at the end, and wait a moment.
5. Optional: turn on **Voice Dictation** and **Keep Scribe Alive** in Settings.

## Commands

| Command | What it does |
|---|---|
| `/fix` | Fix spelling, grammar and punctuation |
| `/improve` | Make it clearer and easier to read |
| `/shorten` · `/expand` | Make it shorter or add detail |
| `/formal` · `/casual` | Change the tone |
| `/simplify` | Plain words and short sentences |
| `/confident` · `/soften` | Remove hedging, or make it gentler |
| `/reply` | Write a reply that fits the conversation on screen |
| `/email` | Turn rough notes into an email |
| `/decline` · `/apology` | Say no kindly, or apologize briefly |
| `/bullets` · `/tldr` · `/todo` | List, one-line summary, or action items |
| `/title` · `/hashtags` · `/emoji` | Title, hashtags, or emojis |
| `/explain` | Explain the text in plain words |
| `/translate:es` | Translate (any 2–5 letter language code) |
| `/ramble` | Clean up dictated speech (used by the mic) |
| `/undo` | Restore the text from before the last rewrite |

Every built-in prompt can be edited in the Hub, and edited prompts can be reset to the original. The trigger character (`/`) can be changed in Settings.

### Custom commands

Tap **New** in the Hub. A command is either an **AI rewrite** (a prompt) or an **instant replace** (fixed text, no AI). Prompts can use these placeholders:

| Placeholder | Filled with |
|---|---|
| `{selection}` | The text you typed |
| `{clipboard}` | Your clipboard |
| `{screen}` | Visible text on screen (password fields are skipped) |
| `{app}` | The name of the app you are typing in |
| `{date}` · `{time}` | Current date and time |

## Privacy

- Text is only sent when you use a command or the mic, and only to the provider you chose.
- `{screen}` is only read when a prompt uses it, and password fields are always skipped.
- Scribe does nothing in password managers, authenticator apps, or common banking and payment apps.
- API keys are encrypted with the Android Keystore. Note that **Export** puts keys in the backup text, so keep backups private.

### Permissions

| Permission | Why |
|---|---|
| Accessibility service | Read and replace text in the focused text box |
| Internet | Call your AI provider |
| Microphone | Voice dictation (only when you turn it on) |
| Run at startup | Turn Scribe back on after a restart (with Keep Alive set up) |
| Write secure settings | Granted through Shizuku only if you set up Keep Alive |

## Tech stack

Kotlin, Jetpack Compose with Material 3 Expressive, OkHttp, WorkManager, Android SpeechRecognizer and Shizuku.

```
app/src/main/java/com/scribe/app/
├── service/     AssistantService (accessibility), VoiceBubble (mic)
├── api/         ScribeApiClient (Gemini, Groq, OpenAI-compatible), OnDeviceAi
├── manager/     Commands, history, keys, usage
├── keepalive/   Shizuku setup, boot receiver, periodic check
└── ui/          Hub, History, Settings, Key usage, theme and components
```

## Building from source

See [HOW_TO_BUILD.md](HOW_TO_BUILD.md). Every push to a pull request is also built by GitHub Actions, and the APK is attached to the run.

## License

MIT. See [LICENSE](LICENSE).
