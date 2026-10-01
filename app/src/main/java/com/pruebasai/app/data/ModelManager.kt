package com.pruebasai.app.data

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

/** Un modelo ya descargado y disponible en el dispositivo. */
data class LocalModel(
    val fileName: String,
    val file: File,
    val sizeBytes: Long,
)

/** Estado de la descarga en curso. */
sealed class DownloadStatus {
    data object Idle : DownloadStatus()
    data class InProgress(
        val fileName: String,
        val bytes: Long,
        val total: Long,
    ) : DownloadStatus() {
        val fraction: Float
            get() = if (total > 0) (bytes.toFloat() / total).coerceIn(0f, 1f) else 0f
    }

    data class Completed(val fileName: String) : DownloadStatus()
    data class Failed(val fileName: String, val reason: String) : DownloadStatus()
    data class Cancelled(val fileName: String) : DownloadStatus()
}

/**
 * Gestiona la descarga de modelos con [DownloadManager] (sigue descargando aunque
 * la app pase a segundo plano y reanuda cortes de red) y los modelos locales.
 */
class ModelManager(private val context: Context) {

    private val downloadManager =
        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    private val _localModels = MutableStateFlow(scanLocalModels())
    val localModels: StateFlow<List<LocalModel>> = _localModels.asStateFlow()

    private val _downloadStatus = MutableStateFlow<DownloadStatus>(DownloadStatus.Idle)
    val downloadStatus: StateFlow<DownloadStatus> = _downloadStatus.asStateFlow()

    private var activeDownloadId: Long = -1L
    private var activeFileName: String = ""

    /** Directorio donde viven los modelos (almacenamiento externo de la app, sin permisos). */
    fun modelsDir(): File {
        val dir = context.getExternalFilesDir("models") ?: File(context.filesDir, "models")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun scanLocalModels(): List<LocalModel> {
        val dirs = listOfNotNull(
            context.getExternalFilesDir("models"),
            File(context.filesDir, "models").takeIf { it.exists() },
        )
        return dirs
            .flatMap { dir -> dir.listFiles()?.toList().orEmpty() }
            .filter { it.isFile && it.length() > 0 && (it.name.endsWith(".litertlm") || it.name.endsWith(".task")) }
            .map { LocalModel(it.name, it, it.length()) }
            .sortedBy { it.fileName }
    }

    fun refreshLocalModels() {
        _localModels.value = scanLocalModels()
    }

    /** Encola una descarga sin bloquear el hilo principal. Devuelve true si se encoló. */
    suspend fun startDownloadAsync(url: String, fileName: String): Boolean =
        withContext(Dispatchers.IO) {
            // setDestinationInExternalFilesDir debe llamarse con el nombre directo del
            // subdirectorio relativo al almacenamiento externo de la app.
            if (activeDownloadId != -1L) return@withContext false
            val target = File(modelsDir(), fileName)
            if (target.exists()) target.delete()

            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle(fileName)
                .setDescription("Descargando modelo de IA local")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationInExternalFilesDir(context, "models", fileName)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(false)
                .setMimeType("application/octet-stream")

            activeDownloadId = downloadManager.enqueue(request)
            activeFileName = fileName
            _downloadStatus.value = DownloadStatus.InProgress(fileName, 0, 0)
            true
        }

    fun cancelDownload() {
        val id = activeDownloadId
        if (id != -1L) {
            downloadManager.remove(id)
            val name = activeFileName
            activeDownloadId = -1L
            activeFileName = ""
            _downloadStatus.value = DownloadStatus.Cancelled(name)
        }
    }

    /**
     * Consulta el progreso de la descarga activa hasta que termine (éxito, fallo o
     * cancelación). Se llama desde una corrutina de la UI.
     */
    suspend fun pollUntilDone() {
        val id = activeDownloadId
        if (id == -1L) return
        while (activeDownloadId != -1L) {
            val status = queryDownload(id)
            when (status) {
                is DownloadStatus.InProgress -> {
                    _downloadStatus.value = status
                    delay(500)
                }
                is DownloadStatus.Completed -> {
                    activeDownloadId = -1L
                    activeFileName = ""
                    _downloadStatus.value = status
                    refreshLocalModels()
                }
                is DownloadStatus.Failed -> {
                    activeDownloadId = -1L
                    activeFileName = ""
                    _downloadStatus.value = status
                }
                else -> {
                    activeDownloadId = -1L
                    delay(500)
                }
            }
        }
    }

    private fun queryDownload(id: Long): DownloadStatus {
        val query = DownloadManager.Query().setFilterById(id)
        var cursor: Cursor? = null
        return try {
            cursor = downloadManager.query(query)
            if (!cursor.moveToFirst()) {
                DownloadStatus.Failed(activeFileName, "La descarga ya no existe")
            } else {
                val bytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                when (cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))) {
                    DownloadManager.STATUS_SUCCESSFUL -> DownloadStatus.Completed(activeFileName)
                    DownloadManager.STATUS_FAILED -> {
                        val reasonIdx = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON)
                        val reason = downloadReason(cursor.getInt(reasonIdx))
                        DownloadStatus.Failed(activeFileName, reason)
                    }
                    else -> DownloadStatus.InProgress(activeFileName, bytes, total)
                }
            }
        } catch (t: Throwable) {
            DownloadStatus.Failed(activeFileName, t.message ?: "Error desconocido")
        } finally {
            cursor?.close()
        }
    }

    fun deleteModel(model: LocalModel): Boolean {
        if (model.fileName == activeFileName) cancelDownload()
        return model.file.delete().also { refreshLocalModels() }
    }

    /** Si hay una descarga pendiente al arrancar la app, la retoma. */
    fun resumeTrackingOnStart() {
        // Busca descargas en curso del DownloadManager que pertenezcan a esta app.
        val query = DownloadManager.Query()
        downloadManager.query(query).use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_ID)
            val titleIdx = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TITLE)
            val statusIdx = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
            while (cursor.moveToNext()) {
                if (cursor.getInt(statusIdx) == DownloadManager.STATUS_PENDING ||
                    cursor.getInt(statusIdx) == DownloadManager.STATUS_RUNNING
                ) {
                    activeDownloadId = cursor.getLong(idIdx)
                    activeFileName = cursor.getString(titleIdx) ?: ""
                    _downloadStatus.value = DownloadStatus.InProgress(activeFileName, 0, 0)
                    return
                }
            }
        }
    }

    private fun downloadReason(code: Int): String = when (code) {
        DownloadManager.ERROR_INSUFFICIENT_SPACE -> "Espacio insuficiente en el dispositivo"
        DownloadManager.ERROR_DEVICE_NOT_FOUND -> "Almacenamiento no disponible"
        DownloadManager.ERROR_CANNOT_RESUME -> "No se pudo reanudar la descarga"
        DownloadManager.ERROR_FILE_ERROR -> "Error de archivo"
        DownloadManager.ERROR_HTTP_DATA_ERROR -> "Error de red al descargar"
        DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "Error HTTP del servidor"
        DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "El archivo ya existe"
        else -> "Error de descarga (código $code)"
    }
}
