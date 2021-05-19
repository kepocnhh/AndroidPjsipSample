package android.pjsip.sample

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        System.loadLibrary("openh264")
//        System.loadLibrary("c++_shared")
        System.loadLibrary("pjsua2")
    }
}
