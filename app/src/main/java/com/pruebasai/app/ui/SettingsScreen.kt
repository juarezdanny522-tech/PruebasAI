package com.pruebasai.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val settings by viewModel.settings.collectAsState()
    val engineState by viewModel.engineState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Ajustes", style = MaterialTheme.typography.titleLarge)

        // ---- Instrucción del sistema ----
        SectionTitle("Instrucción del sistema")
        Text(
            "Define la personalidad del asistente. Se aplica en el siguiente mensaje.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = settings.systemPrompt,
            onValueChange = { viewModel.updateSettings { s -> s.copy(systemPrompt = it) } },
            modifier = Modifier.fillMaxWidth().height(120.dp),
        )

        // ---- Muestreo ----
        SectionTitle("Generación")

        SliderRow(
            label = "Temperatura",
            value = settings.temperature.toFloat(),
            range = 0f..2f,
            display = "%.2f".format(settings.temperature),
        ) { viewModel.updateSettings { s -> s.copy(temperature = it.toDouble()) } }

        SliderRow(
            label = "Top-P",
            value = settings.topP.toFloat(),
            range = 0f..1f,
            display = "%.2f".format(settings.topP),
        ) { viewModel.updateSettings { s -> s.copy(topP = it.toDouble()) } }

        SliderRow(
            label = "Top-K",
            value = settings.topK.toFloat(),
            range = 1f..100f,
            display = "${settings.topK}",
        ) { viewModel.updateSettings { s -> s.copy(topK = it.roundToInt()) } }

        SliderRow(
            label = "Máx. tokens de respuesta",
            value = settings.maxOutputTokens.toFloat(),
            range = 64f..4096f,
            display = "${settings.maxOutputTokens}",
        ) { viewModel.updateSettings { s -> s.copy(maxOutputTokens = (it.roundToInt() / 64) * 64) } }

        // ---- Motor ----
        SectionTitle("Motor de inferencia")
        if (engineState is EngineUiState.Ready) {
            Text(
                "El modelo ya está cargado: los cambios de abajo se aplicarán al volver a cargarlo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }

        SliderRow(
            label = "Longitud de contexto",
            value = settings.contextSize.toFloat(),
            range = 1024f..32768f,
            display = "${settings.contextSize}",
        ) { viewModel.updateSettings { s -> s.copy(contextSize = (it.roundToInt() / 256) * 256) } }

        SliderRow(
            label = "Hilos de CPU",
            value = settings.cpuThreads.toFloat(),
            range = 1f..8f,
            display = "${settings.cpuThreads}",
        ) { viewModel.updateSettings { s -> s.copy(cpuThreads = it.roundToInt()) } }

        RowSwitch(
            label = "Aceleración GPU",
            description = "Más rápido en GPUs compatibles (OpenCL). Si falla al cargar, desactívalo.",
            checked = settings.useGpu,
        ) { viewModel.updateSettings { s -> s.copy(useGpu = it) } }

        RowSwitch(
            label = "Mostrar pensamiento",
            description = "Muestra el razonamiento interno del modelo cuando existe (canal «thinking»).",
            checked = settings.showThinking,
        ) { viewModel.updateSettings { s -> s.copy(showThinking = it) } }

        // ---- Acciones ----
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { viewModel.resetSettings() }) {
                Text("Restablecer ajustes")
            }
            Button(onClick = { viewModel.clearChat() }) {
                Text("Vaciar chat")
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "PruebasAI · IA 100% local · LiteRT-LM de Google",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    display: String,
    onChange: (Float) -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text(display, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}

@Composable
private fun RowSwitch(
    label: String,
    description: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
