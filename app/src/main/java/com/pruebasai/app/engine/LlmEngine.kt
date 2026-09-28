package com.pruebasai.app.engine

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Parámetros de muestreo para la generación. */
data class SamplerParams(
    val temperature: Double = 0.7,
    val topP: Double = 0.95,
    val topK: Int = 40,
)

/** Mensaje de la conversación (usuario/asistente) para reconstruir el contexto. */
data class Turn(val isUser: Boolean, val text: String)

/**
 * Envoltorio Kotlin sobre LiteRT-LM (motor de IA local de Google).
 *
 * El modelo se ejecuta 100% en el dispositivo: no se envía nada a ningún servidor.
 * La generación llega por streaming (trozo a trozo) a través de [MessageCallback].
 */
class LlmEngine {

    @Volatile
    private var engine: Engine? = null

    @Volatile
    private var conversation: Conversation? = null

    /** Config con la que se creó la conversación actual (para saber cuándo recrearla). */
    private var conversationSignature: String? = null

    /** Config con la que se cargó el motor actual. */
    private var loadedConfig: LoadedConfig? = null

    data class LoadedConfig(
        val modelPath: String,
        val contextSize: Int,
        val cpuThreads: Int,
        val useGpu: Boolean,
    )

    val isLoaded: Boolean
        get() = engine?.isInitialized() == true

    /**
     * Carga un modelo. Puede tardar varios segundos: llamar siempre desde una
     * corrutina de fondo.
     */
    suspend fun load(
        modelPath: String,
        contextSize: Int,
        cpuThreads: Int,
        useGpu: Boolean,
        cacheDir: File,
    ) = withContext(Dispatchers.IO) {
        unload()
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val config = EngineConfig(
            modelPath = modelPath,
            backend = if (useGpu) Backend.GPU() else Backend.CPU(threadCount = cpuThreads),
            maxNumTokens = contextSize,
            cacheDir = cacheDir.absolutePath,
        )
        val e = Engine(config)
        e.initialize()
        engine = e
        loadedConfig = LoadedConfig(modelPath, contextSize, cpuThreads, useGpu)
    }

    /**
     * Genera una respuesta en streaming. La conversación interna del motor mantiene
     * el historial; [history] solo se usa si hay que recrear la conversación.
     */
    fun generate(
        userMessage: String,
        history: List<Turn>,
        systemPrompt: String,
        sampler: SamplerParams,
        maxOutputTokens: Int,
        onDelta: (text: String, thinking: String) -> Unit,
        onDone: () -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        val e = engine
        if (e == null || !e.isInitialized()) {
            onError(IllegalStateException("El modelo no está cargado"))
            return
        }
        try {
            val signature = "$systemPrompt|${sampler.temperature}|${sampler.topP}|${sampler.topK}"
            val conv = if (conversation == null || conversationSignature != signature) {
                conversation?.close()
                e.createConversation(
                    ConversationConfig(
                        systemInstruction = if (systemPrompt.isNotBlank()) Contents.of(systemPrompt) else null,
                        initialMessages = history.map { turn ->
                            if (turn.isUser) Message.user(turn.text) else Message.model(turn.text)
                        },
                        samplerConfig = SamplerConfig(
                            topK = sampler.topK.coerceAtLeast(1),
                            topP = sampler.topP.coerceIn(0.0, 1.0),
                            temperature = sampler.temperature.coerceAtLeast(0.0),
                        ),
                        maxOutputToken = maxOutputTokens,
                    )
                ).also { conversationSignature = signature }
            } else {
                conversation!!
            }

            conv.sendMessageAsync(
                userMessage,
                object : MessageCallback {
                    override fun onMessage(message: Message) {
                        val text = message.toString()
                        val thinking = message.channels.entries.joinToString("\n") { it.value }
                        if (text.isNotEmpty() || thinking.isNotEmpty()) {
                            onDelta(text, thinking)
                        }
                    }

                    override fun onDone() = onDone()

                    override fun onError(throwable: Throwable) = onError(throwable)
                },
            )
        } catch (t: Throwable) {
            onError(t)
        }
    }

    /** Detiene la generación en curso (si la hay). */
    fun stop() {
        try {
            conversation?.cancelProcess()
        } catch (_: Throwable) {
        }
    }

    /** Vacía la conversación pero mantiene el modelo cargado. */
    fun newChat() {
        try {
            conversation?.close()
        } catch (_: Throwable) {
        }
        conversation = null
        conversationSignature = null
    }

    /** Número de tokens usados en la conversación actual. */
    fun tokenCount(): Int = try {
        conversation?.getTokenCount() ?: 0
    } catch (_: Throwable) {
        0
    }

    /** Libera todo (motor y conversación). */
    fun unload() {
        try {
            conversation?.close()
        } catch (_: Throwable) {
        }
        conversation = null
        conversationSignature = null
        try {
            engine?.close()
        } catch (_: Throwable) {
        }
        engine = null
        loadedConfig = null
    }
}
