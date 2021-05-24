package android.pjsip.sample.v2

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.pjsip.sample.log
import android.util.Log
import android.view.Surface
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import org.pjsip.pjsua2.Account
import org.pjsip.pjsua2.AccountConfig
import org.pjsip.pjsua2.AuthCredInfo
import org.pjsip.pjsua2.Call
import org.pjsip.pjsua2.CallMediaInfo
import org.pjsip.pjsua2.CallOpParam
import org.pjsip.pjsua2.CallVidSetStreamParam
import org.pjsip.pjsua2.Endpoint
import org.pjsip.pjsua2.EpConfig
import org.pjsip.pjsua2.LogEntry
import org.pjsip.pjsua2.LogWriter
import org.pjsip.pjsua2.OnCallMediaStateParam
import org.pjsip.pjsua2.OnCallStateParam
import org.pjsip.pjsua2.OnIncomingCallParam
import org.pjsip.pjsua2.OnRegStateParam
import org.pjsip.pjsua2.TransportConfig
import org.pjsip.pjsua2.VideoPreview
import org.pjsip.pjsua2.VideoPreviewOpParam
import org.pjsip.pjsua2.VideoWindow
import org.pjsip.pjsua2.VideoWindowHandle
import org.pjsip.pjsua2.pj_qos_type
import org.pjsip.pjsua2.pjmedia_type
import org.pjsip.pjsua2.pjsip_inv_state
import org.pjsip.pjsua2.pjsip_status_code
import org.pjsip.pjsua2.pjsip_transport_type_e
import org.pjsip.pjsua2.pjsua_call_flag
import org.pjsip.pjsua2.pjsua_call_media_status
import org.pjsip.pjsua2.pjsua_call_vid_strm_op

private class AndroidLogWriter : LogWriter() {
    override fun write(entry: LogEntry?) {
        Log.d("org.pjsip.pjsua2", entry?.msg ?: "no message")
    }
}

