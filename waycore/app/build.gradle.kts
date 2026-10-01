import java.util.Properties

plugins {
    id("com.android.application") version "9.4.1"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.20"
}

val localProps = Properties()
val localFile = rootProject.file("local.properties")
if (localFile.exists()) {
    localFile.inputStream().use { input ->
        localProps.load(input)
    }
}

val geminiKeyRaw = providers.gradleProperty("GEMINI_API_KEY").orNull
    ?: localProps.getProperty("GEMINI_API_KEY").orEmpty()

// Mantiene BuildConfig válido aunque la clave haya sido pegada con comillas o saltos de línea.
val geminiKey = geminiKeyRaw.trim()
    .removeSurrounding("\"")
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")

android {
    namespace = "com.wayhat.waycore"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.wayhat.waycore"
        minSdk = 26
        targetSdk = 35
        versionCode = 8
        versionName = "0.7.0"
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiKey\"")
    }

    signingConfigs {
        create("release") {
            val keystoreFile = rootProject.file("keystore/dev-release.p12")
            val props = java.util.Properties().apply {
                val f = rootProject.file("keystore/keystore.properties")
                if (f.exists()) f.inputStream().use { load(it) }
            }
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = props.getProperty("storePassword", "pruebasai-dev-2026")
                keyAlias = props.getProperty("keyAlias", "pruebasai")
                keyPassword = props.getProperty("keyPassword", "pruebasai-dev-2026")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            val rel = signingConfigs.getByName("release")
            signingConfig = if (rel.storeFile != null) rel else signingConfigs.getByName("debug")
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        jniLibs.useLegacyPackaging = true
    }

    androidResources {
        noCompress += "litertlm"
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "x86_64")
            isUniversalApk = false
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.json:json:20240303")

    // ── IA local (Google AI Edge / LiteRT-LM) ──
    implementation("com.google.ai.edge.litertlm:litertlm-android:0.17.1")
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.4.20")
}
