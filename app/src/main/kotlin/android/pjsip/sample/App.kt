package android.pjsip.sample

import android.app.Application
import android.content.Intent

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        System.loadLibrary("openh264")
//        System.loadLibrary("c++_shared")
        System.loadLibrary("pjsua2")
//        startService(Intent(this, CallService::class.java))
    }
}
