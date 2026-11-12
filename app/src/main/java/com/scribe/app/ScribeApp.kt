package com.scribe.app

import android.app.Application
import com.scribe.app.keepalive.KeepAlive
import com.scribe.app.keepalive.KeepAliveWorker

class ScribeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (KeepAlive.canSelfHeal(this)) {
            KeepAlive.ensureServiceEnabled(this)
            KeepAliveWorker.schedule(this)
        }
    }
}
