import java.util.Properties

plugins {
    id("com.android.application")
    // Kotlin viene integrado en AGP 9 (built-in Kotlin). Solo hace falta el
    // plugin del compilador de Compose.
    id("org.jetbrains.kotlin.plugin.compose")
}

// Firma de desarrollo (NO usar nunca para Play Store). Ver keystore/README.md
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore/keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val releaseKeystoreFile = rootProject.file("keystore/dev-release.p12")
val hasReleaseKeystore = releaseKeystoreFile.exists() &&
        keystoreProps.getProperty("storePassword") != null

android {
    namespace = "com.pruebasai.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.pruebasai.app"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // arm64 para móviles reales y x86_64 para emuladores.
        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = releaseKeystoreFile
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (hasReleaseKeystore) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Motor de IA local (LiteRT-LM de Google) — inferencia 100% en el dispositivo
    implementation("com.google.ai.edge.litertlm:litertlm-android:0.17.1")

    // Compose UI
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    // La librería de iconos extendidos se congeló en 1.7.8 (última publicada)
    implementation("androidx.compose.material:material-icons-extended:1.7.8")

    // Activity / Lifecycle / ViewModel
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.3")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // JSON (usado por LiteRT-LM y por la persistencia del chat)
    implementation("com.google.code.gson:gson:2.11.0")

    // UI
    debugImplementation("androidx.compose.ui:ui-tooling")
}
