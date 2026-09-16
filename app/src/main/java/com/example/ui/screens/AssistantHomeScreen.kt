package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.ModeComment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.VoiceGender
import com.example.model.DialogueSender
import com.example.model.DialogueTurn
import com.example.model.VoiceLog
import com.example.ui.AssistantUiState
import com.example.ui.AuraScreen
import com.example.ui.AuraViewModel
import com.example.ui.components.AudioWaveformBar
import com.example.ui.components.HolographicOrb
import com.example.ui.theme.ChatGptBackground
import com.example.ui.theme.ChatGptBorder
import com.example.ui.theme.ChatGptGreen
import com.example.ui.theme.ChatGptInput
import com.example.ui.theme.ChatGptSurface
import com.example.ui.theme.ChatGptTextMuted
import com.example.ui.theme.ChatGptTextPrimary
import com.example.ui.theme.ChatGptUserBubble

@Composable
fun AssistantHomeScreen(
    uiState: AssistantUiState,
    viewModel: AuraViewModel,
    recentLogs: List<VoiceLog>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        if (granted) {
            viewModel.toggleListening()
        }
    }

    AnimatedContent(
        targetState = uiState.isAdvancedVoiceMode,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "chatgpt_voice_transition",
        modifier = modifier.fillMaxSize()
    ) { isVoiceMode ->
        if (isVoiceMode) {
            // ==========================================
            // CHATGPT ADVANCED VOICE MODE (FULLSCREEN CALL)
            // ==========================================
            ChatGptAdvancedVoiceView(
                uiState = uiState,
                viewModel = viewModel,
                hasPermission = hasAudioPermission,
                onRequestPermission = {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            )
        } else {
            // ==========================================
            // CHATGPT MAIN CONVERSATION CHAT VIEW
            // ==========================================
            ChatGptConversationChatView(
                uiState = uiState,
                viewModel = viewModel,
                hasPermission = hasAudioPermission,
                onRequestPermission = {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            )
        }
    }
}

// =========================================================================
// 1. CHATGPT MAIN CONVERSATIONAL CHAT INTERFACE
// =========================================================================
@Composable
fun ChatGptConversationChatView(
    uiState: AssistantUiState,
    viewModel: AuraViewModel,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    var showModelMenu by remember { mutableStateOf(false) }
    var showPlusToolsMenu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Auto-scroll on new message
    LaunchedEffect(uiState.conversationHistory.size) {
        if (uiState.conversationHistory.isNotEmpty()) {
            listState.animateScrollToItem(uiState.conversationHistory.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChatGptBackground)
            .imePadding()
    ) {
        // --- Top Header (ChatGPT Style) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Model Selector Pill (ChatGPT 4o)
            Box {
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { showModelMenu = true },
                    color = ChatGptSurface,
                    border = BorderStroke(1.dp, ChatGptBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Aura 4o",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Selecionar Modelo",
                            tint = ChatGptTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = showModelMenu,
                    onDismissRequest = { showModelMenu = false },
                    modifier = Modifier.background(ChatGptSurface)
                ) {
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text("Aura 4o (Recomendado)", color = Color.White, fontWeight = FontWeight.Bold)
                                Text("Voz realista e inteligência avançada", color = ChatGptTextMuted, fontSize = 12.sp)
                            }
                        },
                        onClick = { showModelMenu = false }
                    )
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text("Aura Offline", color = Color.White, fontWeight = FontWeight.Bold)
                                Text("Execução rápida 100% no aparelho", color = ChatGptTextMuted, fontSize = 12.sp)
                            }
                        },
                        onClick = {
                            viewModel.toggleOfflineMode(!uiState.isOfflineMode)
                            showModelMenu = false
                        }
                    )
                }
            }

            // Right Action Buttons: Voice Gender switch & New Chat
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Voice Gender Switch Pill
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            val next = if (uiState.voiceGender == VoiceGender.FEMALE) VoiceGender.MALE else VoiceGender.FEMALE
                            viewModel.updateVoiceGender(next)
                        },
                    color = ChatGptSurface,
                    border = BorderStroke(1.dp, ChatGptBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (uiState.voiceGender == VoiceGender.FEMALE) "👩 Voz Feminina" else "👨 Voz Masculina",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // New Chat Button
                IconButton(
                    onClick = { viewModel.clearDialogue() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = "Nova Conversa",
                        tint = ChatGptTextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // --- Chat Stream Feed ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (uiState.conversationHistory.isEmpty()) {
                // ChatGPT Empty State Welcome Screen
                ChatGptEmptyState(
                    assistantName = uiState.assistantName,
                    onPromptSelected = { prompt ->
                        viewModel.processUserQuery(prompt)
                    },
                    onOpenVoice = {
                        if (hasPermission) {
                            viewModel.setAdvancedVoiceMode(true)
                        } else {
                            onRequestPermission()
                        }
                    }
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 10.dp, bottom = 20.dp)
                ) {
                    items(uiState.conversationHistory) { turn ->
                        ChatGptMessageBubble(
                            turn = turn,
                            assistantName = uiState.assistantName,
                            voiceGender = uiState.voiceGender,
                            onSpeak = { viewModel.speakTurn(turn.text) },
                            onCopy = { viewModel.copyDictationText() }
                        )
                    }

                    // Live Speech Indicator when user is talking
                    if (uiState.isListening && uiState.liveTranscript.isNotBlank()) {
                        item {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp)),
                                color = ChatGptSurface,
                                border = BorderStroke(1.dp, ChatGptGreen.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(ChatGptGreen)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "\"${uiState.liveTranscript}\"",
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    // Assistant Processing Indicator
                    if (uiState.isProcessing) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.size(24.dp),
                                    shape = CircleShape,
                                    color = ChatGptGreen.copy(alpha = 0.2f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = ChatGptGreen,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Aura está pensando...",
                                    color = ChatGptTextMuted,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- Bottom ChatGPT Floating Capsule Input Bar ---
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            shape = RoundedCornerShape(30.dp),
            color = ChatGptInput,
            border = BorderStroke(1.dp, ChatGptBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Plus '+' Tools Action Button
                Box {
                    IconButton(
                        onClick = { showPlusToolsMenu = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Ferramentas",
                            tint = ChatGptTextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showPlusToolsMenu,
                        onDismissRequest = { showPlusToolsMenu = false },
                        modifier = Modifier.background(ChatGptSurface)
                    ) {
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(Icons.Default.FlashlightOn, contentDescription = null, tint = ChatGptGreen)
                            },
                            text = { Text("Ligar Lanterna", color = Color.White) },
                            onClick = {
                                viewModel.processUserQuery("ligar lanterna")
                                showPlusToolsMenu = false
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = ChatGptGreen)
                            },
                            text = { Text("Baixar WhatsApp na Play Store", color = Color.White) },
                            onClick = {
                                viewModel.processUserQuery("baixar whatsapp na play store")
                                showPlusToolsMenu = false
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(Icons.Default.Download, contentDescription = null, tint = ChatGptGreen)
                            },
                            text = { Text("Baixar App no Celular", color = Color.White) },
                            onClick = {
                                viewModel.setScreen(AuraScreen.DOWNLOAD_APP)
                                showPlusToolsMenu = false
                            }
                        )
                    }
                }

                // Text Input Field
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            text = "Mensagem para Aura...",
                            color = ChatGptTextMuted,
                            fontSize = 15.sp
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("command_input_field"),
                    shape = RoundedCornerShape(26.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = ChatGptGreen
                    ),
                    maxLines = 4
                )

                // Right Actions: Send button OR Dictation & ChatGPT Voice Mode Launcher
                if (inputText.isNotBlank()) {
                    // Send Button (ChatGPT White Pill Circle)
                    Surface(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .clickable {
                                val q = inputText
                                inputText = ""
                                viewModel.processUserQuery(q)
                            }
                            .testTag("send_query_button"),
                        color = Color.White,
                        shape = CircleShape
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Enviar Mensagem",
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Microphone Dictation
                        IconButton(
                            onClick = {
                                if (hasPermission) {
                                    viewModel.toggleListening()
                                } else {
                                    onRequestPermission()
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (uiState.isListening) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Falar por microfone",
                                tint = if (uiState.isListening) Color(0xFFFF3366) else ChatGptTextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Iconic ChatGPT Voice Mode Button (Headphones / Sound Wave Circle)
                        Surface(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable {
                                    if (hasPermission) {
                                        viewModel.setAdvancedVoiceMode(true)
                                    } else {
                                        onRequestPermission()
                                    }
                                }
                                .testTag("chatgpt_voice_mode_launcher"),
                            color = Color.White,
                            shape = CircleShape
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = "Modo Voz Avançado ChatGPT",
                                    tint = Color.Black,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// 2. CHATGPT MESSAGE BUBBLES
// =========================================================================
@Composable
fun ChatGptMessageBubble(
    turn: DialogueTurn,
    assistantName: String,
    voiceGender: VoiceGender,
    onSpeak: () -> Unit,
    onCopy: () -> Unit
) {
    val isUser = turn.sender == DialogueSender.USER

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        if (isUser) {
            // User Bubble (Clean Charcoal Pill on Right)
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .clip(RoundedCornerShape(20.dp)),
                color = ChatGptUserBubble
            ) {
                Text(
                    text = turn.text,
                    color = ChatGptTextPrimary,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        } else {
            // Assistant (ChatGPT) Message stream on Left
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // OpenAI / Aura Emblem
                Surface(
                    modifier = Modifier
                        .size(30.dp)
                        .padding(top = 2.dp),
                    shape = CircleShape,
                    color = ChatGptGreen.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, ChatGptGreen.copy(alpha = 0.5f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = ChatGptGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = turn.text,
                        color = ChatGptTextPrimary,
                        fontSize = 15.sp,
                        lineHeight = 23.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // ChatGPT Message Action Bar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = onSpeak,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Ouvir resposta",
                                tint = ChatGptTextMuted,
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        IconButton(
                            onClick = onCopy,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copiar",
                                tint = ChatGptTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Text(
                            text = "Voz ${voiceGender.displayName}",
                            color = ChatGptTextMuted.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// 3. CHATGPT EMPTY STATE (WELCOME SCREEN)
// =========================================================================
@Composable
fun ChatGptEmptyState(
    assistantName: String,
    onPromptSelected: (String) -> Unit,
    onOpenVoice: () -> Unit
) {
    val prompts = listOf(
        "🎙️ Conversar por voz naturalmente",
        "💡 Ligar a lanterna ou alarme",
        "📲 Baixar o aplicativo no celular",
        "🛍️ Pesquisar apps na Play Store"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Centered Glowing ChatGPT Emblem
        Surface(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .clickable { onOpenVoice() },
            color = ChatGptGreen.copy(alpha = 0.15f),
            border = BorderStroke(1.5.dp, ChatGptGreen.copy(alpha = 0.6f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = ChatGptGreen,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Como posso ajudar você hoje?",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Converse por texto ou toque no botão de fone para o Modo Voz",
            color = ChatGptTextMuted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Quick Suggestion Cards
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            prompts.forEach { prompt ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            val clean = prompt.replace(Regex("^[\\p{So}\\p{Sk}\\p{Sm}\\p{Sc}]+\\s*"), "")
                            onPromptSelected(clean)
                        },
                    color = ChatGptSurface,
                    border = BorderStroke(1.dp, ChatGptBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = prompt,
                            color = Color(0xFFECECEC),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// 4. CHATGPT ADVANCED VOICE MODE (FULLSCREEN VOICE CALL)
// =========================================================================
@Composable
fun ChatGptAdvancedVoiceView(
    uiState: AssistantUiState,
    viewModel: AuraViewModel,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit
) {
    val primaryColor = Color(uiState.themeAccent.primaryHex)
    val secondaryColor = Color(uiState.themeAccent.secondaryHex)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // --- Top Bar ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Voice Model Badge
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF242424),
                border = BorderStroke(1.dp, Color(0xFF383838))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(ChatGptGreen)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Aura Voz • ${uiState.voiceGender.displayName}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Close / Switch to Text View
            IconButton(
                onClick = { viewModel.setAdvancedVoiceMode(false) },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Fechar Modo Voz",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // --- Center Animated ChatGPT Morphing Voice Orb ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            HolographicOrb(
                isListening = uiState.isListening,
                isSpeaking = uiState.isSpeaking,
                isProcessing = uiState.isProcessing,
                audioAmplitude = uiState.audioAmplitude,
                primaryColor = primaryColor,
                secondaryColor = secondaryColor,
                orbSize = 250.dp,
                onClick = {
                    if (hasPermission) {
                        viewModel.toggleListening()
                    } else {
                        onRequestPermission()
                    }
                }
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Subtitle & Status Callout
            Text(
                text = when {
                    uiState.isListening -> "Ouvindo você..."
                    uiState.isSpeaking -> "Aura falando em voz..."
                    uiState.isProcessing -> "Pensando..."
                    else -> "Toque no orbe ou fale para conversar"
                },
                color = if (uiState.isListening) primaryColor else Color(0xFFB4B4B4),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Real-time Spoken Transcript Subtitle
            val subtitleText = when {
                uiState.isListening && uiState.liveTranscript.isNotBlank() -> "\"${uiState.liveTranscript}\""
                uiState.isSpeaking -> uiState.lastAssistantResponse
                else -> ""
            }

            if (subtitleText.isNotBlank()) {
                Text(
                    text = subtitleText,
                    color = Color.White,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            AudioWaveformBar(
                isActive = uiState.isListening || uiState.isSpeaking,
                amplitude = uiState.audioAmplitude,
                primaryColor = primaryColor,
                secondaryColor = secondaryColor,
                modifier = Modifier.padding(horizontal = 40.dp)
            )
        }

        // --- Bottom Controls Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Switch to Chat Transcript View
            IconButton(
                onClick = { viewModel.setAdvancedVoiceMode(false) },
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF242424))
            ) {
                Icon(
                    imageVector = Icons.Default.ModeComment,
                    contentDescription = "Ver Transcrição em Chat",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Center Mic Mute / Unmute Button
            Surface(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .clickable {
                        if (hasPermission) {
                            viewModel.toggleListening()
                        } else {
                            onRequestPermission()
                        }
                    },
                color = if (uiState.isListening) Color.White else Color(0xFF2E2E2E),
                shape = CircleShape
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (uiState.isListening) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = "Mudo / Falar",
                        tint = if (uiState.isListening) Color.Black else Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            // End Call Button (Red X Circle)
            IconButton(
                onClick = {
                    viewModel.stopSpeaking()
                    viewModel.setAdvancedVoiceMode(false)
                },
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF3B1820))
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Encerrar Modo Voz",
                    tint = Color(0xFFFF4D4D),
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}
