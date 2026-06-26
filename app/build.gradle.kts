import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Version + signing are read from files so they can be set manually and in CI.
val versionProps = Properties().apply {
    rootProject.file("version.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
val keystoreProps = Properties().apply {
    rootProject.file("keystore.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
// Release keystore comes from CI env (secrets) or a local keystore.properties; null/blank means none.
val releaseStorePath: String? = (System.getenv("KEYSTORE_FILE") ?: keystoreProps.getProperty("storeFile"))
    ?.takeIf { it.isNotBlank() }

android {
    namespace = "com.vaibhawmishra.voela"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }
    // NDK r27+ produces 16KB-aligned shared libraries by default (needed for our 16KB-page targets)
    ndkVersion = "28.2.13676358"

    defaultConfig {
        applicationId = "com.vaibhawmishra.voela"
        minSdk = 26
        targetSdk = 36
        versionCode = (versionProps.getProperty("VERSION_CODE") ?: "1").trim().toInt()
        versionName = (versionProps.getProperty("VERSION_NAME") ?: "1.0").trim()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Build the bundled LAME (libmp3lame) for MP3 export — see src/main/jni
        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    externalNativeBuild {
        ndkBuild {
            path = file("src/main/jni/Android.mk")
        }
    }

    signingConfigs {
        create("release") {
            if (releaseStorePath != null) {
                storeFile = file(releaseStorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: keystoreProps.getProperty("storePassword")
                keyAlias = System.getenv("KEY_ALIAS") ?: keystoreProps.getProperty("keyAlias")
                keyPassword = System.getenv("KEY_PASSWORD") ?: keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Use a real release keystore when configured (CI secret or local keystore.properties);
            // otherwise fall back to the debug key so local/sideload builds still work.
            signingConfig = if (releaseStorePath != null) signingConfigs.getByName("release")
                            else signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    packaging {
        // youtubedl-android unzips its bundled Python/yt-dlp .so payload from disk at init,
        // so the native libs must be extracted on install rather than loaded from the APK.
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation("com.github.k2-fsa:sherpa-onnx:v1.13.3")
    implementation(libs.youtubedl.android.library)
    // NOTE: youtubedl-android-ffmpeg intentionally omitted — we download a single
    // pre-muxed audio stream (no merge/transcode) and export MP3 via bundled LAME,
    // so yt-dlp never needs ffmpeg. Saves ~34 MB. Re-add if a feature needs muxing.
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.transformer)
    implementation(libs.androidx.media3.common)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}