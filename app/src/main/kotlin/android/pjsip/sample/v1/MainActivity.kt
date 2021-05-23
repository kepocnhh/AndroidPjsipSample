package android.pjsip.sample.v1

import android.os.Bundle
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.pjsip.sample.log
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar

class MainActivity : Activity() {
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            log("on receive ${intent.action}")
            when (intent.action) {
                CallService.ACTION_STATUS_RESPONSE -> {
                    val status = intent.getStringExtra(CallService.KEY_ACCOUNT_STATUS)
                    when (status) {
                        CallService.VALUE_NO_ACCOUNT -> {
                            finish()
                            startActivity(Intent(this@MainActivity, RegistrationActivity::class.java))
                        }
                        CallService.VALUE_ACCOUNT_EXISTS -> {
                            finish()
                            startActivity(Intent(this@MainActivity, DialActivity::class.java))
                        }
                        else -> TODO()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        log("on create")
        setContentView(FrameLayout(this).also { root ->
            root.addView(ProgressBar(this).also {
                it.layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            })
        })
        registerReceiver(receiver, IntentFilter().also {
            setOf(
                CallService.ACTION_STATUS_RESPONSE
            ).forEach(it::addAction)
        })
        startService(Intent(this, CallService::class.java).also {
            it.action = CallService.ACTION_STATUS_REQUEST
        })
//        sendBroadcast(Intent(CallService.ACTION_STATUS_REQUEST))
        log("send broadcast ${CallService.ACTION_STATUS_REQUEST}")
        // todo
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
        // todo
    }
}
