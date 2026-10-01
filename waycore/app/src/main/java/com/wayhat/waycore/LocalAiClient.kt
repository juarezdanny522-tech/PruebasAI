package com.wayhat.waycore

/**
 * Cliente de IA 100% local (LiteRT-LM / Google AI Edge) para Karbys.
 * Responde dentro del teléfono, sin internet ni clave API.
 */
object LocalAiClient {

    suspend fun ask(user: String, memory: List<ConversationTurn>, deviceContext: String): String {
        if (user.isBlank()) return "No escuché ninguna pregunta."
        val systemPrompt = KarbysPrompt.build(user, memory, deviceContext)
        return try {
            com.wayhat.waycore.ai.LocalAiEngine.ask(user, systemPrompt)
        } catch (t: Throwable) {
            "El modelo local tuvo un problema: ${t.message ?: t}"
        }
    }
}