class CallService : Service() {
    companion object {
        const val ACTION_MAKE_CALL = "ACTION_MAKE_CALL"
        const val ACTION_REGISTRATION = "ACTION_REGISTRATION"
        const val KEY_HOST = "KEY_HOST"
        const val KEY_REALM = "KEY_REALM"
        const val KEY_PORT = "KEY_PORT"
        const val KEY_AUDIO_OUTGOING_ENABLED = "KEY_AUDIO_OUTGOING_ENABLED"
        const val KEY_VIDEO_OUTGOING_ENABLED = "KEY_VIDEO_OUTGOING_ENABLED"
        const val KEY_USER_TO_NAME = "KEY_USER_TO_NAME"
        const val KEY_USER_FROM_NAME = "KEY_USER_FROM_NAME"
        const val KEY_USER_FROM_PASSWORD = "KEY_USER_FROM_PASSWORD"
        const val ACTION_CALL_STATE_BROADCAST = "ACTION_CALL_STATE_BROADCAST"
        const val KEY_STATE = "KEY_STATE"
        const val VALUE_STATE_NONE = "VALUE_STATE_NONE"
        const val VALUE_STATE_ERROR = "VALUE_STATE_ERROR"
        const val VALUE_STATE_REGISTRATION = "VALUE_STATE_REGISTRATION"
        const val VALUE_STATE_INCOMING_CALL = "VALUE_STATE_INCOMING_CALL"
        const val VALUE_STATE_EARLY = "VALUE_STATE_EARLY"
        const val VALUE_STATE_DISCONNECTED = "VALUE_STATE_DISCONNECTED"
//        const val VALUE_STATE_MEDIA_START_TRANSMIT = "VALUE_STATE_MEDIA_START_TRANSMIT"
        const val VALUE_STATE_CONFIRMED = "VALUE_STATE_CONFIRMED"
        const val VALUE_STATE_CONNECTING = "VALUE_STATE_CONNECTING"
        const val ACTION_CALL_STATE_REQUEST = "ACTION_CALL_STATE_REQUEST"
        const val KEY_CALL_TIME_START = "KEY_CALL_TIME_START"
        const val ACTION_CALL_CANCEL = "ACTION_CALL_CANCEL"
        const val ACTION_CALL_ACCEPT = "ACTION_CALL_ACCEPT"
        const val ACTION_MEDIA_STATE_REQUEST = "ACTION_MEDIA_STATE_REQUEST"
        const val ACTION_MEDIA_STATE_BROADCAST = "ACTION_MEDIA_STATE_BROADCAST"
        const val KEY_CODE = "KEY_CODE"
        const val KEY_MEDIA_TYPE = "KEY_MEDIA_TYPE"
        const val VALUE_MEDIA_TYPE_AUDIO = "VALUE_MEDIA_TYPE_AUDIO"
        const val VALUE_MEDIA_TYPE_VIDEO = "VALUE_MEDIA_TYPE_VIDEO"
        const val VALUE_MEDIA_TYPE_VIDEO_SURFACE = "VALUE_MEDIA_TYPE_VIDEO_SURFACE"
        const val KEY_MEDIA_SIDE = "KEY_MEDIA_SIDE"
        const val KEY_SIDE = "KEY_SIDE"
        const val ACTION_SET_VIDEO_SURFACE = "ACTION_SET_VIDEO_SURFACE"
        const val VALUE_MEDIA_INCOMING = "VALUE_MEDIA_INCOMING"
        const val VALUE_INCOMING = "VALUE_INCOMING"
        const val VALUE_MEDIA_OUTGOING = "VALUE_MEDIA_OUTGOING"
        const val VALUE_OUTGOING = "VALUE_OUTGOING"
        const val KEY_MEDIA_STATE = "KEY_MEDIA_STATE"
        const val VALUE_MEDIA_ENABLED = "VALUE_MEDIA_ENABLED"
        const val VALUE_MEDIA_DISABLED = "VALUE_MEDIA_DISABLED"
        const val ACTION_SET_MEDIA_STATE = "ACTION_SET_MEDIA_STATE"
        private const val ACTION_REGISTRATION_RESULT = "ACTION_REGISTRATION_RESULT"
        private const val H264_CODEC_ID = "H264/97"
        private var logWriter: LogWriter? = null

        private fun endpoint(): Endpoint {
            val result = Endpoint()
            result.libCreate()
            val epConfig = EpConfig()
            epConfig.uaConfig.userAgent = "android.pjsip.sample"
            epConfig.uaConfig.mainThreadOnly = false
//            epConfig.uaConfig.mainThreadOnly = true
//            epConfig.uaConfig.threadCnt = 0
            epConfig.uaConfig.threadCnt = 1
//            epConfig.uaConfig.threadCnt = 5
            epConfig.logConfig = epConfig.logConfig.also {
                it.level = 4
                it.consoleLevel = 5
                logWriter?.delete()
                logWriter = AndroidLogWriter()
                it.writer = logWriter
            }
            result.libInit(epConfig)
            epConfig.delete()
            val transportConfig = TransportConfig()
            result.transportCreate(pjsip_transport_type_e.PJSIP_TRANSPORT_UDP, transportConfig)
            transportConfig.delete()
            result.libStart()
//            result.libRegisterThread(Thread.currentThread().name)
//            val vidCodecParam = result.getVideoCodecParam(H264_CODEC_ID)
//            vidCodecParam.encFmt = vidCodecParam.encFmt.also {
//                it.width = 640
//                it.height = 480
//            }
//            result.setVideoCodecParam(H264_CODEC_ID, vidCodecParam)
            return result
        }
        private fun accountConfig(
            host: String,
            realm: String,
            port: Int,
            userFromName: String,
            userFromPassword: String,
            registerOnAdd: Boolean,
            isVideoOutgoingEnabled: Boolean
        ): AccountConfig {
            val uriId = "sip:$userFromName@$realm"
            val uriRegistrar = "sip:$host:$port"
            val authScheme = "digest"
            val result = AccountConfig()
            result.idUri = uriId
            result.regConfig.registrarUri = uriRegistrar
            result.regConfig.timeoutSec = 60 // todo
//            result.regConfig.timeoutSec = 120 // todo
            result.regConfig.registerOnAdd = registerOnAdd
            result.sipConfig.authCreds.add(
                AuthCredInfo(authScheme, realm, userFromName, 0, userFromPassword)
            )
            result.sipConfig.proxies.add(uriRegistrar)
//            result.sipConfig.contactUriParams = ""
//            result.mediaConfig.transportConfig.qosType = pj_qos_type.PJ_QOS_TYPE_VOICE
//            result.videoConfig.autoTransmitOutgoing = false
            result.videoConfig.autoTransmitOutgoing = true
//            result.videoConfig.autoTransmitOutgoing = isVideoOutgoingEnabled
            result.videoConfig.autoShowIncoming = true
            result.videoConfig.defaultCaptureDevice = 1 // todo front
            result.videoConfig.defaultRenderDevice = 0 // todo
            return result
        }
    }

