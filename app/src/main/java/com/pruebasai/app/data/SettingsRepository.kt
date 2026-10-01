package com.pruebasai.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Ajustes de la aplicación, persistidos en SharedPreferences. */
data class AppSettings(
    val systemPrompt: String = DEFAULT_SYSTEM_PROMPT,
    val temperature: Double = 0.7,
    val topP: Double = 0.95,
    val topK: Int = 40,
    val maxOutputTokens: Int = 1024,
    val contextSize: Int = 4096,
    val cpuThreads: Int = 4,
    val useGpu: Boolean = false,
    val showThinking: Boolean = true,
) {
    companion object {
        const val DEFAULT_SYSTEM_PROMPT =
            "Eres un asistente de IA útil, amable y preciso. Respondes siempre en español " +
                "de forma clara y concisa salvo que te pidan otro idioma."
    }
}

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("pruebasai_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun load() = AppSettings(
        systemPrompt = prefs.getString(KEY_SYSTEM_PROMPT, AppSettings.DEFAULT_SYSTEM_PROMPT)
            ?: AppSettings.DEFAULT_SYSTEM_PROMPT,
        temperature = prefs.getFloat(KEY_TEMPERATURE, 0.7f).toDouble(),
        topP = prefs.getFloat(KEY_TOP_P, 0.95f).toDouble(),
        topK = prefs.getInt(KEY_TOP_K, 40),
        maxOutputTokens = prefs.getInt(KEY_MAX_OUT, 1024),
        contextSize = prefs.getInt(KEY_CONTEXT, 4096),
        cpuThreads = prefs.getInt(KEY_THREADS, 4),
        useGpu = prefs.getBoolean(KEY_GPU, false),
        showThinking = prefs.getBoolean(KEY_THINKING, true),
    )

    fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(_settings.value)
        _settings.value = next
        prefs.edit()
            .putString(KEY_SYSTEM_PROMPT, next.systemPrompt)
            .putFloat(KEY_TEMPERATURE, next.temperature.toFloat())
            .putFloat(KEY_TOP_P, next.topP.toFloat())
            .putInt(KEY_TOP_K, next.topK)
            .putInt(KEY_MAX_OUT, next.maxOutputTokens)
            .putInt(KEY_CONTEXT, next.contextSize)
            .putInt(KEY_THREADS, next.cpuThreads)
            .putBoolean(KEY_GPU, next.useGpu)
            .putBoolean(KEY_THINKING, next.showThinking)
            .apply()
    }

    fun reset() = update { AppSettings() }

    private companion object {
        const val KEY_SYSTEM_PROMPT = "system_prompt"
        const val KEY_TEMPERATURE = "temperature"
        const val KEY_TOP_P = "top_p"
        const val KEY_TOP_K = "top_k"
        const val KEY_MAX_OUT = "max_output"
        const val KEY_CONTEXT = "context"
        const val KEY_THREADS = "threads"
        const val KEY_GPU = "gpu"
        const val KEY_THINKING = "thinking"
    }
}
