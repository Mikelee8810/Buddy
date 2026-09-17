# Scribe (Python Edition) — Build Guide

## What this is
Identical to the original Scribe app — same UI, same features, same Kotlin Accessibility
Service — but with **Python handling all AI calls** via Chaquopy (Python embedded in the APK).
No Termux. No server. One APK, install and go.

---

## Architecture

```
Your phone (single APK)
│
├── Kotlin  ── Accessibility Service (detects ?fix, ?formal, etc.)
│           ── Jetpack Compose UI   (Dashboard / Keys / Commands / Settings)
│           ── KeyManager          (AES-256-GCM encrypted key storage)
│           ── CommandManager      (built-in + custom triggers)
│           ── PythonBridge.kt     (calls Python via Chaquopy)
│
└── Python  ── gemini_client.py    (Gemini API calls)
            ── openai_client.py    (OpenAI-compatible API calls)
            └── (runs inside the APK — no Termux needed)
```

---

## Step 1 — Install Android Studio

Download from: https://developer.android.com/studio
Install with default settings. When it asks to install the Android SDK, say Yes.

---

## Step 2 — Open the project

1. Open Android Studio
2. Click **"Open"** (not "New Project")
3. Browse to this folder (`ScribePy`) and click **OK**
4. Wait for Gradle sync to finish (first time downloads ~500 MB — just wait)

---

## Step 3 — Connect your phone

1. On your Android phone, go to **Settings → About Phone**
2. Tap **"Build Number"** 7 times to enable Developer Options
3. Go to **Settings → Developer Options → USB Debugging** → turn ON
4. Plug your phone into the PC with a USB cable
5. On the phone, tap **"Allow"** when asked about USB debugging

---

## Step 4 — Build and install

In Android Studio, click the **▶ Run** button (green triangle at the top).

Android Studio will:
- Build the APK (takes 2–5 minutes first time — Chaquopy downloads Python)
- Install it on your phone automatically
- Launch the app

---

## Step 5 — Set up the app (30 seconds)

1. In the app, tap **"Keys"** → paste your Gemini API key
   - Get a free key at: https://aistudio.google.com/app/apikey
2. Tap **"Dashboard"** → tap **"Enable"** → find **"Scribe Assistant"** → toggle ON
3. Done. Open any app and type `?fix` at the end of any text.

---

## All built-in triggers

| Trigger | What it does |
|---------|-------------|
| `?fix` | Fix grammar & spelling |
| `?improve` | Improve clarity |
| `?shorten` | Make shorter |
| `?expand` | Add more detail |
| `?formal` | Professional tone |
| `?casual` | Friendly tone |
| `?emoji` | Add emojis |
| `?reply` | Generate a reply |
| `?undo` | Restore previous text |
| `?translate:es` | Translate (any language code) |

---

## Project file overview

```
ScribePy/
├── app/src/main/
│   ├── python/
│   │   ├── gemini_client.py       ← Python: Gemini API
│   │   └── openai_client.py       ← Python: OpenAI-compatible API
│   └── java/com/scribe/Scribe/
│       ├── api/PythonBridge.kt    ← Kotlin↔Python bridge (Chaquopy)
│       ├── service/AssistantService.kt  ← Accessibility Service
│       ├── manager/KeyManager.kt        ← AES-256 key storage
│       ├── manager/CommandManager.kt    ← Trigger management
│       ├── ui/ (4 screens)              ← Identical to original
│       └── ScribeApp.kt             ← Starts Python on app launch
├── build.gradle.kts               ← Chaquopy plugin here
└── settings.gradle.kts            ← Chaquopy Maven repo here
```

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| Gradle sync fails with "Chaquopy not found" | Check internet connection; Chaquopy downloads on first sync |
| "Python not started" crash | Make sure ScribeApp is listed in AndroidManifest.xml `android:name=".ScribeApp"` |
| APK installs but AI doesn't work | Check your Gemini API key in the Keys tab |
| Accessibility service not listed | Reinstall the app, then check Settings → Accessibility |
| Build error about ABI filters | Open app/build.gradle.kts, remove `x86_64` from abiFilters if not needed |

---

## APK size note

The APK will be larger than the original (~25–40 MB) because it contains
the full Python 3.13 interpreter. This is expected and normal for Chaquopy apps.
