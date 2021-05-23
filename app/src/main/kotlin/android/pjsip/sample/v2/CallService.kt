package android.pjsip.sample.v2

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.pjsip.sample.log
import android.util.Log
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import org.pjsip.pjsua2.Account
import org.pjsip.pjsua2.AccountConfig
import org.pjsip.pjsua2.AuthCredInfo
import org.pjsip.pjsua2.Call
import org.pjsip.pjsua2.CallOpParam
import org.pjsip.pjsua2.Endpoint
import org.pjsip.pjsua2.EpConfig
import org.pjsip.pjsua2.LogEntry
import org.pjsip.pjsua2.LogWriter
import org.pjsip.pjsua2.OnCallMediaStateParam
import org.pjsip.pjsua2.OnCallStateParam
import org.pjsip.pjsua2.OnRegStateParam
import org.pjsip.pjsua2.TransportConfig
import org.pjsip.pjsua2.pj_qos_type
import org.pjsip.pjsua2.pjmedia_type
import org.pjsip.pjsua2.pjsip_inv_state
import org.pjsip.pjsua2.pjsip_transport_type_e
import org.pjsip.pjsua2.pjsua_call_media_status

private class AndroidLogWriter : LogWriter() {
    override fun write(entry: LogEntry?) {
        Log.d("org.pjsip.pjsua2", entry?.msg ?: "no message")
    }
}

