package com.pruebasai.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pruebasai.app.data.AppSettings
import com.pruebasai.app.data.DownloadStatus
import com.pruebasai.app.data.LocalModel
import com.pruebasai.app.data.ModelCatalog
import com.pruebasai.app.data.ModelManager
import com.pruebasai.app.data.SettingsRepository
import com.pruebasai.app.engine.LlmEngine
import com.pruebasai.app.engine.SamplerParams
import com.pruebasai.app.engine.Turn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Un mensaje del chat. */
data class ChatMessage(
    val isUser: Boolean,
    val text: String,
    val thinking: String = "",
    val streaming: Boolean = false,
)

/** Estado del motor de IA. */
sealed class EngineUiState {
    data object NoModel : EngineUiState()
    data class Loading(val modelName: String) : EngineUiState()
    data class Ready(val modelName: String) : EngineUiState()
    data class Error(val message: String) : EngineUiState()
}

/** Pestañas de la aplicación. */
enum class AppTab { CHAT, MODELS, SETTINGS }

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepo = SettingsRepository(application)
    val modelManager = ModelManager(application)
    private val engine = LlmEngine()

    val settings: StateFlow<AppSettings> = settingsRepo.settings

    private val _messages = MutableStateFlow(loadSavedChat())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _engineState = MutableStateFlow<EngineUiState>(EngineUiState.NoModel)
    val engineState: StateFlow<EngineUiState> = _engineState.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _tokensPerSecond = MutableStateFlow(0.0)
    val tokensPerSecond: StateFlow<Double> = _tokensPerSecond.asStateFlow()

    private val _tokenCount = MutableStateFlow(0)
    val tokenCount: StateFlow<Int> = _tokenCount.asStateFlow()

    private val _activeTab = MutableStateFlow(AppTab.CHAT)
    val activeTab: StateFlow<AppTab> = _activeTab.asStateFlow()

    private val _selectedModelFile = MutableStateFlow<String?>(null)
    val selectedModelFile: StateFlow<String?> = _selectedModelFile.asStateFlow()

    private var generateJob: Job? = null
    private var genStartTime = 0L
    private var genDeltas = 0

    init {
        viewModelScope.launch {
            modelManager.resumeTrackingOnStart()
            modelManager.pollUntilDone()
        }
    }

    // ---------- Navegación ----------

    fun selectTab(tab: AppTab) {
        _activeTab.value = tab
    }

    // ---------- Motor / chat ----------

    /** Carga un modelo en el motor. */
    fun loadModel(model: LocalModel) {
        if (_engineState.value is EngineUiState.Loading) return
        _engineState.value = EngineUiState.Loading(model.fileName)
        _selectedModelFile.value = model.fileName
        viewModelScope.launch {
            try {
                val app = getApplication<Application>()
                engine.load(
                    modelPath = model.file.absolutePath,
                    contextSize = settings.value.contextSize,
                    cpuThreads = settings.value.cpuThreads,
                    useGpu = settings.value.useGpu,
                    cacheDir = File(app.filesDir, "litert_cache"),
                )
                _engineState.value = EngineUiState.Ready(model.fileName)
                _tokenCount.value = engine.tokenCount()
            } catch (t: Throwable) {
                _engineState.value = EngineUiState.Error(
                    t.message ?: "No se pudo cargar el modelo. Prueba con otro modelo o reinicia la app."
                )
            }
        }
    }

    /** Envía un mensaje y genera la respuesta en streaming. */
    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _isGenerating.value) return
        if (_engineState.value !is EngineUiState.Ready) return

        // El historial previo sirve para recrear la conversación si hace falta.
        val history = _messages.value.map { Turn(it.isUser, it.text) }

        _messages.update { it + ChatMessage(isUser = true, text = trimmed) }
        _messages.update { it + ChatMessage(isUser = false, text = "", streaming = true) }
        saveChat()
        _isGenerating.value = true
        genStartTime = System.currentTimeMillis()
        genDeltas = 0
        _tokensPerSecond.value = 0.0

        val s = settings.value
        generateJob = viewModelScope.launch {
            engine.generate(
                userMessage = trimmed,
                history = history,
                systemPrompt = s.systemPrompt,
                sampler = SamplerParams(s.temperature, s.topP, s.topK),
                maxOutputTokens = s.maxOutputTokens,
                onDelta = { delta, thinking ->
                    genDeltas++
                    val elapsed = (System.currentTimeMillis() - genStartTime) / 1000.0
                    if (elapsed > 0.3) _tokensPerSecond.value = genDeltas / elapsed
                    _messages.update { list ->
                        if (list.isEmpty()) list else {
                            val last = list.last()
                            list.dropLast(1) + last.copy(
                                text = last.text + delta,
                                thinking = if (thinking.isNotEmpty()) {
                                    (last.thinking + thinking).trim()
                                } else last.thinking,
                            )
                        }
                    }
                },
                onDone = {
                    _isGenerating.value = false
                    _tokenCount.value = engine.tokenCount()
                    finalizeLastMessage()
                },
                onError = { error ->
                    _isGenerating.value = false
                    val cancelled = error is kotlinx.coroutines.CancellationException
                    if (cancelled) {
                        finalizeLastMessage()
                    } else {
                        _messages.update { list ->
                            if (list.isEmpty()) list else {
                                val last = list.last()
                                val errText = when {
                                    last.text.isNotBlank() -> last.text
                                    else -> "⚠️ Error: ${error.message ?: "fallo al generar"}"
                                }
                                list.dropLast(1) + last.copy(text = errText, streaming = false)
                            }
                        }
                        saveChat()
                    }
                },
            )
        }
    }

    private fun finalizeLastMessage() {
        _messages.update { list ->
            if (list.isEmpty()) list
            else list.dropLast(1) + list.last().copy(streaming = false)
        }
        saveChat()
    }

    /** Detiene la generación en curso. */
    fun stopGeneration() {
        engine.stop()
    }

    /** Vacía el chat y reinicia la conversación del motor. */
    fun clearChat() {
        if (_isGenerating.value) {
            engine.stop()
            _isGenerating.value = false
        }
        engine.newChat()
        _messages.value = emptyList()
        _tokenCount.value = 0
        _tokensPerSecond.value = 0.0
        saveChat()
    }

    // ---------- Descargas ----------

    fun downloadModel(url: String, fileName: String) {
        viewModelScope.launch {
            val ok = modelManager.startDownloadAsync(url, fileName)
            if (ok) modelManager.pollUntilDone()
        }
    }

    fun downloadCatalogEntry(id: String) {
        val entry = ModelCatalog.models.firstOrNull { it.id == id } ?: return
        downloadModel(entry.url, entry.fileName)
    }

    fun cancelDownload() = modelManager.cancelDownload()

    fun deleteModel(model: LocalModel) {
        if (model.fileName == _selectedModelFile.value) {
            engine.unload()
            _engineState.value = EngineUiState.NoModel
            _selectedModelFile.value = null
        }
        modelManager.deleteModel(model)
    }

    // ---------- Ajustes ----------

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        settingsRepo.update(transform)
    }

    fun resetSettings() = settingsRepo.reset()

    // ---------- Persistencia del chat ----------

    private fun chatFile(): File = File(getApplication<Application>().filesDir, "chat_history.json")

    private fun saveChat() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val arr = JSONArray()
                for (m in _messages.value) {
                    arr.put(JSONObject().apply {
                        put("u", m.isUser)
                        put("t", m.text)
                        put("th", m.thinking)
                    })
                }
                chatFile().writeText(JSONObject().put("messages", arr).toString())
            } catch (_: Throwable) {
            }
        }
    }

    private fun loadSavedChat(): List<ChatMessage> {
        return try {
            val f = File(getApplication<Application>().filesDir, "chat_history.json")
            if (!f.exists()) return emptyList()
            val arr = JSONObject(f.readText()).getJSONArray("messages")
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        ChatMessage(
                            isUser = o.optBoolean("u", false),
                            text = o.optString("t", ""),
                            thinking = o.optString("th", ""),
                        )
                    )
                }
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    override fun onCleared() {
        super.onCleared()
        engine.unload()
    }
}
