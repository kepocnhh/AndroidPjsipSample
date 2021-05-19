package android.pjsip.sample

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.IBinder

class CallService : Service() {
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            // when (intent.action) {... todo
        }
    }

    override fun onCreate() {
        super.onCreate()
        // todo
    }

    override fun onDestroy() {
        super.onDestroy()
        // todo
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
