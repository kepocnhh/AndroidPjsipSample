package android.pjsip.sample.v2

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.pjsip.sample.log
import android.view.Gravity
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.TimeUnit

class CallActivity : Activity() {
    private var isAudioOutgoingEnabled = false
    private var isVideoOutgoingEnabled = false
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            when (intent.action) {
                CallService.ACTION_CALL_STATE_BROADCAST -> {
                    when (intent.getStringExtra(CallService.KEY_STATE)) {
//                        CallService.VALUE_STATE_MEDIA_START_TRANSMIT -> {
//                            check(intent.hasExtra(CallService.VALUE_MEDIA_TYPE_VIDEO))
//                            val video = intent.getBooleanExtra(CallService.VALUE_MEDIA_TYPE_VIDEO, false)
//                            if (video) {
//                                sendBroadcast(Intent(CallService.ACTION_SET_VIDEO_SURFACE).also {
////                                    it.putExtra(CallService.VALUE_MEDIA_INCOMING, surface)
//                                    val outgoing = requireNotNull(outgoingSurfaceView)
//                                    it.putExtra(CallService.VALUE_MEDIA_OUTGOING, outgoing.holder.surface)
//                                })
//                            }
//                        }
                        CallService.VALUE_STATE_CONFIRMED -> {
                            val time = intent.getLongExtra(CallService.KEY_CALL_TIME_START, -1)
                            check(time > 0)
                            startTimer(time)
                            requireNotNull(audioOutgoingStateButton).visibility = View.VISIBLE
                            requireNotNull(videoOutgoingStateButton).visibility = View.VISIBLE
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
                                        button.text = "disable outgoing audio"
                                    } else {
                                        button.text = "enable outgoing audio"
                                    }
                                }
                                else -> TODO()
                            }
                        }
                        CallService.VALUE_MEDIA_TYPE_VIDEO -> {
                            when (intent.getStringExtra(CallService.KEY_MEDIA_SIDE)) {
                                CallService.VALUE_MEDIA_OUTGOING -> {
                                    val isEnabled = when (intent.getStringExtra(CallService.KEY_MEDIA_STATE)) {
                                        CallService.VALUE_MEDIA_ENABLED -> true
                                        CallService.VALUE_MEDIA_DISABLED -> false
                                        else -> TODO()
                                    }
                                    isVideoOutgoingEnabled = isEnabled
                                    val button = requireNotNull(videoOutgoingStateButton)
                                    if (isEnabled) {
                                        button.text = "disable outgoing video"
                                    } else {
                                        button.text = "enable outgoing video"
                                    }
                                }
                                else -> TODO()
                            }
                        }
                        CallService.VALUE_MEDIA_TYPE_VIDEO_SURFACE -> {
                            sendBroadcast(Intent(CallService.ACTION_SET_VIDEO_SURFACE).also {
                                val incoming = requireNotNull(incomingSurfaceView)
                                it.putExtra(CallService.VALUE_MEDIA_INCOMING, incoming.holder.surface)
                                val outgoing = requireNotNull(outgoingSurfaceView)
                                it.putExtra(CallService.VALUE_MEDIA_OUTGOING, outgoing.holder.surface)
                            })
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
    private var videoOutgoingStateButton: Button? = null
    private var incomingSurfaceView: SurfaceView? = null
    private var outgoingSurfaceView: SurfaceView? = null

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
                    it.text = "disable outgoing audio"
                } else {
                    it.text = "enable outgoing audio"
                }
                it.setOnClickListener {
                    sendBroadcast(Intent(CallService.ACTION_SET_MEDIA_STATE).also { intent ->
                        intent.putExtra(CallService.KEY_MEDIA_SIDE, CallService.VALUE_MEDIA_OUTGOING)
                        intent.putExtra(CallService.KEY_MEDIA_TYPE, CallService.VALUE_MEDIA_TYPE_AUDIO)
                        val state = if (isAudioOutgoingEnabled) {
                            CallService.VALUE_MEDIA_DISABLED
                        } else {
                            CallService.VALUE_MEDIA_ENABLED
                        }
                        intent.putExtra(CallService.KEY_MEDIA_STATE, state)
                    })
                }
            }
            this.audioOutgoingStateButton = audioOutgoingStateButton
            root.addView(audioOutgoingStateButton)
            val videoOutgoingStateButton = Button(this).also {
                it.visibility = View.GONE
                if (isVideoOutgoingEnabled) {
                    it.text = "disable outgoing video"
                } else {
                    it.text = "enable outgoing video"
                }
                it.setOnClickListener {
                    sendBroadcast(Intent(CallService.ACTION_SET_MEDIA_STATE).also { intent ->
                        intent.putExtra(CallService.KEY_MEDIA_SIDE, CallService.VALUE_MEDIA_OUTGOING)
                        intent.putExtra(CallService.KEY_MEDIA_TYPE, CallService.VALUE_MEDIA_TYPE_VIDEO)
                        val state = if (isVideoOutgoingEnabled) {
                            CallService.VALUE_MEDIA_DISABLED
                        } else {
                            CallService.VALUE_MEDIA_ENABLED
                        }
                        intent.putExtra(CallService.KEY_MEDIA_STATE, state)
                    })
                }
            }
            this.videoOutgoingStateButton = videoOutgoingStateButton
            root.addView(videoOutgoingStateButton)
            root.addView(Button(this).also {
                it.text = "cancel"
                it.setOnClickListener {
                    sendBroadcast(Intent(CallService.ACTION_CALL_CANCEL))
                    finish()
                    startActivity(Intent(this, MainActivity::class.java))
                }
            })
        }
        val outgoingSurfaceView = SurfaceView(this).also {
            it.layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            it.holder.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    // todo
                }

                override fun surfaceChanged(
                    holder: SurfaceHolder,
                    format: Int,
                    width: Int,
                    height: Int
                ) {
                    // todo
                }

                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    // todo
                }
            })
        }
        this.outgoingSurfaceView = outgoingSurfaceView
        val incomingSurfaceView = SurfaceView(this).also {
            it.layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            it.holder.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    // todo
                }

                override fun surfaceChanged(
                    holder: SurfaceHolder,
                    format: Int,
                    width: Int,
                    height: Int
                ) {
                    // todo
                }

                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    // todo
                }
            })
        }
        this.incomingSurfaceView = incomingSurfaceView
        setContentView(FrameLayout(this).also { root ->
            root.background = ColorDrawable(Color.BLACK)
            root.addView(incomingSurfaceView)
            root.addView(outgoingSurfaceView)
            root.post {
                outgoingSurfaceView.layoutParams = FrameLayout.LayoutParams(
                    root.width / 3,
                    root.height / 3,
                    Gravity.BOTTOM
                )
            }
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
        log("set video surface...")
        sendBroadcast(Intent(CallService.ACTION_SET_VIDEO_SURFACE).also {
            it.putExtra(CallService.VALUE_MEDIA_INCOMING, null as Surface?)
            it.putExtra(CallService.VALUE_MEDIA_OUTGOING, null as Surface?)
        })
        incomingSurfaceView = null
        outgoingSurfaceView = null
        // todo
    }
}
