package com.pruebasai.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pruebasai.app.data.ModelCatalog

@Composable
fun ChatScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val messages by viewModel.messages.collectAsState()
    val engineState by viewModel.engineState.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val tokensPerSecond by viewModel.tokensPerSecond.collectAsState()
    val tokenCount by viewModel.tokenCount.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, messages.lastOrNull()?.text) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = modifier.fillMaxSize().imePadding()) {
        // ---- Barra de estado del motor ----
        EngineStatusBar(
            engineState = engineState,
            isGenerating = isGenerating,
            tokensPerSecond = tokensPerSecond,
            tokenCount = tokenCount,
            onGoToModels = { viewModel.selectTab(AppTab.MODELS) },
        )

        // ---- Mensajes ----
        if (messages.isEmpty()) {
            EmptyChatHint(Modifier.weight(1f))
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 12.dp, vertical = 8.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(messages) { msg ->
                    MessageBubble(msg, showThinking = settings.showThinking)
                }
            }
        }

        // ---- Acciones rápidas ----
        if (messages.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = { viewModel.clearChat() }) {
                    Icon(Icons.Filled.DeleteSweep, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Nuevo chat")
                }
            }
        }

        // ---- Entrada de texto ----
        Surface(tonalElevation = 2.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            when (engineState) {
                                is EngineUiState.Ready -> "Escribe tu mensaje…"
                                is EngineUiState.Loading -> "Cargando modelo…"
                                else -> "Descarga y carga un modelo para chatear"
                            }
                        )
                    },
                    maxLines = 5,
                    shape = RoundedCornerShape(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                if (isGenerating) {
                    ExtendedFloatingActionButton(
                        onClick = { viewModel.stopGeneration() },
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ) {
                        Icon(Icons.Filled.Stop, contentDescription = "Detener")
                        Spacer(Modifier.width(6.dp))
                        Text("Detener")
                    }
                } else {
                    ExtendedFloatingActionButton(
                        onClick = {
                            if (input.isNotBlank() && engineState is EngineUiState.Ready) {
                                viewModel.sendMessage(input)
                                input = ""
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar")
                        Spacer(Modifier.width(6.dp))
                        Text("Enviar")
                    }
                }
            }
        }
    }
}

@Composable
private fun EngineStatusBar(
    engineState: EngineUiState,
    isGenerating: Boolean,
    tokensPerSecond: Double,
    tokenCount: Int,
    onGoToModels: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                when (val state = engineState) {
                    is EngineUiState.NoModel -> {
                        Text("Sin modelo cargado", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Descarga un modelo para usar la IA 100% en tu móvil",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    is EngineUiState.Loading -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Cargando ${state.modelName}…", fontWeight = FontWeight.SemiBold)
                        }
                        Text(
                            "Puede tardar unos segundos",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    is EngineUiState.Ready -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(8.dp)
                                    .background(
                                        if (isGenerating) MaterialTheme.colorScheme.tertiary
                                        else MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(50),
                                    )
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                state.modelName,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        Text(
                            if (isGenerating) {
                                "Generando… %.1f tok/s".format(tokensPerSecond)
                            } else {
                                "Listo · contexto: $tokenCount tokens · 100% local"
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    is EngineUiState.Error -> {
                        Text("Error al cargar el modelo", fontWeight = FontWeight.SemiBold)
                        Text(state.message, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            when (engineState) {
                is EngineUiState.NoModel, is EngineUiState.Error -> {
                    Button(onClick = onGoToModels) { Text("Modelos") }
                }
                is EngineUiState.Loading -> {
                    LinearProgressIndicator(
                        modifier = Modifier.width(80.dp),
                    )
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun EmptyChatHint(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.Filled.NewReleases,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(12.dp))
            Text("Tu asistente de IA local", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Descarga un modelo en la pestaña «Modelos» y empieza\na chatear sin conexión y sin enviar tus datos a nadie.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage, showThinking: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start,
    ) {
        if (!message.isUser && message.thinking.isNotEmpty() && showThinking) {
            Text(
                "🧠 " + message.thinking,
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 2.dp)
                    .widthIn(max = 320.dp),
            )
        }
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 16.dp,
            ),
            color = if (message.isUser) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            modifier = Modifier.widthIn(max = 320.dp),
        ) {
            Text(
                text = message.text + if (message.streaming) " ▌" else "",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
