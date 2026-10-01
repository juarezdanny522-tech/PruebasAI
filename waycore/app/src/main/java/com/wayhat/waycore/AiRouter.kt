package com.wayhat.waycore

import android.content.Context
import com.wayhat.waycore.ai.LocalAiEngine
import com.wayhat.waycore.ai.ModelManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Decide con qué cerebro responde Karbys: el modelo local (LiteRT-LM) o Gemini.
 * Modos: auto (local primero, Gemini de respaldo), local (solo modelo local),
 * gemini (nube).
 */
object AiRouter {

    const val MODE_AUTO = "auto"
    const val MODE_LOCAL = "local"
    const val MODE_CLOUD = "gemini"

    private const val PREFS = "waycore_ai"
    private const val KEY_MODE = "ai_mode"
    private const val KEY_MODEL = "selected_model"

    fun getMode(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_MODE, MODE_AUTO) ?: MODE_AUTO

    fun setMode(context: Context, mode: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_MODE, mode).apply()
    }

    fun selectedModelName(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_MODEL, null)

    fun selectModel(context: Context, fileName: String?) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_MODEL, fileName).apply()
    }

    /** Carga el modelo seleccionado si aún no está en memoria. */
    suspend fun ensureEngineReady(context: Context): Boolean {
        if (LocalAiEngine.isReady) return true
        val name = selectedModelName(context) ?: return false
        ModelManager.init(context)
        val model = ModelManager.localModels.value.firstOrNull { it.fileName == name } ?: return false
        LocalAiEngine.load(model.file)
        return LocalAiEngine.isReady
    }

    /** Precarga el motor en segundo plano para que la respuesta sea rápida. */
    fun warmUp(context: Context) {
        ModelManager.init(context)
        if (selectedModelName(context) != null && !LocalAiEngine.isReady) {
            GlobalScope.launch(Dispatchers.IO) {
                try { ensureEngineReady(context) } catch (_: Throwable) {}
            }
        }
    }

    suspend fun ask(context: Context, user: String, memory: List<ConversationTurn>, deviceContext: String): String {
        val mode = getMode(context)
        if (mode == MODE_CLOUD) {
            return GeminiClient.ask(ApiKeyStore.get(context), user, memory, deviceContext)
        }

        val localReady = try {
            ensureEngineReady(context)
        } catch (_: Throwable) {
            false
        }

        if (localReady) {
            val prompt = KarbysPrompt.build(user, memory, deviceContext)
            val local = try {
                LocalAiEngine.ask(user, prompt)
            } catch (t: Throwable) {
                "El modelo local tuvo un problema: ${t.message ?: t}"
            }
            if (mode == MODE_LOCAL) return local
            // modo auto: si el modelo local falló, cae a Gemini
            if (!local.startsWith("El modelo local tuvo un problema")) return local
        } else if (mode == MODE_LOCAL) {
            return "El modelo local no está listo. Descarga un modelo en la sección Cerebro de Karbys."
        }

        // auto sin modelo local (o con fallo local): Gemini
        return GeminiClient.ask(ApiKeyStore.get(context), user, memory, deviceContext)
    }
}
