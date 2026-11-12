package com.scribe.app.keepalive

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import com.scribe.app.service.AssistantService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

/**
 * Keeps Scribe's accessibility service switched on.
 *
 * Shizuku is used once to grant WRITE_SECURE_SETTINGS (this grant survives
 * reboots) and to exempt Scribe from battery optimization. After that, Scribe
 * can turn its own accessibility service back on whenever Android disables it,
 * without needing Shizuku to be running.
 */
object KeepAlive {
    const val SHIZUKU_REQUEST_CODE = 4242
    private const val WRITE_SECURE = "android.permission.WRITE_SECURE_SETTINGS"

    private fun serviceId(context: Context): String =
        ComponentName(context, AssistantService::class.java).flattenToString()

    fun canSelfHeal(context: Context): Boolean =
        context.checkSelfPermission(WRITE_SECURE) == PackageManager.PERMISSION_GRANTED

    fun isServiceEnabled(context: Context): Boolean {
        val list = Settings.Secure.getString(
            context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val id = serviceId(context)
        return list.split(':').any { it.equals(id, ignoreCase = true) }
    }

    /** Turn the accessibility service back on if it was switched off. Returns true if it's on. */
    fun ensureServiceEnabled(context: Context): Boolean {
        if (isServiceEnabled(context)) return true
        if (!canSelfHeal(context)) return false
        return try {
            val resolver = context.contentResolver
            val current = Settings.Secure.getString(
                resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ).orEmpty()
            val id = serviceId(context)
            val updated = if (current.isBlank()) id else "$current:$id"
            Settings.Secure.putString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, updated)
            Settings.Secure.putInt(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, 1)
            true
        } catch (_: Exception) {
            false
        }
    }

    // ── Shizuku ──────────────────────────────────────────────────────────────

    fun isShizukuRunning(): Boolean = try { Shizuku.pingBinder() } catch (_: Throwable) { false }

    fun hasShizukuPermission(): Boolean = try {
        !Shizuku.isPreV11() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (_: Throwable) { false }

    fun requestShizukuPermission() {
        try { Shizuku.requestPermission(SHIZUKU_REQUEST_CODE) } catch (_: Throwable) {}
    }

    /**
     * One-time setup through Shizuku: grant the self-healing permission and
     * exempt Scribe from battery optimization. Returns a plain-English result.
     */
    suspend fun setUpWithShizuku(context: Context): Result<String> = withContext(Dispatchers.IO) {
        if (!isShizukuRunning()) return@withContext Result.failure(Exception("Shizuku isn't running. Open the Shizuku app and start it first."))
        if (!hasShizukuPermission()) return@withContext Result.failure(Exception("Scribe doesn't have Shizuku permission yet."))
        val pkg = context.packageName
        val grant = runShell("pm grant $pkg $WRITE_SECURE")
        runShell("dumpsys deviceidle whitelist +$pkg")
        runShell("cmd appops set $pkg RUN_ANY_IN_BACKGROUND allow")
        if (grant != 0 && !canSelfHeal(context)) {
            return@withContext Result.failure(Exception("Shizuku couldn't grant the permission."))
        }
        ensureServiceEnabled(context)
        KeepAliveWorker.schedule(context)
        Result.success("Scribe will now keep itself switched on.")
    }

    /** Runs a shell command as the Shizuku user (adb or root). Returns the exit code. */
    private fun runShell(command: String): Int = try {
        val method = Shizuku::class.java.getDeclaredMethod(
            "newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java
        )
        method.isAccessible = true
        val process = method.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
        process.waitFor()
    } catch (_: Throwable) {
        -1
    }
}

/** Re-enables the service after a reboot or an app update. */
class KeepAliveReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        KeepAlive.ensureServiceEnabled(context.applicationContext)
    }
}

/** Every 15 minutes, switch the service back on if Android turned it off. */
class KeepAliveWorker(
    context: Context,
    params: androidx.work.WorkerParameters
) : androidx.work.Worker(context, params) {
    override fun doWork(): Result {
        KeepAlive.ensureServiceEnabled(applicationContext)
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            val request = androidx.work.PeriodicWorkRequestBuilder<KeepAliveWorker>(
                15, java.util.concurrent.TimeUnit.MINUTES
            ).build()
            androidx.work.WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "scribe_keep_alive",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
