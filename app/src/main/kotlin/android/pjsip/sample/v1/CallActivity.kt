package android.pjsip.sample.v1

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.pjsip.sample.log
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
import org.pjsip.pjsua2.VideoWindowHandle
import org.pjsip.pjsua2.pjsip_inv_state

class CallActivity : Activity() {
    private fun onAudio(enabled: Boolean) {
        if (mediaAudioEnabled == enabled) return
        mediaAudioEnabled = enabled
        val button = requireNotNull(audioButton)
        if (enabled) {
            button.text = "disable audio"
        } else {
            button.text = "enable audio"
        }
    }
    private fun onVideoOutgoing(enabled: Boolean) {
        if (videoOutgoingEnabled == enabled) return
        videoOutgoingEnabled = enabled
        val button = requireNotNull(videoButton)
        if (enabled) {
            button.text = "disable outgoing video"
        } else {
            button.text = "enable outgoing video"
        }
    }
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            log("on receive ${intent.action}")
            when (intent.action) {
                CallService.ACTION_MEDIA_STATE_BROADCAST -> {
                    val type = intent.getStringExtra(CallService.KEY_MEDIA_TYPE)
                    when (type) {
                        CallService.VALUE_MEDIA_TYPE_AUDIO -> {
                            val state = intent.getStringExtra(CallService.KEY_MEDIA_STATE)
                            val enabled = when (state) {
                                CallService.VALUE_MEDIA_ENABLED -> true
                                CallService.VALUE_MEDIA_DISABLED -> false
                                else -> TODO()
                            }
                            onAudio(enabled = enabled)
                        }
                        CallService.VALUE_MEDIA_TYPE_VIDEO -> {
                            val action = intent.getStringExtra(CallService.KEY_MEDIA_ACTION)
                            log("on receive ${intent.action}/$action")
                            when (action) {
                                CallService.VALUE_MEDIA_START_RECEIVE -> {
                                    val incomingSurfaceView = requireNotNull(incomingSurfaceView)
                                    sendBroadcast(Intent(CallService.ACTION_SET_VIDEO_INCOMING_STATE).also {
                                        it.putExtra(CallService.KEY_VIDEO_STATE, CallService.VALUE_MEDIA_ENABLED)
                                        it.putExtra(CallService.KEY_VIDEO_SURFACE, incomingSurfaceView.holder.surface)
                                    })
                                }
                                CallService.VALUE_MEDIA_SET_STATE -> {
                                    val state = intent.getStringExtra(CallService.KEY_MEDIA_STATE)
                                    val enabled = when (state) {
                                        CallService.VALUE_MEDIA_ENABLED -> true
                                        CallService.VALUE_MEDIA_DISABLED -> false
                                        else -> TODO()
                                    }
                                    onVideoOutgoing(enabled = enabled)
                                }
                                else -> TODO()
                            }
                        }
                        else -> TODO()
                    }
                }
                CallService.ACTION_CALL_STATE_BROADCAST -> {
                    val state = intent.getIntExtra(CallService.KEY_CALL_STATE, -1)
                    when (state) {
                        pjsip_inv_state.PJSIP_INV_STATE_CONFIRMED -> {
                            val time = intent.getLongExtra(CallService.KEY_CALL_TIME, -1)
                            check(time > 0)
                            startTimer(time)
                            requireNotNull(audioButton).visibility = View.VISIBLE
                            requireNotNull(videoButton).visibility = View.VISIBLE
                        }
                        pjsip_inv_state.PJSIP_INV_STATE_DISCONNECTED -> {
                            finish()
                            startActivity(Intent(this@CallActivity, MainActivity::class.java))
                        }
//                        else -> TODO()
                    }
                }
            }
        }
    }

    private var videoButton: Button? = null
    private var videoOutgoingEnabled: Boolean = false
    private var incomingSurfaceView: SurfaceView? = null
    private var audioButton: Button? = null
    private var mediaAudioEnabled: Boolean = false
    private var timeTextView: TextView? = null
    private var timer: Timer? = null

    private fun startTimer(time: Long) {
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
        val timer = requireNotNull(timer)
        timer.cancel()
        this.timer = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val foreground = LinearLayout(this).also { root ->
            root.background = ColorDrawable(Color.TRANSPARENT)
            root.orientation = LinearLayout.VERTICAL
            val timeTextView = TextView(this).also {
                it.setTextColor(Color.WHITE)
            }
            this.timeTextView = timeTextView
            root.addView(timeTextView)
            val audioButton = Button(this).also {
                it.visibility = View.GONE
                if (mediaAudioEnabled) {
                    it.text = "disable audio"
                } else {
                    it.text = "enable audio"
                }
                it.setOnClickListener {
                    sendBroadcast(Intent(CallService.ACTION_SET_AUDIO_STATE).also { intent ->
                        val value = if (mediaAudioEnabled) {
                            CallService.VALUE_MEDIA_DISABLED
                        } else {
                            CallService.VALUE_MEDIA_ENABLED
                        }
                        intent.putExtra(CallService.KEY_MEDIA_STATE, value)
                    })
                }
            }
            this.audioButton = audioButton
            root.addView(audioButton)
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
            root.addView(Button(this).also {
                it.text = "cancel"
                it.setOnClickListener {
                    sendBroadcast(Intent(CallService.ACTION_CALL_CANCEL))
                    finish()
                    startActivity(Intent(this, MainActivity::class.java))
                }
            })
        }
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
                    sendBroadcast(Intent(CallService.ACTION_SET_VIDEO_INCOMING_STATE).also { intent ->
                        intent.putExtra(CallService.KEY_VIDEO_STATE, CallService.VALUE_MEDIA_ENABLED)
                        intent.putExtra(CallService.KEY_VIDEO_SURFACE, holder.surface)
                    })
//                    val videoWindowHandle = VideoWindowHandle()
//                    videoWindowHandle.handle.setWindow(holder.surface)
//                    MainActivity.currentCall.vidWin.setWindow(vidWH)
//                    TODO()
                }

                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    sendBroadcast(Intent(CallService.ACTION_SET_VIDEO_INCOMING_STATE).also { intent ->
                        intent.putExtra(CallService.KEY_VIDEO_STATE, CallService.VALUE_MEDIA_DISABLED)
                    })
