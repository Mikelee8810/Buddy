package com.scribe.app

import android.app.Application
import android.content.Intent
import android.os.Build
import com.scribe.app.keepalive.KeepAlive
import com.scribe.app.keepalive.KeepAliveWorker
import kotlin.system.exitProcess

class ScribeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        installCrashScreen()
        try {
            if (KeepAlive.canSelfHeal(this)) {
                KeepAlive.ensureServiceEnabled(this)
                KeepAliveWorker.schedule(this)
            }
        } catch (_: Throwable) {
            // Keep-alive is a nice-to-have; never let it stop the app opening.
        }
    }

    /** On an uncaught crash in the app's own screens, show the error instead of closing. */
    private fun installCrashScreen() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            // The accessibility service shares this process; let its crashes
            // go to the system handler so Android can restart it.
            if (thread.name != "main" || !isUiCrash(error)) {
                previous?.uncaughtException(thread, error); return@setDefaultUncaughtExceptionHandler
            }
            try {
                val details = buildString {
                    append("Scribe ").append(BuildConfig.VERSION_NAME)
                    append(" · Android ").append(Build.VERSION.RELEASE)
                    append(" · ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append("\n\n")
                    append(android.util.Log.getStackTraceString(error))
                }
                startActivity(
                    Intent(this, CrashActivity::class.java)
                        .putExtra(CrashActivity.EXTRA_DETAILS, details)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                )
            } catch (_: Throwable) {
                previous?.uncaughtException(thread, error); return@setDefaultUncaughtExceptionHandler
            }
            android.os.Process.killProcess(android.os.Process.myPid())
            exitProcess(10)
        }
    }

    private fun isUiCrash(error: Throwable): Boolean =
        error.stackTrace.none { it.className.startsWith("com.scribe.app.service") }
}
