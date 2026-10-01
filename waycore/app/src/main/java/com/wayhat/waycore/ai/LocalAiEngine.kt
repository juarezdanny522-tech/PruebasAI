package com.wayhat.waycore.ai

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.LlmContentType
import com.google.ai.edge.litertlm.MessageCallback
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.ToolConfig
import com.google.ai.edge.litertlm.tool
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Motor de IA local de Karbys basado en LiteRT-LM (Google AI Edge).
 * Ejecuta modelos .litertlm dentro del teléfono: sin internet, sin nube.
 */
object LocalAiEngine {

    sealed class State {
        data object NoModel : State()
        data object Loading : State()
        data object Ready : State()
        data class Error(val message: String) : State()
    }

    private val _state = MutableStateFlow<State>(State.NoModel)
    val state: StateFlow<State> = _state

    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private var loadedFile: File? = null

    val isReady: Boolean get() = _state.value is State.Ready

    /** Carga un modelo .litertlm y prepara la conversación con las herramientas de WayHat. */
    @Synchronized
    suspend fun load(modelFile: File) {
        if (loadedFile == modelFile && isReady) return
        close()
        _state.value = State.Loading
        try {
            val config = EngineConfig(
                modelPath = modelFile.absolutePath,
                backend = Backend.CPU(threadCount = 4),
                maxNumTokens = 4096,
                cacheDir = modelFile.parentFile ?: File("/data/local/tmp")
            )
            val eng = Engine(config)
            eng.initialize()
            engine = eng
            loadedFile = modelFile
            newConversation()
            _state.value = State.Ready
        } catch (t: Throwable) {
            close()
            _state.value = State.Error(t.message ?: "Error cargando el modelo local")
        }
    }

    /** Nueva conversación (llamar antes de cada pregunta para inyectar contexto fresco). */
    fun newConversation() {
        val eng = engine ?: return
        conversation = eng.createConversation(
            ConversationConfig(
                maxOutputToken = 512,
                samplerConfig = SamplerConfig(temperature = 0.7f, topK = 40, topP = 0.95f),
                tools = listOf(tool(KarbysTools())),
                toolConfig = ToolConfig(automaticToolCalling = true)
            )
        )
    }

    /** Envía un mensaje al modelo y suspende hasta la respuesta completa. */
    suspend fun ask(userMessage: String, systemPrompt: String): String {
        val conv = conversation ?: return "El modelo local no está cargado."
        val fullMessage = systemPrompt

        return suspendCancellableCoroutine { cont ->
            val acc = StringBuilder()
            // El system prompt viaja como primer mensaje del usuario (instrucción de sistema).
            conv.sendMessageAsync(fullMessage, object : MessageCallback {
                override fun onMessage(message: LlmContentType) {
                    acc.append(message.toString())
                }
                override fun onDone() {
                    if (cont.isActive) {
                        val answer = acc.toString().trim()
                        // El eco del system prompt no debe llegar a la voz: usamos la respuesta
                        // generada después de la última marca del mensaje real del usuario.
                        val cleaned = if (answer.contains(userMessage)) {
                            answer.substringAfterLast(userMessage).trim()
                        } else answer
                        // Nueva conversación para la próxima pregunta con contexto fresco.
                        newConversation()
                        cont.resume(cleaned.ifBlank { "No obtuve respuesta del modelo local." })
                    }
                }
                override fun onError(code: Int, message: String) {
                    if (cont.isActive) {
                        newConversation()
                        cont.resume("El modelo local tuvo un problema ($code): $message")
                    }
                }
            })
        }
    }

    @Synchronized
    fun close() {
        try { conversation?.cancelProcess() } catch (_: Throwable) {}
        try { conversation?.close() } catch (_: Throwable) {}
        try { engine?.close() } catch (_: Throwable) {}
        conversation = null
        engine = null
        loadedFile = null
    }
}
