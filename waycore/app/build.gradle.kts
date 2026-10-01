import java.util.Properties

plugins {
    id("com.android.application")
    // Kotlin viene integrado en AGP 9 (built-in Kotlin). Solo hace falta el
    // plugin del compilador de Compose.
    id("org.jetbrains.kotlin.plugin.compose")
}

// Clave de Gemini embebida en el build (opcional). La app también permite
// pegarla en pantalla de configuración o usar la IA local sin clave.
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

// Firma de desarrollo (NO usar nunca para Play Store). Ver keystore/README.md
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore/keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val releaseKeystoreFile = rootProject.file("keystore/dev-release.p12")
val hasReleaseKeystore = releaseKeystoreFile.exists() &&
        keystoreProps.getProperty("storePassword") != null

android {
    namespace = "com.wayhat.waycore"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.wayhat.waycore"
        minSdk = 26
        targetSdk = 35
        versionCode = 10
        versionName = "0.7.2"
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiKey\"")

        // Sin filtrar ABIs: se empaquetan todos los que traigan las librerías
        // (arm64, x86_64, armeabi-v7a...) para que la app abra en cualquier teléfono.
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
            // Sin minificar (R8): máxima compatibilidad para que la app abra en
            // cualquier teléfono. Revisar después.
            isMinifyEnabled = false
            signingConfig = if (hasReleaseKeystore) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
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
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Motor de IA local (LiteRT-LM de Google) — inferencia 100% en el dispositivo
    implementation("com.google.ai.edge.litertlm:litertlm-android:0.17.1")

    // Compose UI (Material 3)
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // Activity / Lifecycle
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.3")

    // Trabajo en segundo plano y corrutinas
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // Gemini en la nube (modo opcional)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // JSON usado por LiteRT-LM y por el protocolo con el ESP32
    implementation("com.google.code.gson:gson:2.11.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
