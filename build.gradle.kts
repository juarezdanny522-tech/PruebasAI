// Configuración a nivel de proyecto. Los plugins se declaran aquí pero no se aplican.
plugins {
    id("com.android.application") version "9.4.1" apply false
    // Kotlin va integrado en AGP 9 (built-in Kotlin). Solo se aplica el plugin del
    // compilador de Compose, en la misma versión que Kotlin.
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.20" apply false
}
