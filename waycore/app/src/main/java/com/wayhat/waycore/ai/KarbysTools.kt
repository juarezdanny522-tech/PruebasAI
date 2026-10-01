package com.wayhat.waycore.ai

import com.google.ai.edge.litertlm.Tool
import com.google.ai.edge.litertlm.ToolParam
import com.google.ai.edge.litertlm.ToolSet
import com.wayhat.waycore.WayHatService
import kotlinx.coroutines.runBlocking
import org.json.JSONObject

/**
 * Herramientas que el modelo local puede ejecutar en WayHat.
 * Mismas 5 acciones seguras que el function calling de Gemini: cada una pasa por
 * la lista blanca de [WayHatService.executeTool] y el ESP32 la confirma.
 */
class KarbysTools : ToolSet {

    @Tool(description = "Cambia la sensibilidad de detección de obstáculos de WayHat. Rango permitido: 20 a 150 centímetros.")
    fun setWayhatSensitivity(
        @ToolParam(description = "Nueva sensibilidad en centímetros, entre 20 y 150.") centimeters: Int
    ): String = runBlocking {
        WayHatService.executeTool("set_wayhat_sensitivity", JSONObject().put("centimeters", centimeters))
    }

    @Tool(description = "Cambia el modo de WayHat: SAFE (seguridad) o CHARLA (chat).")
    fun setWayhatMode(
        @ToolParam(description = "Modo deseado: safe o chat.") mode: String
    ): String = runBlocking {
        WayHatService.executeTool("set_wayhat_mode", JSONObject().put("mode", mode))
    }

    @Tool(description = "Activa o desactiva los avisos sonoros de proximidad (buzzer).")
    fun setWayhatAlerts(
        @ToolParam(description = "true para activar los avisos, false para silenciarlos.") enabled: Boolean
    ): String = runBlocking {
        WayHatService.executeTool("set_wayhat_alerts", JSONObject().put("enabled", enabled))
    }

    @Tool(description = "Hace una prueba del buzzer de WayHat: emite un pitido de verificación.")
    fun testWayhatAlert(): String = runBlocking {
        WayHatService.executeTool("test_wayhat_alert", JSONObject())
    }

    @Tool(description = "Solicita al ESP32 una lectura inmediata de sus sensores antes de responder cuando el usuario pide datos actuales.")
    fun refreshWayhatTelemetry(): String = runBlocking {
        WayHatService.executeTool("refresh_wayhat_telemetry", JSONObject())
    }
}
