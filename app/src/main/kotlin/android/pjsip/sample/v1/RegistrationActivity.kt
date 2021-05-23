package android.pjsip.sample.v1

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.pjsip.sample.log
import android.pjsip.sample.showToast
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

class RegistrationActivity : Activity() {
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            log("on receive ${intent.action}")
            when (intent.action) {
                CallService.ACTION_REGISTRATION_RESPONSE -> {
                    val status = intent.getStringExtra(CallService.KEY_ACCOUNT_STATUS)
                    when (status) {
                        CallService.VALUE_ERROR -> {
                            showToast("error")
                        }
                        CallService.VALUE_SUCCESS -> {
                            finish()
                            startActivity(Intent(this@RegistrationActivity, MainActivity::class.java))
                        }
                        else -> TODO()
                    }
                }
            }
        }
    }

    private fun onRegistration(
        host: String,
        realm: String,
        portText: String,
        userFromName: String,
        userFromPassword: String
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
        sendBroadcast(Intent(CallService.ACTION_REGISTRATION_REQUEST).also { intent ->
            intent.putExtra(CallService.KEY_HOST, host)
            intent.putExtra(CallService.KEY_REALM, realm)
            intent.putExtra(CallService.KEY_PORT, port)
            intent.putExtra(CallService.KEY_USER_FROM_NAME, userFromName)
            intent.putExtra(CallService.KEY_USER_FROM_PASSWORD, userFromPassword)
        })
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val defaultHost = ""
        val defaultRealm = ""
        val defaultPort = 5060
        val defaultUserFromName = ""
        val defaultUserFromPassword = ""
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
            root.addView(Button(this).also {
                it.text = "register"
                it.setOnClickListener {
                    onRegistration(
                        host = hostEditText.text.toString(),
                        realm = realmEditText.text.toString(),
                        portText = portEditText.text.toString(),
                        userFromName = userFromNameEditText.text.toString(),
                        userFromPassword = userFromPasswordEditText.text.toString()
                    )
                }
            })
        })
        log("on create")
        registerReceiver(receiver, IntentFilter().also {
            setOf(
                CallService.ACTION_REGISTRATION_RESPONSE
            ).forEach(it::addAction)
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
        // todo
    }
}