class CallService : Service() {
    companion object {
        const val ACTION_MAKE_CALL = "ACTION_MAKE_CALL"
        const val KEY_HOST = "KEY_HOST"
        const val KEY_REALM = "KEY_REALM"
        const val KEY_PORT = "KEY_PORT"
        const val KEY_AUDIO_OUTGOING_ENABLED = "KEY_AUDIO_OUTGOING_ENABLED"
        const val KEY_USER_TO_NAME = "KEY_USER_TO_NAME"
        const val KEY_USER_FROM_NAME = "KEY_USER_FROM_NAME"
        const val KEY_USER_FROM_PASSWORD = "KEY_USER_FROM_PASSWORD"
        const val ACTION_CALL_STATE_BROADCAST = "ACTION_CALL_STATE_BROADCAST"
        const val KEY_STATE = "KEY_STATE"
        const val VALUE_STATE_NONE = "VALUE_STATE_NONE"
        const val VALUE_STATE_ERROR = "VALUE_STATE_ERROR"
        const val VALUE_STATE_REGISTRATION = "VALUE_STATE_REGISTRATION"
        const val VALUE_STATE_EARLY = "VALUE_STATE_EARLY"
        const val VALUE_STATE_DISCONNECTED = "VALUE_STATE_DISCONNECTED"
        const val VALUE_STATE_CONFIRMED = "VALUE_STATE_CONFIRMED"
        const val VALUE_STATE_CONNECTING = "VALUE_STATE_CONNECTING"
        const val ACTION_CALL_STATE_REQUEST = "ACTION_CALL_STATE_REQUEST"
        const val KEY_CALL_TIME_START = "KEY_CALL_TIME_START"
        const val ACTION_CALL_CANCEL = "ACTION_CALL_CANCEL"
        const val ACTION_MEDIA_STATE_REQUEST = "ACTION_MEDIA_STATE_REQUEST"
        const val ACTION_MEDIA_STATE_BROADCAST = "ACTION_MEDIA_STATE_BROADCAST"
        const val KEY_MEDIA_TYPE = "KEY_MEDIA_TYPE"
        const val VALUE_MEDIA_TYPE_AUDIO = "VALUE_MEDIA_TYPE_AUDIO"
        const val VALUE_MEDIA_TYPE_VIDEO = "VALUE_MEDIA_TYPE_VIDEO"
        const val KEY_MEDIA_SIDE = "KEY_MEDIA_SIDE"
        const val VALUE_MEDIA_INCOMING = "VALUE_MEDIA_INCOMING"
        const val VALUE_MEDIA_OUTGOING = "VALUE_MEDIA_OUTGOING"
        const val KEY_MEDIA_STATE = "KEY_MEDIA_STATE"
        const val VALUE_MEDIA_ENABLED = "VALUE_MEDIA_ENABLED"
        const val VALUE_MEDIA_DISABLED = "VALUE_MEDIA_DISABLED"
        const val ACTION_SET_MEDIA_STATE = "ACTION_SET_MEDIA_STATE"
        private var logWriter: LogWriter? = null

        private fun endpoint(): Endpoint {
            val result = Endpoint()
            result.libCreate()
            val epConfig = EpConfig()
            epConfig.uaConfig.userAgent = "android.pjsip.sample"
            epConfig.uaConfig.mainThreadOnly = false
//            epConfig.uaConfig.mainThreadOnly = true
//            epConfig.uaConfig.threadCnt = 0
//            epConfig.uaConfig.threadCnt = 1
            epConfig.uaConfig.threadCnt = 5
            epConfig.logConfig = epConfig.logConfig.also {
                it.level = 4
                it.consoleLevel = 5
                logWriter?.delete()
                logWriter = AndroidLogWriter()
                it.writer = logWriter
            }
            result.libInit(epConfig)
            result.transportCreate(pjsip_transport_type_e.PJSIP_TRANSPORT_UDP, TransportConfig())
            result.libStart()
            return result
        }
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
            result.regConfig.registrarUri = uriRegistrar
            result.regConfig.timeoutSec = 60 // todo
//            result.regConfig.timeoutSec = 120 // todo
            result.regConfig.registerOnAdd = false // todo
//            result.regConfig.registerOnAdd = true // todo
            result.sipConfig.authCreds.add(
                AuthCredInfo(authScheme, realm, userFromName, 0, userFromPassword)
            )
            result.sipConfig.proxies.add(uriRegistrar)
//            result.sipConfig.contactUriParams = ""
//            result.mediaConfig.transportConfig.qosType = pj_qos_type.PJ_QOS_TYPE_VOICE
            result.videoConfig.autoTransmitOutgoing = false
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

    private fun onCallFinish() {
        requireNotNull(call).delete()
        call = null
        val account = requireNotNull(account)
        account.shutdown()
        account.delete()
        val endpoint = requireNotNull(endpoint)
        endpoint.libDestroy()
        endpoint.delete()
        this.account = null
        this.endpoint = null
    }
    private fun onMakeCall(
        host: String,
        realm: String,
        port: Int,
        userFromName: String,
        userFromPassword: String,
        userToName: String,
        isAudioOutgoingEnabled: Boolean
    ) {
        check(endpoint == null)
        val endpoint = endpoint()
        this.endpoint = endpoint
        val accountConfig = accountConfig(
            host = host,
            realm = realm,
            port = port,
            userFromName = userFromName,
            userFromPassword = userFromPassword
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
        val call = object : Call(account) {
            override fun onCallState(prm: OnCallStateParam?) {
                if (prm == null) TODO()
                log("on call state: ${info.state}")
                when (info.state) {
                    pjsip_inv_state.PJSIP_INV_STATE_EARLY -> {
                        sendBroadcast(Intent(ACTION_CALL_STATE_BROADCAST).also {
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
                    val state = if (isAudioOutgoingEnabled) VALUE_MEDIA_ENABLED else VALUE_MEDIA_DISABLED
                    sendBroadcast(Intent(ACTION_MEDIA_STATE_BROADCAST).also {
                        it.putExtra(KEY_MEDIA_SIDE, VALUE_MEDIA_OUTGOING)
                        it.putExtra(KEY_MEDIA_TYPE, VALUE_MEDIA_TYPE_AUDIO)
                        it.putExtra(KEY_MEDIA_STATE, state)
                    })
                    audioMedia.startTransmit(audDevManager.playbackDevMedia)
                }
                /*
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
                */
            }
        }
        val uriDestination = "sip:$userToName@$host"
        val callOpParam = CallOpParam()
        callOpParam.opt.audioCount = 1
        callOpParam.opt.videoCount = 0
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
        val account = requireNotNull(account)
//        if (account.info.regIsActive) TODO()
        val call = requireNotNull(call)
        val state = call.info.state
        sendBroadcast(Intent(ACTION_CALL_STATE_BROADCAST).also {
            it.putExtra(KEY_STATE, when (state) {
                pjsip_inv_state.PJSIP_INV_STATE_EARLY -> VALUE_STATE_EARLY
                pjsip_inv_state.PJSIP_INV_STATE_CONNECTING -> VALUE_STATE_CONNECTING
                pjsip_inv_state.PJSIP_INV_STATE_CONFIRMED -> VALUE_STATE_CONFIRMED
                else -> TODO()
            })
        })
    }
    private fun onReceive(intent: Intent) {
        when (intent.action) {
            ACTION_SET_MEDIA_STATE -> {
                when (intent.getStringExtra(KEY_MEDIA_TYPE)) {
                    VALUE_MEDIA_TYPE_AUDIO -> {
                        when (intent.getStringExtra(KEY_MEDIA_SIDE)) {
                            VALUE_MEDIA_OUTGOING -> {
                                val endpoint = requireNotNull(endpoint)
                                val call = requireNotNull(call)
                                val media = call.info.media.indices.map {
                                    call.info.media[it]
                                }.filter {
                                    it.status == pjsua_call_media_status.PJSUA_CALL_MEDIA_ACTIVE
                                }
                                val audio = media.firstOrNull {
                                    it.type == pjmedia_type.PJMEDIA_TYPE_AUDIO
                                }
                                if (audio != null) {
                                    val isEnabled = when (intent.getStringExtra(KEY_MEDIA_STATE)) {
                                        VALUE_MEDIA_ENABLED -> true
                                        VALUE_MEDIA_DISABLED -> false
                                        else -> TODO()
                                    }
                                    val audioMedia = call.getAudioMedia(audio.index.toInt())!!
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
                            }
                            else -> TODO()
                        }
                    }
                    VALUE_MEDIA_TYPE_VIDEO -> TODO()
                    else -> TODO()
                }
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
                onMakeCall(
                    host = host,
                    realm = realm,
                    port = port,
                    userFromName = userFromName,
                    userFromPassword = userFromPassword,
                    userToName = userToName,
                    isAudioOutgoingEnabled = isAudioOutgoingEnabled
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
//        executor = Executors.newSingleThreadExecutor()
        executor = Executors.newFixedThreadPool(5)
        registerReceiver(receiver, IntentFilter().also {
            setOf(
                ACTION_SET_MEDIA_STATE,
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
