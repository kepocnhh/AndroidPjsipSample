package android.pjsip.sample.v1

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
import org.pjsip.pjsua2.AudioMedia
import org.pjsip.pjsua2.AuthCredInfo
import org.pjsip.pjsua2.Call
import org.pjsip.pjsua2.CallMediaInfo
import org.pjsip.pjsua2.CallOpParam
import org.pjsip.pjsua2.CallVidSetStreamParam
import org.pjsip.pjsua2.Endpoint
import org.pjsip.pjsua2.EpConfig
import org.pjsip.pjsua2.LogConfig
import org.pjsip.pjsua2.LogEntry
import org.pjsip.pjsua2.LogWriter
import org.pjsip.pjsua2.OnCallMediaStateParam
import org.pjsip.pjsua2.OnCallStateParam
import org.pjsip.pjsua2.OnIncomingCallParam
import org.pjsip.pjsua2.OnRegStateParam
import org.pjsip.pjsua2.OnSelectAccountParam
import org.pjsip.pjsua2.StringVector
import org.pjsip.pjsua2.TransportConfig
import org.pjsip.pjsua2.VideoWindow
import org.pjsip.pjsua2.VideoWindowHandle
import org.pjsip.pjsua2.pj_log_decoration
import org.pjsip.pjsua2.pj_qos_type
import org.pjsip.pjsua2.pjmedia_type
import org.pjsip.pjsua2.pjsip_inv_state
import org.pjsip.pjsua2.pjsip_status_code
import org.pjsip.pjsua2.pjsip_transport_type_e
import org.pjsip.pjsua2.pjsua_call_flag
import org.pjsip.pjsua2.pjsua_call_media_status
import org.pjsip.pjsua2.pjsua_call_vid_strm_op
import org.pjsip.pjsua2.pjsua_destroy_flag

private class AndroidLogWriter : LogWriter() {
    override fun write(entry: LogEntry?) {
//        println(entry?.msg)
        Log.d("org.pjsip.pjsua2", entry?.msg ?: "no message")
    }
}