    private var executor: ExecutorService? = null
    private var endpoint: Endpoint? = null
    private var account: Account? = null
    private var callTimeStart: Long? = null
    private var call: Call? = null
    private var isAudioOutgoingEnabled: Boolean = false
    private var isVideoOutgoingEnabled: Boolean = false
//    private var videoCallMediaInfo: CallMediaInfo? = null
    private var incomingVideoWindow: VideoWindow? = null
    private var outgoingVideoPreview: VideoPreview? = null

    private fun onAccountFinish() {
        val account = requireNotNull(account)
        account.shutdown()
        account.delete()
        this.account = null
        log("delete account")
//        logWriter?.delete()
//        logWriter = null
//        log("delete log writer")
        val endpoint = requireNotNull(endpoint)
//        endpoint.libRegisterThread(Thread.currentThread().name)
        endpoint.libDestroy()
        endpoint.delete()
        this.endpoint = null
        log("delete endpoint")
    }
    private fun onCallFinish() {
        incomingVideoWindow?.delete()
        incomingVideoWindow = null
        log("delete incoming video window")
        outgoingVideoPreview?.delete()
        outgoingVideoPreview = null
//        log("delete outgoing video preview")
//        videoCallMediaInfo?.delete()
//        videoCallMediaInfo = null
        log("delete video call media info")
        requireNotNull(call).delete()
        call = null
        log("delete call")
        onAccountFinish()
    }
    private fun onMakeCall(
        host: String,
        realm: String,
        port: Int,
        userFromName: String,
        userFromPassword: String,
        userToName: String,
        isAudioOutgoingEnabled: Boolean,
        isVideoOutgoingEnabled: Boolean
    ) {
        check(endpoint == null)
        val endpoint = endpoint()
        this.endpoint = endpoint
        val accountConfig = accountConfig(
            host = host,
            realm = realm,
            port = port,
            userFromName = userFromName,
            userFromPassword = userFromPassword,
            registerOnAdd = false,
            isVideoOutgoingEnabled = isVideoOutgoingEnabled
        )
        check(account == null)
        val account = object : Account() {
            override fun onRegState(prm: OnRegStateParam?) {
                if (prm == null) TODO()
                when (prm.code) {
                    200 -> {
                        // todo
                    }
                    else -> {
                        TODO()
                    }
                }
            }
        }
        this.account = account
        account.create(accountConfig)
        account.info.regExpiresSec = 10 // todo
        check(call == null)
        this.isAudioOutgoingEnabled = isAudioOutgoingEnabled
        this.isVideoOutgoingEnabled = isVideoOutgoingEnabled
        val call = object : Call(account) {
            override fun onCallState(prm: OnCallStateParam?) {
                if (prm == null) TODO()
                log("on call state: ${info.state}")
                when (info.state) {
                    pjsip_inv_state.PJSIP_INV_STATE_EARLY -> {
                        sendBroadcast(Intent(ACTION_CALL_STATE_BROADCAST).also {
                            it.putExtra(KEY_SIDE, VALUE_OUTGOING)
                            it.putExtra(KEY_STATE, VALUE_STATE_EARLY)
                        })
                    }
                    pjsip_inv_state.PJSIP_INV_STATE_CONNECTING -> {
                        sendBroadcast(Intent(ACTION_CALL_STATE_BROADCAST).also {
                            it.putExtra(KEY_STATE, VALUE_STATE_CONNECTING)
                        })
                    }
                    pjsip_inv_state.PJSIP_INV_STATE_DISCONNECTED -> {
                        sendBroadcast(Intent(ACTION_CALL_STATE_BROADCAST).also {
                            it.putExtra(KEY_STATE, VALUE_STATE_DISCONNECTED)
                        })
                    }
                    pjsip_inv_state.PJSIP_INV_STATE_CONFIRMED -> {
                        val time = System.currentTimeMillis()
                        callTimeStart = time
                        sendBroadcast(Intent(ACTION_CALL_STATE_BROADCAST).also {
                            it.putExtra(KEY_STATE, VALUE_STATE_CONFIRMED)
                            it.putExtra(KEY_CALL_TIME_START, time)
                        })
                    }
                }
            }

            override fun onCallMediaState(prm: OnCallMediaStateParam?) {
                if (prm == null) TODO()
                val media = info.media.indices.map {
                    info.media[it]
                }.filter {
                    it.status == pjsua_call_media_status.PJSUA_CALL_MEDIA_ACTIVE
                }
//                        check(media.size == 2) // todo
                val audio = media.firstOrNull {
                    it.type == pjmedia_type.PJMEDIA_TYPE_AUDIO
                }
                if (audio != null) {
                    val audioMedia = getAudioMedia(audio.index.toInt())!!
                    val audDevManager = endpoint.audDevManager()
                    if (isAudioOutgoingEnabled) {
                        audDevManager.captureDevMedia.startTransmit(audioMedia)
                    }
                    sendBroadcast(Intent(ACTION_MEDIA_STATE_BROADCAST).also {
                        it.putExtra(KEY_MEDIA_SIDE, VALUE_MEDIA_OUTGOING)
                        it.putExtra(KEY_MEDIA_TYPE, VALUE_MEDIA_TYPE_AUDIO)
                        val state = if (isAudioOutgoingEnabled) VALUE_MEDIA_ENABLED else VALUE_MEDIA_DISABLED
                        it.putExtra(KEY_MEDIA_STATE, state)
                    })
                    audioMedia.startTransmit(audDevManager.playbackDevMedia)
                }
                val video = media.firstOrNull {
                    it.type == pjmedia_type.PJMEDIA_TYPE_VIDEO
                }
                if (video != null) {
//                    videoCallMediaInfo = video
                    val callVidSetStreamParam = CallVidSetStreamParam()
                    requireNotNull(call).vidSetStream(pjsua_call_vid_strm_op.PJSUA_CALL_VID_STRM_START_TRANSMIT, callVidSetStreamParam)
                    callVidSetStreamParam.delete()
                    incomingVideoWindow = VideoWindow(video.videoIncomingWindowId)
                    outgoingVideoPreview = VideoPreview(video.videoCapDev)
                    log("outgoing video set $isVideoOutgoingEnabled")
                    sendBroadcast(Intent(ACTION_SET_MEDIA_STATE).also {
                        it.putExtra(KEY_MEDIA_SIDE, VALUE_MEDIA_OUTGOING)
                        it.putExtra(KEY_MEDIA_TYPE, VALUE_MEDIA_TYPE_VIDEO)
                        val state = if (isVideoOutgoingEnabled) VALUE_MEDIA_ENABLED else VALUE_MEDIA_DISABLED
                        it.putExtra(KEY_MEDIA_STATE, state)
                    })
                    sendBroadcast(Intent(ACTION_MEDIA_STATE_BROADCAST).also {
                        it.putExtra(KEY_MEDIA_TYPE, VALUE_MEDIA_TYPE_VIDEO_SURFACE)
                    })
                }
//                sendBroadcast(Intent(ACTION_CALL_STATE_BROADCAST).also {
//                    it.putExtra(KEY_STATE, VALUE_STATE_MEDIA_START_TRANSMIT)
//                    it.putExtra(VALUE_MEDIA_TYPE_AUDIO, audio != null)
//                    it.putExtra(VALUE_MEDIA_TYPE_VIDEO, video != null)
//                })
            }
        }
        val uriDestination = "sip:$userToName@$host"
        val callOpParam = CallOpParam()
        callOpParam.opt.audioCount = 1
        callOpParam.opt.videoCount = 1
        this.call = call
        log("call")
        call.makeCall(uriDestination, callOpParam)
        callOpParam.delete()
    }

