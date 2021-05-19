repositories {
    mavenCentral()
    google()
}

plugins {
    id("com.android.application")
    id("kotlin-android")
}

android {
    compileSdkVersion(30)
    buildToolsVersion("29.0.3")

    defaultConfig {
        minSdkVersion(16)
        targetSdkVersion(30)
        applicationId = "test.android.pjsip"
        versionCode = 1
        versionName = "0.0.1"
        vectorDrawables.useSupportLibrary = true
        multiDexEnabled = true
    }

    sourceSets.all {
        java.srcDir("src/$name/kotlin")
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".$name"
            versionNameSuffix = "-$name"
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }
}

dependencies {
    implementation(project(":pjsip"))
    implementation(
        group = "org.jetbrains.kotlin",
        name = "kotlin-stdlib",
        version = "1.3.72"
    )
    implementation("com.android.support:support-compat:28.0.0")
}
