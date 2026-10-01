package com.wayhat.waycore.ai

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Descarga modelos .litertlm con el DownloadManager del sistema (sigue en
 * segundo plano) y guarda el progreso observable por la interfaz.
 */
object ModelManager {

    data class DownloadState(
        val file: String? = null,          // fileName en curso
        val fraction: Float = 0f,          // 0..1
        val done: Boolean = false,
        val error: String? = null
    )

    data class LocalModel(val fileName: String, val file: File, val sizeMb: Long)

    private val _progress = MutableStateFlow(DownloadState())
    val progress: StateFlow<DownloadState> = _progress

    private val _localModels = MutableStateFlow<List<LocalModel>>(emptyList())
    val localModels: StateFlow<List<LocalModel>> = _localModels

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastRefresh = 0L

    fun modelsDir(context: Context): File =
        File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir, "models")
            .also { it.mkdirs() }

    fun init(context: Context) {
        refreshLocal(context)
    }

    fun refreshLocal(context: Context) {
        val dir = modelsDir(context)
        val list = dir.listFiles { f -> f.isFile && f.name.endsWith(".litertlm") && f.length() > 1_000_000 }
            ?.map { LocalModel(it.name, it, it.length() / (1024 * 1024)) }
            ?.sortedBy { it.fileName }
            ?: emptyList()
        _localModels.value = list
        lastRefresh = System.currentTimeMillis()
    }

    /** Lanza (o reanuda) la descarga de [model] y sigue su progreso. */
    fun download(context: Context, model: ModelCatalog) {
        if (_progress.file != null && !_progress.done) return
        _progress.value = DownloadState(file = model.fileName, fraction = 0f)
        val target = File(modelsDir(context), model.fileName)
        if (target.exists()) target.delete()

        val request = DownloadManager.Request(Uri.parse(model.url))
            .setTitle("Karbys: ${model.label}")
            .setDescription("Descargando modelo de IA local (${model.size})")
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "models/${model.fileName}")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val id = dm.enqueue(request)

        scope.launch {
            while (true) {
                delay(700)
                val q = DownloadManager.Query().setFilterById(id)
                val cursor: Cursor = dm.query(q)
                var status = 0
                var bytes = 0L
                var total = -1L
                cursor.use {
                    if (it.moveToFirst()) {
                        val colStatus = it.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        val colSoFar = it.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                        val colTotal = it.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                        status = it.getInt(colStatus)
                        bytes = it.getLong(colSoFar)
                        total = it.getLong(colTotal)
                    }
                }
                when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        refreshLocal(context)
                        _progress.value = DownloadState(file = null, fraction = 1f, done = true)
                        break
                    }
                    DownloadManager.STATUS_FAILED -> {
                        try { target.delete() } catch (_: Throwable) {}
                        _progress.value = DownloadState(
                            file = null, done = true,
                            error = "La descarga falló. Revisa tu conexión e inténtalo otra vez."
                        )
                        break
                    }
                    else -> {
                        val frac = if (total > 0) bytes.toFloat() / total.toFloat() else 0f
                        _progress.value = DownloadState(file = model.fileName, fraction = frac)
                        if (System.currentTimeMillis() - lastRefresh > 4000) refreshLocal(context)
                    }
                }
            }
        }
    }

    fun clearError() {
        if (_progress.error != null) _progress.value = DownloadState()
    }
}
