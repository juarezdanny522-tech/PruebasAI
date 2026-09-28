package com.pruebasai.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.pruebasai.app.data.DownloadStatus
import com.pruebasai.app.data.ModelCatalog
import com.pruebasai.app.data.ModelEntry

@Composable
fun ModelsScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val localModels by viewModel.modelManager.localModels.collectAsState()
    val downloadStatus by viewModel.modelManager.downloadStatus.collectAsState()
    val engineState by viewModel.engineState.collectAsState()
    val selectedFile by viewModel.selectedModelFile.collectAsState()

    var customUrl by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ---- Progreso de descarga global ----
        if (downloadStatus is DownloadStatus.InProgress) {
            item {
                DownloadProgressCard(
                    status = downloadStatus as DownloadStatus.InProgress,
                    onCancel = { viewModel.cancelDownload() },
                )
            }
        }
        if (downloadStatus is DownloadStatus.Failed) {
            item {
                val failed = downloadStatus as DownloadStatus.Failed
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                ) {
                    Text(
                        "❌ Error descargando ${failed.fileName}: ${failed.reason}",
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }

        // ---- Modelos descargados ----
        item {
            Text("En tu dispositivo", style = MaterialTheme.typography.titleMedium)
        }
        if (localModels.isEmpty()) {
            item {
                Text(
                    "Todavía no has descargado ningún modelo. Elige uno de abajo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(localModels) { model ->
            val isActive = engineState is EngineUiState.Ready &&
                (engineState as EngineUiState.Ready).modelName == model.fileName
            val isSelected = selectedFile == model.fileName
            LocalModelCard(
                name = model.fileName,
                size = ModelCatalog.formatSize(model.sizeBytes),
                active = isActive,
                loading = isSelected && engineState is EngineUiState.Loading,
                onLoad = { viewModel.loadModel(model) },
                onDelete = { viewModel.deleteModel(model) },
            )
        }

        // ---- Catálogo ----
        item {
            Spacer(Modifier.height(8.dp))
            Text("Catálogo de modelos", style = MaterialTheme.typography.titleMedium)
            Text(
                "Se descargan desde Hugging Face. Una vez descargados funcionan sin internet.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(ModelCatalog.models) { entry ->
            val already = localModels.any { it.fileName == entry.fileName }
            val downloading = downloadStatus is DownloadStatus.InProgress &&
                (downloadStatus as DownloadStatus.InProgress).fileName == entry.fileName
            CatalogCard(
                entry = entry,
                alreadyDownloaded = already,
                downloading = downloading,
                anyDownloadActive = downloadStatus is DownloadStatus.InProgress,
                onDownload = { viewModel.downloadCatalogEntry(entry.id) },
            )
        }

        // ---- URL personalizada ----
        item {
            Spacer(Modifier.height(8.dp))
            Text("Descargar desde una URL", style = MaterialTheme.typography.titleMedium)
            Text(
                "Pega la dirección directa de cualquier archivo .litertlm o .task de Hugging Face.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = customUrl,
                onValueChange = { customUrl = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("https://huggingface.co/.../archivo.litertlm") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                leadingIcon = { Icon(Icons.Filled.Link, null) },
            )
            Spacer(Modifier.height(8.dp))
            val fileName = ModelCatalog.fileNameFromUrl(customUrl)
            val canDownload = fileName != null && downloadStatus !is DownloadStatus.InProgress
            Row {
                Button(
                    onClick = {
                        fileName?.let { viewModel.downloadModel(customUrl.trim(), it) }
                    },
                    enabled = canDownload,
                ) {
                    Icon(Icons.Filled.CloudDownload, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Descargar")
                }
            }
            if (customUrl.isNotBlank() && fileName == null) {
                Text(
                    "⚠️ La URL debe terminar en .litertlm o .task",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun DownloadProgressCard(status: DownloadStatus.InProgress, onCancel: () -> Unit) {
    Card {
        Column(Modifier.padding(16.dp)) {
            Text("Descargando ${status.fileName}", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { status.fraction },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${(status.fraction * 100).toInt()}% · " +
                        "${ModelCatalog.formatSize(status.bytes)} de ${ModelCatalog.formatSize(status.total)}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(onClick = onCancel) { Text("Cancelar") }
            }
        }
    }
}

@Composable
private fun LocalModelCard(
    name: String,
    size: String,
    active: Boolean,
    loading: Boolean,
    onLoad: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (active) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (active) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                }
                Text(size, style = MaterialTheme.typography.bodySmall)
            }
            if (loading) {
                Text("Cargando…", style = MaterialTheme.typography.bodySmall)
            } else if (!active) {
                Button(onClick = onLoad) { Text("Cargar") }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Eliminar")
            }
        }
    }
}

@Composable
private fun CatalogCard(
    entry: ModelEntry,
    alreadyDownloaded: Boolean,
    downloading: Boolean,
    anyDownloadActive: Boolean,
    onDownload: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(entry.name, fontWeight = FontWeight.SemiBold)
                    if (entry.badge != null) {
                        AssistChip(
                            onClick = {},
                            label = { Text(entry.badge) },
                        )
                    }
                }
                Text(
                    ModelCatalog.formatSize(entry.sizeBytes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                entry.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            when {
                alreadyDownloaded -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Ya descargado", style = MaterialTheme.typography.bodySmall)
                    }
                }
                downloading -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text("Descargando…", style = MaterialTheme.typography.bodySmall)
                }
                else -> {
                    Button(onClick = onDownload, enabled = !anyDownloadActive) {
                        Icon(Icons.Filled.CloudDownload, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Descargar")
                    }
                }
            }
        }
    }
}
