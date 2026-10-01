package com.wayhat.waycore.ai

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import com.google.ai.edge.litertlm.SamplerConfig
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

    /** Carga un modelo .litertlm y prepara el motor. */
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
                cacheDir = (modelFile.parentFile ?: File("/data/local/tmp")).absolutePath
            )
            val eng = Engine(config)
            eng.initialize()
            engine = eng
            loadedFile = modelFile
            newConversation("")
            _state.value = State.Ready
        } catch (t: Throwable) {
            close()
            _state.value = State.Error(t.message ?: "Error cargando el modelo local")
        }
    }

    /**
     * Nueva conversación con el system prompt de Karbys (personalidad + contexto
     * fresco del dispositivo) y las herramientas seguras de WayHat.
     */
    fun newConversation(systemPrompt: String) {
        val eng = engine ?: return
        try { conversation?.close() } catch (_: Throwable) {}
        conversation = eng.createConversation(
            ConversationConfig(
                systemInstruction = Contents.of(systemPrompt),
                maxOutputToken = 512,
                samplerConfig = SamplerConfig(topK = 40, topP = 0.95, temperature = 0.7),
                tools = listOf(tool(KarbysTools())),
                automaticToolCalling = true
            )
        )
    }

    /** Envía un mensaje al modelo y suspende hasta la respuesta completa. */
    suspend fun ask(userMessage: String, systemPrompt: String): String {
        if (engine == null) return "El modelo local no está cargado."
        // Conversación fresca por pregunta: así el contexto de sensores siempre está al día.
        newConversation(systemPrompt)
        val conv = conversation ?: return "El modelo local no está cargado."

        return suspendCancellableCoroutine { cont ->
            val acc = StringBuilder()
            conv.sendMessageAsync(userMessage, object : MessageCallback {
                override fun onMessage(message: Message) {
                    acc.append(message.toString())
                }
                override fun onDone() {
                    if (cont.isActive) {
                        val answer = acc.toString().trim()
                        cont.resume(answer.ifBlank { "No obtuve respuesta del modelo local." })
                    }
                }
                override fun onError(throwable: Throwable) {
                    if (cont.isActive) {
                        cont.resume("El modelo local tuvo un problema: ${throwable.message ?: throwable}")
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
