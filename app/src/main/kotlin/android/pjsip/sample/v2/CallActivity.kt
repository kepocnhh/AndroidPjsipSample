package android.pjsip.sample.v2

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.TimeUnit

class CallActivity : Activity() {
    private var isAudioOutgoingEnabled = false
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            when (intent.action) {
                CallService.ACTION_CALL_STATE_BROADCAST -> {
                    val state = intent.getStringExtra(CallService.KEY_STATE)
                    when (state) {
                        CallService.VALUE_STATE_CONFIRMED -> {
                            val time = intent.getLongExtra(CallService.KEY_CALL_TIME_START, -1)
                            check(time > 0)
                            startTimer(time)
                            requireNotNull(audioOutgoingStateButton).visibility = View.VISIBLE
                        }
                        CallService.VALUE_STATE_DISCONNECTED -> {
                            finish()
                            startActivity(Intent(this@CallActivity, MainActivity::class.java))
                        }
//                        else -> TODO()
                    }
                }
                CallService.ACTION_MEDIA_STATE_BROADCAST -> {
                    when (intent.getStringExtra(CallService.KEY_MEDIA_TYPE)) {
                        CallService.VALUE_MEDIA_TYPE_AUDIO -> {
                            when (intent.getStringExtra(CallService.KEY_MEDIA_SIDE)) {
                                CallService.VALUE_MEDIA_OUTGOING -> {
                                    val isEnabled = when (intent.getStringExtra(CallService.KEY_MEDIA_STATE)) {
                                        CallService.VALUE_MEDIA_ENABLED -> true
                                        CallService.VALUE_MEDIA_DISABLED -> false
                                        else -> TODO()
                                    }
                                    isAudioOutgoingEnabled = isEnabled
                                    val button = requireNotNull(audioOutgoingStateButton)
                                    if (isEnabled) {
                                        button.text = "disable audio"
                                    } else {
                                        button.text = "enable audio"
                                    }
                                }
                                else -> TODO()
                            }
                        }
                        CallService.VALUE_MEDIA_TYPE_VIDEO -> {
                            TODO()
                        }
                        else -> TODO()
                    }
                }
            }
        }
    }

    private var timeTextView: TextView? = null
    private var timer: Timer? = null
    private var audioOutgoingStateButton: Button? = null

    private fun startTimer(time: Long) {
        check(timer == null)
        val timer = Timer()
        val timerTask = object : TimerTask() {
            override fun run() {
                val timeNow = System.currentTimeMillis()
                val d = timeNow - time
                val h = TimeUnit.MILLISECONDS.toHours(d)
                val minutes = TimeUnit.MILLISECONDS.toMinutes(d)
                val m = minutes - h * 60
                val s = TimeUnit.MILLISECONDS.toSeconds(d) - minutes * 60
                val timeTextView = requireNotNull(timeTextView)
                timeTextView.text = String.format("%02d:%02d:%02d", h, m, s)
            }
        }
        val delay = 0L
        val period = 250L
        timer.schedule(timerTask, delay, period)
        this.timer = timer
    }
    private fun stopTimer() {
        timer?.cancel()
        timer = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val foreground = LinearLayout(this).also { root ->
            root.orientation = LinearLayout.VERTICAL
            val timeTextView = TextView(this).also {
                it.setTextColor(Color.WHITE)
            }
            this.timeTextView = timeTextView
            root.addView(timeTextView)
            val audioOutgoingStateButton = Button(this).also {
                it.visibility = View.GONE
                if (isAudioOutgoingEnabled) {
                    it.text = "disable audio"
                } else {
                    it.text = "enable audio"
                }
                it.setOnClickListener {
                    sendBroadcast(Intent(CallService.ACTION_SET_MEDIA_STATE).also { intent ->
                        intent.putExtra(CallService.KEY_MEDIA_SIDE, CallService.VALUE_MEDIA_OUTGOING)
                        intent.putExtra(CallService.KEY_MEDIA_TYPE, CallService.VALUE_MEDIA_TYPE_AUDIO)
                        val value = if (isAudioOutgoingEnabled) {
                            CallService.VALUE_MEDIA_DISABLED
                        } else {
                            CallService.VALUE_MEDIA_ENABLED
                        }
                        intent.putExtra(CallService.KEY_MEDIA_STATE, value)
                    })
                }
            }
            this.audioOutgoingStateButton = audioOutgoingStateButton
            root.addView(audioOutgoingStateButton)
            /*
            val videoButton = Button(this).also {
                it.visibility = View.GONE
                if (videoOutgoingEnabled) {
                    it.text = "disable video"
                } else {
                    it.text = "enable video"
                }
                it.setOnClickListener {
                    sendBroadcast(Intent(CallService.ACTION_SET_VIDEO_STATE).also { intent ->
                        val value = if (videoOutgoingEnabled) {
                            CallService.VALUE_MEDIA_DISABLED
                        } else {
                            CallService.VALUE_MEDIA_ENABLED
                        }
                        intent.putExtra(CallService.KEY_MEDIA_STATE, value)
                    })
                }
            }
            this.videoButton = videoButton
            root.addView(videoButton)
            */
            root.addView(Button(this).also {
                it.text = "cancel"
                it.setOnClickListener {
                    sendBroadcast(Intent(CallService.ACTION_CALL_CANCEL))
                    finish()
                    startActivity(Intent(this, MainActivity::class.java))
                }
            })
        }
        setContentView(FrameLayout(this).also { root ->
            root.background = ColorDrawable(Color.BLACK)
//            root.addView(incomingSurfaceView)
            root.addView(foreground)
        })
        registerReceiver(receiver, IntentFilter().also {
            setOf(
                CallService.ACTION_CALL_STATE_BROADCAST,
                CallService.ACTION_MEDIA_STATE_BROADCAST
            ).forEach(it::addAction)
        })
        sendBroadcast(Intent(CallService.ACTION_CALL_STATE_REQUEST))
        sendBroadcast(Intent(CallService.ACTION_MEDIA_STATE_REQUEST))
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTimer()
        unregisterReceiver(receiver)
        // todo
    }
}