class CallService : Service() {
    companion object {
        const val ACTION_STATUS_REQUEST = "ACTION_STATUS_REQUEST"
        const val ACTION_STATUS_RESPONSE = "ACTION_STATUS_RESPONSE"
        const val KEY_ACCOUNT_STATUS = "KEY_ACCOUNT_STATUS"
        const val VALUE_NO_ACCOUNT = "VALUE_NO_ACCOUNT"
        const val VALUE_ACCOUNT_EXISTS = "VALUE_ACCOUNT_EXISTS"
        const val VALUE_ERROR = "VALUE_ERROR"
        const val VALUE_SUCCESS = "VALUE_SUCCESS"
        const val ACTION_REGISTRATION_REQUEST = "ACTION_REGISTRATION_REQUEST"
        private const val ACTION_REGISTRATION_RESULT = "ACTION_REGISTRATION_RESULT"
        private const val KEY_REGISTRATION_STATUS = "KEY_REGISTRATION_STATUS"
        private const val VALUE_REGISTRATION_SUCCESS = "VALUE_REGISTRATION_SUCCESS"
        private const val VALUE_REGISTRATION_ERROR = "VALUE_REGISTRATION_ERROR"
        const val ACTION_REGISTRATION_RESPONSE = "ACTION_REGISTRATION_RESPONSE"
        const val KEY_HOST = "KEY_HOST"
        const val KEY_REALM = "KEY_REALM"
        const val KEY_PORT = "KEY_PORT"
        const val KEY_USER_TO_NAME = "KEY_USER_TO_NAME"
        const val KEY_USER_FROM_NAME = "KEY_USER_FROM_NAME"
        const val KEY_USER_FROM_PASSWORD = "KEY_USER_FROM_PASSWORD"
        const val ACTION_MAKE_CALL = "ACTION_MAKE_CALL"
        const val ACTION_CALL_RESULT = "ACTION_CALL_RESULT"
        const val KEY_CALL_TYPE = "KEY_CALL_TYPE"
        const val KEY_CALL_STATE = "KEY_CALL_STATE"
        const val KEY_CALL_TIME = "KEY_CALL_TIME"
        const val VALUE_CALL_OUTGOING = "VALUE_CALL_OUTGOING"
        const val VALUE_CALL_INCOMING = "VALUE_CALL_INCOMING"
        const val ACTION_CALL_CANCEL = "ACTION_CALL_CANCEL"
        const val ACTION_CALL_STATE_REQUEST = "ACTION_CALL_STATE_REQUEST"
        const val ACTION_CALL_STATE_BROADCAST = "ACTION_CALL_STATE_BROADCAST"
        const val ACTION_MEDIA_STATE_REQUEST = "ACTION_MEDIA_STATE_REQUEST"
        const val ACTION_MEDIA_STATE_BROADCAST = "ACTION_MEDIA_STATE_BROADCAST"
        const val ACTION_SET_AUDIO_STATE = "ACTION_SET_AUDIO_STATE"
        const val ACTION_SET_VIDEO_STATE = "ACTION_SET_VIDEO_STATE"
        const val VALUE_MEDIA_ENABLED = "VALUE_MEDIA_ENABLED"
        const val VALUE_MEDIA_DISABLED = "VALUE_MEDIA_DISABLED"
        const val ACTION_SET_VIDEO_INCOMING_STATE = "ACTION_SET_VIDEO_INCOMING_STATE"
        const val KEY_VIDEO_STATE = "KEY_VIDEO_STATE"
        const val KEY_VIDEO_SURFACE = "KEY_VIDEO_SURFACE"
        const val KEY_MEDIA_ACTION = "KEY_MEDIA_ACTION"
        const val KEY_MEDIA_STATE = "KEY_MEDIA_STATE"
        const val KEY_MEDIA_TYPE = "KEY_MEDIA_TYPE"
        const val KEY_MEDIA_SIDE = "KEY_MEDIA_SIDE"
        const val VALUE_MEDIA_START_RECEIVE = "VALUE_MEDIA_START_RECEIVE"
        const val VALUE_MEDIA_TYPE_AUDIO = "VALUE_MEDIA_TYPE_AUDIO"
        const val VALUE_MEDIA_TYPE_VIDEO = "VALUE_MEDIA_TYPE_VIDEO"
        const val VALUE_MEDIA_SET_STATE = "VALUE_MEDIA_SET_STATE"
        const val VALUE_MEDIA_INCOMING = "VALUE_MEDIA_INCOMING"
        const val VALUE_MEDIA_OUTGOING = "VALUE_MEDIA_OUTGOING"

        private val DEFAULT_CODEC_PRIORITIES = listOf<Pair<String, Short>>(
//            "OPUS"        to 0,
            "PCMA/8000"   to 254,
            "PCMU/8000"   to 0,
//            "G729/8000"   to 0,
            "speex/8000"  to 0,
            "speex/16000" to 0,
            "speex/32000" to 0,
            "GSM/8000"    to 0,
            "G722/16000"  to 0,
//            "G7221/16000" to 0,
//            "G7221/32000" to 0,
            "ilbc/8000"   to 0
        )
        private const val H264_CODEC_ID = "H264/97"
    }

    private var endpoint: Endpoint? = null
    private var host: String? = null
    private var account: Account? = null
    private var call: Call? = null
    private var mediaAudioEnabled: Boolean = false
    private var incomingVideoWindow: VideoWindow? = null
    private var incomingVideoCallMediaInfo: CallMediaInfo? = null
    private var videoOutgoingEnabled: Boolean = false
    private var lastCallTime: Long? = null
    private var logWriter: LogWriter? = null
    private var executor: ExecutorService? = null