    private fun onCallStateRequest() {
        val endpoint = endpoint
        if (endpoint == null) {
            sendBroadcast(Intent(ACTION_CALL_STATE_BROADCAST).also {
                it.putExtra(KEY_STATE, VALUE_STATE_NONE)
            })
            return
        }
//        val account = requireNotNull(account)
//        if (account.info.regIsActive) TODO()
        val call = requireNotNull(call)
        val state = call.info.state
        sendBroadcast(Intent(ACTION_CALL_STATE_BROADCAST).also {
            if (state == pjsip_inv_state.PJSIP_INV_STATE_CONFIRMED) {
                it.putExtra(KEY_STATE, VALUE_STATE_CONFIRMED)
                it.putExtra(KEY_CALL_TIME_START, callTimeStart)
            } else it.putExtra(KEY_STATE, when (state) {
                pjsip_inv_state.PJSIP_INV_STATE_EARLY -> VALUE_STATE_EARLY
                pjsip_inv_state.PJSIP_INV_STATE_CONNECTING -> VALUE_STATE_CONNECTING
                else -> TODO()
            })
        })
    }

    private fun onIncomingCall(callId: Int) {
        val endpoint = requireNotNull(endpoint)
        val account = requireNotNull(account)
        check(call == null)
        this.isAudioOutgoingEnabled = true // todo
        this.isVideoOutgoingEnabled = true // todo
        val call = object : Call(account, callId) {
            override fun onCallState(prm: OnCallStateParam?) {
                if (prm == null) TODO()
                log("on call state: ${info.state}")
                when (info.state) {
                    pjsip_inv_state.PJSIP_INV_STATE_EARLY -> {
                        sendBroadcast(Intent(ACTION_CALL_STATE_BROADCAST).also {
                            it.putExtra(KEY_SIDE, VALUE_INCOMING)
                            it.putExtra(KEY_STATE, VALUE_STATE_EARLY)
                        })
                    }
                    pjsip_inv_state.PJSIP_INV_STATE_CONNECTING -> {
                        sendBroadcast(Intent(ACTION_CALL_STATE_BROADCAST).also {
                            it.putExtra(KEY_STATE, VALUE_STATE_CONNECTING)
                        })
                    }
                    pjsip_inv_state.PJSIP_INV_STATE_DISCONNECTED -> {
                        sendBroadcast(Intent(ACTION_CALL_STATE_BROADCAST).also {
                            it.putExtra(KEY_STATE, VALUE_STATE_DISCONNECTED)
                        })
                    }
                    pjsip_inv_state.PJSIP_INV_STATE_CONFIRMED -> {
                        val time = System.currentTimeMillis()
                        callTimeStart = time
                        sendBroadcast(Intent(ACTION_CALL_STATE_BROADCAST).also {
                            it.putExtra(KEY_STATE, VALUE_STATE_CONFIRMED)
                            it.putExtra(KEY_CALL_TIME_START, time)
                        })
                    }
                }
            }

            override fun onCallMediaState(prm: OnCallMediaStateParam?) {
                if (prm == null) TODO()
                val media = info.media.indices.map {
                    info.media[it]
                }.filter {
                    it.status == pjsua_call_media_status.PJSUA_CALL_MEDIA_ACTIVE
                }
//                        check(media.size == 2) // todo
                val audio = media.firstOrNull {
                    it.type == pjmedia_type.PJMEDIA_TYPE_AUDIO
                }
                if (audio != null) {
                    val audioMedia = getAudioMedia(audio.index.toInt())!!
                    val audDevManager = endpoint.audDevManager()
                    if (isAudioOutgoingEnabled) {
                        audDevManager.captureDevMedia.startTransmit(audioMedia)
                    }
                    sendBroadcast(Intent(ACTION_MEDIA_STATE_BROADCAST).also {
                        it.putExtra(KEY_MEDIA_SIDE, VALUE_MEDIA_OUTGOING)
                        it.putExtra(KEY_MEDIA_TYPE, VALUE_MEDIA_TYPE_AUDIO)
                        val state = if (isAudioOutgoingEnabled) VALUE_MEDIA_ENABLED else VALUE_MEDIA_DISABLED
                        it.putExtra(KEY_MEDIA_STATE, state)
                    })
                    audioMedia.startTransmit(audDevManager.playbackDevMedia)
                }
                val video = media.firstOrNull {
                    it.type == pjmedia_type.PJMEDIA_TYPE_VIDEO
                }
                if (video != null) {
                    val callVidSetStreamParam = CallVidSetStreamParam()
                    requireNotNull(call).vidSetStream(pjsua_call_vid_strm_op.PJSUA_CALL_VID_STRM_START_TRANSMIT, callVidSetStreamParam)
                    callVidSetStreamParam.delete()
                    incomingVideoWindow = VideoWindow(video.videoIncomingWindowId)
                    outgoingVideoPreview = VideoPreview(video.videoCapDev)
                    log("outgoing video set $isVideoOutgoingEnabled")
                    sendBroadcast(Intent(ACTION_SET_MEDIA_STATE).also {
                        it.putExtra(KEY_MEDIA_SIDE, VALUE_MEDIA_OUTGOING)
                        it.putExtra(KEY_MEDIA_TYPE, VALUE_MEDIA_TYPE_VIDEO)
                        val state = if (isVideoOutgoingEnabled) VALUE_MEDIA_ENABLED else VALUE_MEDIA_DISABLED
                        it.putExtra(KEY_MEDIA_STATE, state)
                    })
                    sendBroadcast(Intent(ACTION_MEDIA_STATE_BROADCAST).also {
                        it.putExtra(KEY_MEDIA_TYPE, VALUE_MEDIA_TYPE_VIDEO_SURFACE)
                    })
                }
            }
        }
        val callOpParam = CallOpParam()
        callOpParam.statusCode = pjsip_status_code.PJSIP_SC_RINGING
        call.answer(callOpParam)
        callOpParam.delete()
        this.call = call
    }

