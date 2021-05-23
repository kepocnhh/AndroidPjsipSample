package android.pjsip.sample.v2

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.pjsip.sample.log
import android.pjsip.sample.showToast
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private var receiver: BroadcastReceiver? = null

    private fun onMakeCall(
        host: String,
        realm: String,
        portText: String,
        userFromName: String,
        userFromPassword: String,
        userToName: String,
        isAudioOutgoingEnabled: Boolean
    ) {
        if (host.isEmpty()) {
            showToast("Host is empty!")
            return
        }
        if (realm.isEmpty()) {
            showToast("Realm is empty!")
            return
        }
        val port = portText.toIntOrNull()
        if (port == null) {
            showToast("Port is empty!")
            return
        }
        if (port < 0) {
            showToast("Port error!")
            return
        }
        if (userFromName.isEmpty()) {
            showToast("User from name is empty!")
            return
        }
        if (userToName.isEmpty()) {
            showToast("User to name is empty!")
            return
        }
        log("make call")
        startService(Intent(this, CallService::class.java).also { intent ->
            intent.action = CallService.ACTION_MAKE_CALL
            intent.putExtra(CallService.KEY_HOST, host)
            intent.putExtra(CallService.KEY_REALM, realm)
            intent.putExtra(CallService.KEY_PORT, port)
            intent.putExtra(CallService.KEY_USER_FROM_NAME, userFromName)
            intent.putExtra(CallService.KEY_USER_FROM_PASSWORD, userFromPassword)
            intent.putExtra(CallService.KEY_USER_TO_NAME, userToName)
            intent.putExtra(CallService.KEY_AUDIO_OUTGOING_ENABLED, isAudioOutgoingEnabled)
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val defaultHost = ""
        val defaultRealm = ""
        val defaultPort = 5060
        val defaultUserFromName = "001"
        val defaultUserFromPassword = ""
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent == null) return
                when (intent.action) {
                    CallService.ACTION_CALL_STATE_BROADCAST -> {
                        val state = intent.getStringExtra(CallService.KEY_STATE)
                        showToast("call state $state")
                        when (state) {
                            CallService.VALUE_STATE_ERROR -> {
                                showToast("error")
                            }
                            CallService.VALUE_STATE_DISCONNECTED -> {
                                showToast("disconnected")
                            }
                            CallService.VALUE_STATE_REGISTRATION -> {
                                showToast("registration ok")
                            }
                            CallService.VALUE_STATE_EARLY -> {
                                unregisterReceiver(receiver)
                                receiver = null
                                finish()
                                startActivity(Intent(this@MainActivity, CallActivity::class.java))
                            }
                        }
                    }
                }
            }
        }
        setContentView(LinearLayout(this).also { root ->
            root.orientation = LinearLayout.VERTICAL
            root.addView(TextView(this).also { it.text = "host" })
            val hostEditText = EditText(this).also { it.setText(defaultHost) }
            root.addView(hostEditText)
            root.addView(TextView(this).also { it.text = "realm" })
            val realmEditText = EditText(this).also { it.setText(defaultRealm) }
            root.addView(realmEditText)
            root.addView(TextView(this).also { it.text = "port" })
            val portEditText = EditText(this).also { it.setText(defaultPort.toString()) }
            root.addView(portEditText)
            root.addView(TextView(this).also { it.text = "user from name" })
            val userFromNameEditText = EditText(this).also { it.setText(defaultUserFromName) }
            root.addView(userFromNameEditText)
            root.addView(TextView(this).also { it.text = "user from password" })
            val userFromPasswordEditText = EditText(this).also { it.setText(defaultUserFromPassword) }
            root.addView(userFromPasswordEditText)
            root.addView(TextView(this).also { it.text = "user to name" })
            val userToNameEditText = EditText(this).also { it.setText("002") }
            root.addView(userToNameEditText)
            val audioOutgoingCheckBox = CheckBox(this)
            root.addView(LinearLayout(this).also { container ->
                container.addView(audioOutgoingCheckBox)
                container.addView(TextView(this).also { it.text = "audio outgoing" })
            })
            audioOutgoingCheckBox.isChecked = true
            root.addView(Button(this).also {
                it.text = "make call"
                it.setOnClickListener {
                    onMakeCall(
                        host = hostEditText.text.toString(),
                        realm = realmEditText.text.toString(),
                        portText = portEditText.text.toString(),
                        userFromName = userFromNameEditText.text.toString(),
                        userFromPassword = userFromPasswordEditText.text.toString(),
                        userToName = userToNameEditText.text.toString(),
                        isAudioOutgoingEnabled = audioOutgoingCheckBox.isChecked
                    )
                }
            })
        })
        registerReceiver(receiver, IntentFilter().also {
            setOf(
                CallService.ACTION_CALL_STATE_BROADCAST
            ).forEach(it::addAction)
        })
        this.receiver = receiver
    }

    override fun onDestroy() {
        super.onDestroy()
        val receiver = receiver
        if (receiver != null) {
            unregisterReceiver(receiver)
            this.receiver = null
        }
        // todo
    }
}
