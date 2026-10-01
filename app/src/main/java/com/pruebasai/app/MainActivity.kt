package com.pruebasai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pruebasai.app.ui.AppTab
import com.pruebasai.app.ui.ChatScreen
import com.pruebasai.app.ui.MainViewModel
import com.pruebasai.app.ui.ModelsScreen
import com.pruebasai.app.ui.SettingsScreen
import com.pruebasai.app.ui.theme.PruebasAiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PruebasAiTheme {
                PruebasAiApp()
            }
        }
    }
}

private data class TabItem(val tab: AppTab, val label: String, val icon: ImageVector)

@Composable
fun PruebasAiApp(viewModel: MainViewModel = viewModel()) {
    val activeTab by viewModel.activeTab.collectAsState()

    val tabs = listOf(
        TabItem(AppTab.CHAT, "Chat", Icons.Filled.Chat),
        TabItem(AppTab.MODELS, "Modelos", Icons.Filled.SmartToy),
        TabItem(AppTab.SETTINGS, "Ajustes", Icons.Filled.Settings),
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                tabs.forEach { item ->
                    NavigationBarItem(
                        selected = activeTab == item.tab,
                        onClick = { viewModel.selectTab(item.tab) },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                    )
                }
            }
        },
    ) { padding ->
        when (activeTab) {
            AppTab.CHAT -> ChatScreen(viewModel, Modifier.padding(padding))
            AppTab.MODELS -> ModelsScreen(viewModel, Modifier.padding(padding))
            AppTab.SETTINGS -> SettingsScreen(viewModel, Modifier.padding(padding))
        }
    }
}