//                    TODO()
                }
            })
        }
        this.incomingSurfaceView = incomingSurfaceView
        setContentView(FrameLayout(this).also { root ->
            root.background = ColorDrawable(Color.BLACK)
            root.addView(incomingSurfaceView)
            root.addView(foreground)
        })
        registerReceiver(receiver, IntentFilter().also {
            setOf(
                CallService.ACTION_MEDIA_STATE_BROADCAST,
                CallService.ACTION_CALL_STATE_BROADCAST
            ).forEach(it::addAction)
        })
        sendBroadcast(Intent(CallService.ACTION_CALL_STATE_REQUEST))
        sendBroadcast(Intent(CallService.ACTION_MEDIA_STATE_REQUEST))
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTimer()
        sendBroadcast(Intent(CallService.ACTION_SET_VIDEO_STATE).also { intent ->
            intent.putExtra(CallService.KEY_MEDIA_SIDE, CallService.VALUE_MEDIA_INCOMING)
            intent.putExtra(CallService.KEY_MEDIA_STATE, CallService.VALUE_MEDIA_DISABLED)
        })
        sendBroadcast(Intent(CallService.ACTION_SET_VIDEO_STATE).also { intent ->
            intent.putExtra(CallService.KEY_MEDIA_SIDE, CallService.VALUE_MEDIA_OUTGOING)
            intent.putExtra(CallService.KEY_MEDIA_STATE, CallService.VALUE_MEDIA_DISABLED)
        })
        unregisterReceiver(receiver)
        // todo
    }
}