    private fun onRegistration(
        host: String,
        realm: String,
        port: Int,
        userFromName: String,
        userFromPassword: String
    ) {
        check(endpoint == null)
        val endpoint = endpoint()
        this.endpoint = endpoint
        val accountConfig = accountConfig(
            host = host,
            realm = realm,
            port = port,
            userFromName = userFromName,
            userFromPassword = userFromPassword,
            registerOnAdd = true,
            isVideoOutgoingEnabled = isVideoOutgoingEnabled
        )
        check(account == null)
        val account = object : Account() {
            override fun onRegState(prm: OnRegStateParam?) {
                if (prm == null) TODO()
                sendBroadcast(Intent(ACTION_REGISTRATION_RESULT).also {
                    it.putExtra(KEY_CODE, prm.code)
                })
            }
            override fun onIncomingCall(prm: OnIncomingCallParam?) {
                if (prm == null) TODO()
                onIncomingCall(callId = prm.callId)
            }
        }
        this.account = account
        account.create(accountConfig)
        account.info.regExpiresSec = 10 // todo
    }

    private fun onReceive(intent: Intent) {
        when (intent.action) {
            ACTION_REGISTRATION -> {
                val host = intent.getStringExtra(KEY_HOST)
                if (host.isNullOrEmpty()) error("Host is empty!")
                val realm = intent.getStringExtra(KEY_REALM)
                if (realm.isNullOrEmpty()) error("Realm is empty!")
                val port = intent.getIntExtra(KEY_PORT, -1)
                check(port > 0)
                val userFromName = intent.getStringExtra(KEY_USER_FROM_NAME)
                if (userFromName.isNullOrEmpty()) error("User from name is empty!")
                val userFromPassword = intent.getStringExtra(KEY_USER_FROM_PASSWORD).orEmpty()
                onRegistration(
                    host = host,
                    realm = realm,
                    port = port,
                    userFromName = userFromName,
                    userFromPassword = userFromPassword
                )
            }
            ACTION_REGISTRATION_RESULT -> {
                val code = intent.getIntExtra(KEY_CODE, -1)
                if (code == 200) {
                    sendBroadcast(Intent(ACTION_CALL_STATE_BROADCAST).also {
                        it.putExtra(KEY_STATE, VALUE_STATE_REGISTRATION)
                    })
                } else {
                    onAccountFinish()
                    sendBroadcast(Intent(ACTION_CALL_STATE_BROADCAST).also {
                        it.putExtra(KEY_STATE, VALUE_STATE_ERROR)
                    })
                }
            }
            ACTION_MEDIA_STATE_REQUEST -> {
                sendBroadcast(Intent(ACTION_MEDIA_STATE_BROADCAST).also {
                    it.putExtra(KEY_MEDIA_TYPE, VALUE_MEDIA_TYPE_AUDIO)
                    it.putExtra(KEY_MEDIA_SIDE, VALUE_MEDIA_OUTGOING)
                    val state = if (isAudioOutgoingEnabled) VALUE_MEDIA_ENABLED else VALUE_MEDIA_DISABLED
                    it.putExtra(KEY_MEDIA_STATE, state)
                })
                sendBroadcast(Intent(ACTION_MEDIA_STATE_BROADCAST).also {
                    it.putExtra(KEY_MEDIA_TYPE, VALUE_MEDIA_TYPE_VIDEO)
                    it.putExtra(KEY_MEDIA_SIDE, VALUE_MEDIA_OUTGOING)
                    val state = if (isVideoOutgoingEnabled) VALUE_MEDIA_ENABLED else VALUE_MEDIA_DISABLED
                    it.putExtra(KEY_MEDIA_STATE, state)
                })
                if (outgoingVideoPreview != null && incomingVideoWindow != null) {
                    sendBroadcast(Intent(ACTION_MEDIA_STATE_BROADCAST).also {
                        it.putExtra(KEY_MEDIA_TYPE, VALUE_MEDIA_TYPE_VIDEO_SURFACE)
                    })
                }
            }
            ACTION_SET_VIDEO_SURFACE -> {
                val outgoing = intent.getParcelableExtra<Surface>(VALUE_MEDIA_OUTGOING)
                log("outgoing video preview...")
                if (outgoing == null) {
                    outgoingVideoPreview?.stop()
//                    outgoingVideoPreview.delete()
//                    this.outgoingVideoPreview = null
                    log("outgoing video preview stop")
                } else {
//                    val old = outgoingVideoPreview
//                    if (old != null) {
//                        old.stop()
//                        old.delete()
//                        outgoingVideoPreview = null
//                    }
//                    val videoCallMediaInfo = requireNotNull(videoCallMediaInfo)
                    val outgoingVideoPreview = requireNotNull(outgoingVideoPreview)
                    val videoWindowHandle = VideoWindowHandle()
                    videoWindowHandle.handle.setWindow(outgoing)
                    val videoPreviewOpParam = VideoPreviewOpParam()
                    videoPreviewOpParam.window = videoWindowHandle
                    outgoingVideoPreview.start(videoPreviewOpParam)
                    videoPreviewOpParam.delete()
                    videoWindowHandle.delete()
//                    this.outgoingVideoPreview = outgoingVideoPreview
                    log("outgoing video preview start")
                }
                val incoming = intent.getParcelableExtra<Surface>(VALUE_MEDIA_INCOMING)
                log("incoming video preview...")
                val incomingVideoWindow = incomingVideoWindow
                if (incomingVideoWindow != null) {
                    val videoWindowHandle = VideoWindowHandle()
                    videoWindowHandle.handle.setWindow(incoming)
                    incomingVideoWindow.setWindow(videoWindowHandle)
                    videoWindowHandle.delete()
                }
            }
            ACTION_SET_MEDIA_STATE -> {
                when (intent.getStringExtra(KEY_MEDIA_TYPE)) {
                    VALUE_MEDIA_TYPE_AUDIO -> {
                        when (intent.getStringExtra(KEY_MEDIA_SIDE)) {
                            VALUE_MEDIA_OUTGOING -> {
                                val endpoint = requireNotNull(endpoint)
                                val call = requireNotNull(call)
                                val list = call.info.media.indices.map {
                                    call.info.media[it]
                                }.filter {
                                    it.status == pjsua_call_media_status.PJSUA_CALL_MEDIA_ACTIVE
                                }
                                val mediaInfo = list.firstOrNull {
                                    it.type == pjmedia_type.PJMEDIA_TYPE_AUDIO
                                }
                                if (mediaInfo == null) TODO()
                                val isEnabled = when (intent.getStringExtra(KEY_MEDIA_STATE)) {
                                    VALUE_MEDIA_ENABLED -> true
                                    VALUE_MEDIA_DISABLED -> false
                                    else -> TODO()
                                }
                                val audioMedia = call.getAudioMedia(mediaInfo.index.toInt())!!
                                val audDevManager = endpoint.audDevManager()
                                if (isEnabled) {
                                    audDevManager.captureDevMedia.startTransmit(audioMedia)
                                } else {
                                    audDevManager.captureDevMedia.stopTransmit(audioMedia)
                                }
                                log("switch audio enabled: $isEnabled")
                                sendBroadcast(Intent(ACTION_MEDIA_STATE_BROADCAST).also {
                                    it.putExtra(KEY_MEDIA_SIDE, VALUE_MEDIA_OUTGOING)
                                    it.putExtra(KEY_MEDIA_TYPE, VALUE_MEDIA_TYPE_AUDIO)
                                    val state = if (isEnabled) VALUE_MEDIA_ENABLED else VALUE_MEDIA_DISABLED
                                    it.putExtra(KEY_MEDIA_STATE, state)
                                })
                            }
                            else -> TODO()
                        }
                    }
                    VALUE_MEDIA_TYPE_VIDEO -> {
                        when (intent.getStringExtra(KEY_MEDIA_SIDE)) {
                            VALUE_MEDIA_OUTGOING -> {
//                                val endpoint = requireNotNull(endpoint)
                                val call = requireNotNull(call)
                                val list = call.info.media.indices.map {
                                    call.info.media[it]
                                }.filter {
                                    it.status == pjsua_call_media_status.PJSUA_CALL_MEDIA_ACTIVE
                                }
                                val mediaInfo = list.firstOrNull {
                                    it.type == pjmedia_type.PJMEDIA_TYPE_VIDEO
                                }
                                if (mediaInfo == null) TODO()
                                val isEnabled = when (intent.getStringExtra(KEY_MEDIA_STATE)) {
                                    VALUE_MEDIA_ENABLED -> true
                                    VALUE_MEDIA_DISABLED -> false
                                    else -> TODO()
                                }
                                val callVidSetStreamParam = CallVidSetStreamParam()
                                if (isEnabled) {
                                    call.vidSetStream(pjsua_call_vid_strm_op.PJSUA_CALL_VID_STRM_START_TRANSMIT, callVidSetStreamParam)
                                } else {
                                    call.vidSetStream(pjsua_call_vid_strm_op.PJSUA_CALL_VID_STRM_STOP_TRANSMIT, callVidSetStreamParam)
                                }
                                callVidSetStreamParam.delete()
                                log("vid set stream $isEnabled")
                                sendBroadcast(Intent(ACTION_MEDIA_STATE_BROADCAST).also {
                                    it.putExtra(KEY_MEDIA_SIDE, VALUE_MEDIA_OUTGOING)
                                    it.putExtra(KEY_MEDIA_TYPE, VALUE_MEDIA_TYPE_VIDEO)
                                    val state = if (isEnabled) VALUE_MEDIA_ENABLED else VALUE_MEDIA_DISABLED
                                    it.putExtra(KEY_MEDIA_STATE, state)
                                })
                            }
                            else -> TODO()
                        }
                    }
                    else -> TODO()
                }
            }
            ACTION_CALL_ACCEPT -> {
                val call = requireNotNull(call)
                val callOpParam = CallOpParam()
                callOpParam.opt.audioCount = 1
                callOpParam.opt.videoCount = 1
                callOpParam.statusCode = pjsip_status_code.PJSIP_SC_OK
                call.answer(callOpParam)
                callOpParam.delete()
            }
            ACTION_CALL_CANCEL -> {
                onCallFinish()
            }
            ACTION_CALL_STATE_BROADCAST -> {
                val state = intent.getStringExtra(KEY_STATE)
                when (state) {
                    VALUE_STATE_DISCONNECTED -> {
                        onCallFinish()
                    }
                }
            }
            ACTION_CALL_STATE_REQUEST -> {
                onCallStateRequest()
            }
            ACTION_MAKE_CALL -> {
                log("make call")
                val host = intent.getStringExtra(KEY_HOST)
                if (host.isNullOrEmpty()) error("Host is empty!")
                val realm = intent.getStringExtra(KEY_REALM)
                if (realm.isNullOrEmpty()) error("Realm is empty!")
                val port = intent.getIntExtra(KEY_PORT, -1)
                check(port > 0)
                val userFromName = intent.getStringExtra(KEY_USER_FROM_NAME)
                if (userFromName.isNullOrEmpty()) error("User from name is empty!")
                val userFromPassword = intent.getStringExtra(KEY_USER_FROM_PASSWORD).orEmpty()
                val userToName = intent.getStringExtra(KEY_USER_TO_NAME)
                if (userToName.isNullOrEmpty()) error("User to name is empty!")
                check(intent.hasExtra(KEY_AUDIO_OUTGOING_ENABLED))
                val isAudioOutgoingEnabled = intent.getBooleanExtra(KEY_AUDIO_OUTGOING_ENABLED, false)
                check(intent.hasExtra(KEY_VIDEO_OUTGOING_ENABLED))
                val isVideoOutgoingEnabled = intent.getBooleanExtra(KEY_VIDEO_OUTGOING_ENABLED, false)
                onMakeCall(
                    host = host,
                    realm = realm,
                    port = port,
                    userFromName = userFromName,
                    userFromPassword = userFromPassword,
                    userToName = userToName,
                    isAudioOutgoingEnabled = isAudioOutgoingEnabled,
                    isVideoOutgoingEnabled = isVideoOutgoingEnabled
                )
            }
            else -> TODO()
        }
    }

    private fun onReceive(executor: ExecutorService, intent: Intent) {
        executor.execute {
            endpoint?.libRegisterThread(Thread.currentThread().name)
            onReceive(intent)
        }
//        onReceive(intent)
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent != null) onReceive(executor!!, intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        executor = Executors.newSingleThreadExecutor()
//        executor = Executors.newFixedThreadPool(5)
        registerReceiver(receiver, IntentFilter().also {
            setOf(
                ACTION_REGISTRATION,
                ACTION_REGISTRATION_RESULT,
                ACTION_MEDIA_STATE_REQUEST,
                ACTION_SET_VIDEO_SURFACE,
                ACTION_SET_MEDIA_STATE,
                ACTION_CALL_ACCEPT,
                ACTION_CALL_CANCEL,
                ACTION_CALL_STATE_BROADCAST,
                ACTION_CALL_STATE_REQUEST,
                ACTION_MAKE_CALL
            ).forEach(it::addAction)
        })
        // todo
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) onReceive(executor!!, intent)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        executor?.shutdown()
        executor = null
        unregisterReceiver(receiver)
    }
}
