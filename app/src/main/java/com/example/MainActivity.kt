package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardVoice
import androidx.compose.material.icons.filled.ModeComment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.KeyboardVoice
import androidx.compose.material.icons.outlined.ModeComment
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AssistantUiState
import com.example.ui.AuraScreen
import com.example.ui.AuraViewModel
import com.example.ui.screens.AssistantHomeScreen
import com.example.ui.screens.CustomCommandsScreen
import com.example.ui.screens.DownloadAppScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VoiceDictationScreen
import com.example.ui.screens.VoiceNotesScreen
import com.example.ui.theme.AuraTheme
import com.example.ui.theme.ChatGptBackground
import com.example.ui.theme.ChatGptBorder
import com.example.ui.theme.ChatGptGreen
import com.example.ui.theme.ChatGptSurface
import com.example.ui.theme.ChatGptTextMuted
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.CyberDarkSurface

class MainActivity : ComponentActivity() {

    private val viewModel: AuraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val logs by viewModel.logs.collectAsStateWithLifecycle()
            val commands by viewModel.commands.collectAsStateWithLifecycle()
            val notes by viewModel.notes.collectAsStateWithLifecycle()

            AuraTheme(themeAccent = uiState.themeAccent) {
                AuraAppContent(
                    uiState = uiState,
                    viewModel = viewModel,
                    logs = logs,
                    commands = commands,
                    notes = notes
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuraAppContent(
    uiState: AssistantUiState,
    viewModel: AuraViewModel,
    logs: List<com.example.model.VoiceLog>,
    commands: List<com.example.model.CustomCommand>,
    notes: List<com.example.model.VoiceNote>
) {
    val primaryColor = Color(uiState.themeAccent.primaryHex)
    val isFullscreenVoice = uiState.isAdvancedVoiceMode && uiState.currentScreen == AuraScreen.ASSISTANT_HUD

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ChatGptBackground,
        bottomBar = {
            if (!isFullscreenVoice) {
                AuraBottomNav(
                    currentScreen = uiState.currentScreen,
                    onScreenSelected = { viewModel.setScreen(it) },
                    primaryColor = primaryColor
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isFullscreenVoice) androidx.compose.foundation.layout.PaddingValues(0.dp) else innerPadding)
                .background(ChatGptBackground)
        ) {
            AnimatedContent(
                targetState = uiState.currentScreen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screen_transition"
            ) { screen ->
                when (screen) {
                    AuraScreen.ASSISTANT_HUD -> AssistantHomeScreen(
                        uiState = uiState,
                        viewModel = viewModel,
                        recentLogs = logs
                    )
                    AuraScreen.VOICE_TYPING -> VoiceDictationScreen(
                        uiState = uiState,
                        viewModel = viewModel
                    )
                    AuraScreen.CUSTOM_COMMANDS -> CustomCommandsScreen(
                        uiState = uiState,
                        viewModel = viewModel,
                        commands = commands
                    )
                    AuraScreen.VOICE_NOTES -> VoiceNotesScreen(
                        uiState = uiState,
                        viewModel = viewModel,
                        notes = notes
                    )
                    AuraScreen.DOWNLOAD_APP -> DownloadAppScreen(
                        uiState = uiState,
                        viewModel = viewModel
                    )
                    AuraScreen.SETTINGS -> SettingsScreen(
                        uiState = uiState,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

data class NavItem(
    val screen: AuraScreen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

@Composable
fun AuraBottomNav(
    currentScreen: AuraScreen,
    onScreenSelected: (AuraScreen) -> Unit,
    primaryColor: Color
) {
    val items = listOf(
        NavItem(AuraScreen.ASSISTANT_HUD, "Chat", Icons.Filled.ModeComment, Icons.Outlined.ModeComment, "nav_hud"),
        NavItem(AuraScreen.VOICE_TYPING, "Ditado", Icons.Filled.KeyboardVoice, Icons.Outlined.KeyboardVoice, "nav_typing"),
        NavItem(AuraScreen.CUSTOM_COMMANDS, "Comandos", Icons.Filled.AutoFixHigh, Icons.Outlined.AutoFixHigh, "nav_commands"),
        NavItem(AuraScreen.DOWNLOAD_APP, "Baixar App", Icons.Filled.Download, Icons.Outlined.Download, "nav_download"),
        NavItem(AuraScreen.SETTINGS, "Ajustes", Icons.Filled.Settings, Icons.Outlined.Settings, "nav_settings")
    )

    NavigationBar(
        containerColor = ChatGptSurface,
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, ChatGptBorder))
    ) {
        items.forEach { item ->
            val isSelected = currentScreen == item.screen
            NavigationBarItem(
                selected = isSelected,
                onClick = { onScreenSelected(item.screen) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = Color.White,
                    unselectedIconColor = ChatGptTextMuted,
                    unselectedTextColor = ChatGptTextMuted,
                    indicatorColor = Color(0xFF2F2F2F)
                ),
                modifier = Modifier.testTag(item.tag)
            )
        }
    }
}
