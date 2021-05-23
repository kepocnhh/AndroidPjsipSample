package android.pjsip.sample.v1

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.pjsip.sample.showToast
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

class DialActivity : Activity() {
    private fun onMakeCall(userToName: String) {
        if (userToName.isEmpty()) {
            showToast("User to name is empty!")
            return
        }
        sendBroadcast(Intent(CallService.ACTION_MAKE_CALL).also { intent ->
            intent.putExtra(CallService.KEY_USER_TO_NAME, userToName)
        })
        finish()
        startActivity(Intent(this, CallActivity::class.java))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(LinearLayout(this).also { root ->
            root.orientation = LinearLayout.VERTICAL
            root.addView(TextView(this).also { it.text = "user to name" })
            val userToNameEditText = EditText(this).also { it.setText("002") }
            root.addView(userToNameEditText)
            root.addView(Button(this).also {
                it.text = "make call"
                it.setOnClickListener {
                    onMakeCall(userToName = userToNameEditText.text.toString())
                }
            })
        })
    }
}
