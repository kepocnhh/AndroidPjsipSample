repositories {
    mavenCentral()
    google()
}

plugins {
    id("com.android.library")
}

android {
    compileSdkVersion(30)
    buildToolsVersion("29.0.3")

    defaultConfig {
        minSdkVersion(16)
        targetSdkVersion(30)
    }
}