    private fun accountConfig(
        host: String,
        realm: String,
        port: Int,
        userFromName: String,
        userFromPassword: String
    ): AccountConfig {
        val uriId = "sip:$userFromName@$realm"
        val uriRegistrar = "sip:$host:$port"
        val authScheme = "digest"
        val result = AccountConfig()
        result.idUri = uriId
//        accountConfig.regConfig.setCallID(callId) // todo
        result.regConfig.registrarUri = uriRegistrar
        result.regConfig.timeoutSec = 60 // todo
//        result.regConfig.timeoutSec = 120 // todo
//        result.regConfig.registerOnAdd = false // todo
        result.regConfig.registerOnAdd = true // todo
        result.sipConfig.authCreds.add(
            AuthCredInfo(authScheme, realm, userFromName, 0, userFromPassword)
        )
        result.sipConfig.proxies.add(uriRegistrar)
        result.sipConfig.contactUriParams = ""
        result.mediaConfig.transportConfig.qosType = pj_qos_type.PJ_QOS_TYPE_VOICE
        result.videoConfig.autoTransmitOutgoing = false
        result.videoConfig.autoShowIncoming = true
        result.videoConfig.defaultCaptureDevice = 1 // todo front
        result.videoConfig.defaultRenderDevice = 0 // todo
        return result
    }
    private fun endpoint(
        videoWidth: Long,
        videoHeight: Long
    ): Endpoint {
//        val result = object : Endpoint() {
//            override fun onSelectAccount(prm: OnSelectAccountParam?) {
//                log("on select account " + prm?.accountIndex)
//            }
//        }
//        val result = Endpoint.instance()
        val result = Endpoint()
//        result.libRegisterThread(this::class.java.name)
        result.libCreate()
        log("pjsip|endpoint: lib create")
        //
        val epConfig = EpConfig()
        epConfig.uaConfig.userAgent = "android.pjsip.sample"
        epConfig.uaConfig.mainThreadOnly = false
//        epConfig.uaConfig.mainThreadOnly = true
//        epConfig.uaConfig.threadCnt = 0
        epConfig.uaConfig.threadCnt = 1
//        val stunServers = setOf("stun.pjsip.org")
//        epConfig.uaConfig.stunServer = StringVector().also { vector ->
//            stunServers.forEach { vector.add(it) }
//        }
        //
//        epConfig.medConfig.hasIoqueue = true
//        epConfig.medConfig.clockRate = 16000
//        epConfig.medConfig.quality = 10
//        epConfig.medConfig.ecOptions = 1
//        epConfig.medConfig.ecTailLen = 200
//        epConfig.medConfig.threadCnt = 2
        //
        epConfig.logConfig = epConfig.logConfig.also {
            it.level = 4
            it.consoleLevel = 5
            logWriter = AndroidLogWriter()
            it.writer = logWriter
        }
        result.libInit(epConfig)
        log("pjsip|endpoint: lib init")
        result.transportCreate(pjsip_transport_type_e.PJSIP_TRANSPORT_UDP, TransportConfig().also {
            it.qosType = pj_qos_type.PJ_QOS_TYPE_VOICE
        })
//        result.transportCreate(pjsip_transport_type_e.PJSIP_TRANSPORT_TCP, TransportConfig().also {
//            it.qosType = pj_qos_type.PJ_QOS_TYPE_VOICE
//        })
        result.libStart()
        log("pjsip|endpoint: lib start")
        //
//        DEFAULT_CODEC_PRIORITIES.forEach { (id, priority) ->
//            result.codecSetPriority(id, priority)
//        }
        // Set H264 Parameters
        /*
        val vidCodecParam = result.getVideoCodecParam(H264_CODEC_ID)
        vidCodecParam.encFmt = vidCodecParam.encFmt.also {
            it.width = videoWidth
            it.height = videoHeight
        }
        val decFmtp = vidCodecParam.decFmtp
        for (i in 0 until decFmtp.size) {
            if (decFmtp[i].name == "profile-level-id") {
                decFmtp[i].setVal("42e01f")
                break
            }
        }
        vidCodecParam.decFmtp = decFmtp
        result.setVideoCodecParam(H264_CODEC_ID, vidCodecParam)
        log("pjsip|endpoint: set video codec param")
        */
        //
        return result
    }
    private fun onAudio(enabled: Boolean) {
        if (enabled == mediaAudioEnabled) return
        mediaAudioEnabled = enabled
        val endpoint = requireNotNull(endpoint)
        val call = requireNotNull(call)
        for (i in call.info.media.indices) {
            val mediaInfo = call.info.media[i]
            if (mediaInfo.status != pjsua_call_media_status.PJSUA_CALL_MEDIA_ACTIVE) continue
            if (mediaInfo.type != pjmedia_type.PJMEDIA_TYPE_AUDIO) continue
            val audioMedia = call.getAudioMedia(i)!!
            val audDevManager = endpoint.audDevManager()
            if (enabled) {
                audDevManager.captureDevMedia.startTransmit(audioMedia)
            } else {
                audDevManager.captureDevMedia.stopTransmit(audioMedia)
            }
            log("switch audio enabled: $enabled")
            sendBroadcast(Intent(ACTION_MEDIA_STATE_BROADCAST).also {
                it.putExtra(KEY_MEDIA_TYPE, VALUE_MEDIA_TYPE_AUDIO)
                val value = if (enabled) VALUE_MEDIA_ENABLED else VALUE_MEDIA_DISABLED
                it.putExtra(KEY_MEDIA_STATE, value)
            })
        }
    }
    private fun onVideoOutgoing(enabled: Boolean) {
        if (enabled == videoOutgoingEnabled) return
        videoOutgoingEnabled = enabled
        val call = requireNotNull(call)
        val callVidSetStreamParam = CallVidSetStreamParam()
        if (enabled) {
            call.vidSetStream(pjsua_call_vid_strm_op.PJSUA_CALL_VID_STRM_START_TRANSMIT, callVidSetStreamParam)
        } else {
            call.vidSetStream(pjsua_call_vid_strm_op.PJSUA_CALL_VID_STRM_STOP_TRANSMIT, callVidSetStreamParam)
        }
        callVidSetStreamParam.delete()
        sendBroadcast(Intent(ACTION_MEDIA_STATE_BROADCAST).also {
            it.putExtra(KEY_MEDIA_TYPE, VALUE_MEDIA_TYPE_VIDEO)
            it.putExtra(KEY_MEDIA_ACTION, VALUE_MEDIA_SET_STATE)
            val value = if (enabled) VALUE_MEDIA_ENABLED else VALUE_MEDIA_DISABLED
            it.putExtra(KEY_MEDIA_STATE, value)
        })
    }
    private fun onVideoIncoming(enabled: Boolean) {
        if (enabled) {
            check(incomingVideoWindow == null)
            val id = requireNotNull(incomingVideoCallMediaInfo).videoIncomingWindowId
            incomingVideoWindow = VideoWindow(id)
        } else {
            requireNotNull(incomingVideoWindow).delete()
            incomingVideoWindow = null
        }
    }
    private fun onCallFinish() {
        incomingVideoCallMediaInfo?.delete()
        incomingVideoCallMediaInfo = null
        incomingVideoWindow?.delete()
        incomingVideoWindow = null
        requireNotNull(call).delete()
        call = null
    }
    private fun onReceive(intent: Intent) {
        when (intent.action) {
            ACTION_SET_VIDEO_INCOMING_STATE -> {
                val state = intent.getStringExtra(KEY_VIDEO_STATE)
                val enabled = when (state) {
                    VALUE_MEDIA_ENABLED -> true
                    VALUE_MEDIA_DISABLED -> false
                    else -> TODO()
                }
                val incomingVideoWindow = incomingVideoWindow ?: return
                val call = requireNotNull(call)
                log("on receive ${intent.action} video state $state")
                val videoWindowHandle = VideoWindowHandle()
                val callVidSetStreamParam = CallVidSetStreamParam()
                if (enabled) {
                    val surface = intent.getParcelableExtra<Surface>(KEY_VIDEO_SURFACE)
                    videoWindowHandle.handle.setWindow(surface)
//                    call.vidSetStream(pjsua_call_vid_strm_op.PJSUA_CALL_VID_STRM_START_TRANSMIT, callVidSetStreamParam)
                } else {
                    videoWindowHandle.handle.setWindow(null)
//                    call.vidSetStream(pjsua_call_vid_strm_op.PJSUA_CALL_VID_STRM_STOP_TRANSMIT, callVidSetStreamParam)
                }
                incomingVideoWindow.setWindow(videoWindowHandle)
                callVidSetStreamParam.delete()
                videoWindowHandle.delete()
            }
            ACTION_SET_AUDIO_STATE -> {
                val state = intent.getStringExtra(KEY_MEDIA_STATE)
                val enabled = when (state) {
                    VALUE_MEDIA_ENABLED -> true
                    VALUE_MEDIA_DISABLED -> false
                    else -> TODO()
                }
                onAudio(enabled = enabled)
            }
            ACTION_SET_VIDEO_STATE -> {
                val state = intent.getStringExtra(KEY_MEDIA_STATE)
                val enabled = when (state) {
                    VALUE_MEDIA_ENABLED -> true
                    VALUE_MEDIA_DISABLED -> false
                    else -> TODO()
                }
                val side = intent.getStringExtra(KEY_MEDIA_SIDE)
                when (side) {
                    VALUE_MEDIA_OUTGOING -> {
                        onVideoOutgoing(enabled = enabled)
                    }
                    VALUE_MEDIA_INCOMING -> {
                        onVideoIncoming(enabled = enabled)
                    }
                    else -> TODO()
                }
            }
            ACTION_MEDIA_STATE_REQUEST -> {
                sendBroadcast(Intent(ACTION_MEDIA_STATE_BROADCAST).also {
                    it.putExtra(KEY_MEDIA_TYPE, VALUE_MEDIA_TYPE_AUDIO)
                    val value = if (mediaAudioEnabled) VALUE_MEDIA_ENABLED else VALUE_MEDIA_DISABLED
                    it.putExtra(KEY_MEDIA_STATE, value)
                })
                sendBroadcast(Intent(ACTION_MEDIA_STATE_BROADCAST).also {
                    it.putExtra(KEY_MEDIA_TYPE, VALUE_MEDIA_TYPE_VIDEO)
                    it.putExtra(KEY_MEDIA_ACTION, VALUE_MEDIA_SET_STATE)
                    val value = if (videoOutgoingEnabled) VALUE_MEDIA_ENABLED else VALUE_MEDIA_DISABLED
                    it.putExtra(KEY_MEDIA_STATE, value)
                })
            }
            ACTION_STATUS_REQUEST -> {
                val account = account
                if (account == null) {
                    sendBroadcast(Intent(ACTION_STATUS_RESPONSE).also {
                        it.putExtra(KEY_ACCOUNT_STATUS, VALUE_NO_ACCOUNT)
                    })
                } else {
                    sendBroadcast(Intent(ACTION_STATUS_RESPONSE).also {
                        it.putExtra(KEY_ACCOUNT_STATUS, VALUE_ACCOUNT_EXISTS)
                    })
                }
            }
            ACTION_REGISTRATION_REQUEST -> {
                val host = intent.getStringExtra(KEY_HOST)
                if (host.isNullOrEmpty()) error("Host is empty!")
                val realm = intent.getStringExtra(KEY_REALM)
                if (realm.isNullOrEmpty()) error("Realm is empty!")
                val port = intent.getIntExtra(KEY_PORT, -1)
                check(port > 0)
                val userFromName = intent.getStringExtra(KEY_USER_FROM_NAME)
                if (userFromName.isNullOrEmpty()) error("User from name is empty!")
                val userFromPassword = intent.getStringExtra(KEY_USER_FROM_PASSWORD).orEmpty()
                check(endpoint == null)
                val endpoint = endpoint(
                    videoWidth = 640,
                    videoHeight = 480
                )
                log("endpoint " + endpoint.libGetState())
                val accountConfig = accountConfig(
                    host = host,
                    realm = realm,
                    port = port,
                    userFromName = userFromName,
                    userFromPassword = userFromPassword
                )
                log("account config " + accountConfig.idUri)
                check(account == null)
                val account = object : Account() {
                    override fun onRegState(prm: OnRegStateParam?) {
                        if (prm == null) TODO()
                        log("on reg state " + prm.code + " " + prm.reason)
                        when (prm.code) {
                            200 -> {
                                this@CallService.host = host
                                sendBroadcast(Intent(ACTION_REGISTRATION_RESPONSE).also {
                                    it.putExtra(KEY_ACCOUNT_STATUS, VALUE_SUCCESS)
                                })
                            }
                            else -> {
//                                shutdown()
//                                delete()
//                                Thread.sleep(3_000)
                                sendBroadcast(Intent(ACTION_REGISTRATION_RESULT).also {
                                    it.putExtra(KEY_REGISTRATION_STATUS, VALUE_REGISTRATION_ERROR)
                                })
//                                onRegistrationError(endpoint)
                            }
                        }
                    }
                }
                this.endpoint = endpoint
                this.account = account
                try {
//                    log("account create...")
                    account.create(accountConfig)
                    account.info.regExpiresSec = 10 // todo
//                    log("account create " + account.info.id)
                } catch (e: Throwable) {
                    sendBroadcast(Intent(ACTION_REGISTRATION_RESULT).also {
                        it.putExtra(KEY_REGISTRATION_STATUS, VALUE_REGISTRATION_ERROR)
                    })
                }
            }
            ACTION_REGISTRATION_RESULT -> {
                val status = intent.getStringExtra(KEY_REGISTRATION_STATUS)
                when (status) {
                    VALUE_REGISTRATION_SUCCESS -> TODO()
                    VALUE_REGISTRATION_ERROR -> {
//                        Thread.sleep(3_000)
                        val account = requireNotNull(account)
                        account.shutdown()
                        account.delete()
                        val endpoint = requireNotNull(endpoint)
                        endpoint.libDestroy()
                        endpoint.delete()
                        this.account = null
                        this.endpoint = null
                        sendBroadcast(Intent(ACTION_REGISTRATION_RESPONSE).also {
                            it.putExtra(KEY_ACCOUNT_STATUS, VALUE_ERROR)
                        })
                    }
                }
            }
            ACTION_MAKE_CALL -> {
                val userToName = intent.getStringExtra(KEY_USER_TO_NAME)
                if (userToName.isNullOrEmpty()) error("User to name is empty!")
//                requireNotNull(endpoint).audDevManager().setNullDev() // todo
                val account = requireNotNull(account)
                val host = requireNotNull(host)
                check(account.info.regIsActive)
                check(call == null)
                val call = object : Call(account) {
                    override fun onCallState(prm: OnCallStateParam?) {
//                        log("on call state")
                        if (prm == null) TODO()
                        log("outgoing call state " + info.state)
//                        log("outgoing call state text " + info.stateText)
//                        log("outgoing call reason " + info.lastReason)
//                        log("outgoing call status code " + info.lastStatusCode)
                        when (info.state) {
                            pjsip_inv_state.PJSIP_INV_STATE_DISCONNECTED -> {
                                sendBroadcast(Intent(ACTION_CALL_RESULT).also {
                                    it.putExtra(KEY_CALL_TYPE, VALUE_CALL_OUTGOING)
                                    it.putExtra(KEY_CALL_STATE, info.state)
                                })
                                sendBroadcast(Intent(ACTION_CALL_STATE_BROADCAST).also {
                                    it.putExtra(KEY_CALL_STATE, info.state)
                                })
                            }
                            pjsip_inv_state.PJSIP_INV_STATE_CONFIRMED -> {
                                val time = System.currentTimeMillis()
                                lastCallTime = time
//                                sendBroadcast(Intent(ACTION_CALL_RESULT).also {
//                                    it.putExtra(KEY_CALL_TYPE, VALUE_CALL_OUTGOING)
//                                    it.putExtra(KEY_CALL_STATUS, VALUE_CALL_DISCONNECTED)
//                                })
                                sendBroadcast(Intent(ACTION_CALL_STATE_BROADCAST).also {
                                    it.putExtra(KEY_CALL_STATE, info.state)
                                    it.putExtra(KEY_CALL_TIME, time)
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
                            val audDevManager = requireNotNull(endpoint).audDevManager()
                            if (mediaAudioEnabled) {
                                audDevManager.captureDevMedia.startTransmit(audioMedia)
                            }
                            audioMedia.startTransmit(audDevManager.playbackDevMedia)
                        }
                        val video = media.firstOrNull {
                            it.type == pjmedia_type.PJMEDIA_TYPE_VIDEO
                        }
                        if (video != null) {
                            incomingVideoCallMediaInfo = video
                            incomingVideoWindow = VideoWindow(video.videoIncomingWindowId)
                            val call = requireNotNull(call)
                            val callVidSetStreamParam = CallVidSetStreamParam()
                            if (videoOutgoingEnabled) {
                                call.vidSetStream(pjsua_call_vid_strm_op.PJSUA_CALL_VID_STRM_START_TRANSMIT, callVidSetStreamParam)
                            } else {
                                call.vidSetStream(pjsua_call_vid_strm_op.PJSUA_CALL_VID_STRM_STOP_TRANSMIT, callVidSetStreamParam)
                            }
                            callVidSetStreamParam.delete()
                            sendBroadcast(Intent(ACTION_MEDIA_STATE_BROADCAST).also {
                                it.putExtra(KEY_MEDIA_TYPE, VALUE_MEDIA_TYPE_VIDEO)
                                it.putExtra(KEY_MEDIA_ACTION, VALUE_MEDIA_START_RECEIVE)
                            })
                        }
                    }
                }
                this.call = call
                account.info.uri
                val uriDestination = "sip:$userToName@$host"
                val callOpParam = CallOpParam()
                callOpParam.opt.audioCount = 1
                callOpParam.opt.videoCount = 1
                callOpParam.opt.flag = pjsua_call_flag.PJSUA_CALL_UNHOLD.toLong()
                mediaAudioEnabled = true // todo
                videoOutgoingEnabled = true // todo
                call.makeCall(uriDestination, callOpParam)
                callOpParam.delete()
//                val id = call.id
//                log("call $id")
            }
            ACTION_CALL_RESULT -> {
                val type = intent.getStringExtra(KEY_CALL_TYPE)
                when (type) {
                    VALUE_CALL_OUTGOING -> {
                        val state = intent.getIntExtra(KEY_CALL_STATE, -1)
                        when (state) {
                            pjsip_inv_state.PJSIP_INV_STATE_DISCONNECTED -> {
                                log("outgoing call disconnected")
                                onCallFinish()
                            }
                            pjsip_inv_state.PJSIP_INV_STATE_CONFIRMED -> TODO()
                            else -> TODO()
                        }
                    }
                    VALUE_CALL_INCOMING -> TODO()
                    else -> TODO()
                }
            }
            ACTION_CALL_CANCEL -> {
                log("call cancel")
                onCallFinish()
            }
            ACTION_CALL_STATE_REQUEST -> {
                val call = requireNotNull(call)
                val state = call.info.state
                sendBroadcast(Intent(ACTION_CALL_STATE_BROADCAST).also {
                    it.putExtra(KEY_CALL_STATE, state)
                    if (state == pjsip_inv_state.PJSIP_INV_STATE_CONFIRMED) {
                        val time = requireNotNull(lastCallTime)
                        it.putExtra(KEY_CALL_TIME, time)
                    }
                })
            }
            else -> TODO()
        }
    }

    private fun onReceive(executor: ExecutorService, intent: Intent) {
        executor.execute {
            onReceive(intent)
        }
    }
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            log("on receive ${intent?.action}")
            if (intent != null) onReceive(executor!!, intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        log("on create")
        executor = Executors.newSingleThreadExecutor()
        registerReceiver(receiver, IntentFilter().also {
            setOf(
                ACTION_SET_VIDEO_INCOMING_STATE,
                ACTION_SET_AUDIO_STATE,
                ACTION_SET_VIDEO_STATE,
                ACTION_MEDIA_STATE_REQUEST,
                ACTION_CALL_STATE_REQUEST,
                ACTION_CALL_CANCEL,
                ACTION_STATUS_REQUEST,
                ACTION_REGISTRATION_REQUEST,
                ACTION_REGISTRATION_RESULT,
                ACTION_MAKE_CALL,
                ACTION_CALL_RESULT
            ).forEach(it::addAction)
        })
        // todo
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        log("on start command ${intent?.action}")
        if (intent != null) onReceive(executor!!, intent)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        log("on destroy")
        executor?.shutdown()
        executor = null
        unregisterReceiver(receiver)
        // todo
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
