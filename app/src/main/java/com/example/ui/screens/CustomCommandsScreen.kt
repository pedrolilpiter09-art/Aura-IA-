package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CustomCommand
import com.example.ui.AssistantUiState
import com.example.ui.AuraViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomCommandsScreen(
    uiState: AssistantUiState,
    viewModel: AuraViewModel,
    commands: List<CustomCommand>,
    modifier: Modifier = Modifier
) {
    val primaryColor = Color(uiState.themeAccent.primaryHex)
    val secondaryColor = Color(uiState.themeAccent.secondaryHex)

    var showAddDialog by remember { mutableStateOf(false) }
    var triggerText by remember { mutableStateOf("") }
    var actionType by remember { mutableStateOf("PLAY_STORE") }
    var payloadText by remember { mutableStateOf("") }
    var replyText by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val actionTypes = listOf(
        "PLAY_STORE" to "Baixar App na Play Store",
        "OPEN_APP" to "Abrir Aplicativo do Sistema",
        "WEB_SEARCH" to "Pesquisar na Web",
        "FLASHLIGHT_ON" to "Ligar Lanterna",
        "FLASHLIGHT_OFF" to "Desligar Lanterna",
        "ALARM" to "Definir Alarme",
        "TEXT_RESPONSE" to "Resposta Personalizada por Voz"
    )

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "AUTOMAÇÕES & COMANDOS",
                        color = primaryColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Gatilhos de linguagem natural personalizados",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }

                Surface(
                    color = Color(0xFF131D31),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF233048))
                ) {
                    Text(
                        text = "${commands.size} ativos",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (commands.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nenhum comando personalizado criado ainda.\nToque no botão '+' abaixo para adicionar!",
                        color = Color(0xFF64748B),
                        fontSize = 14.sp,
                        lineHeight = 22.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(commands) { cmd ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp)),
                            color = Color(0xFF0C1424),
                            border = BorderStroke(
                                1.dp,
                                if (cmd.isEnabled) primaryColor.copy(alpha = 0.35f) else Color(0xFF1E293B)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Action Icon
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            viewModel.processUserQuery(cmd.triggerPhrase)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (cmd.actionType) {
                                            "PLAY_STORE" -> Icons.Default.ShoppingBag
                                            "OPEN_APP" -> Icons.Default.Apps
                                            "WEB_SEARCH" -> Icons.Default.Search
                                            "FLASHLIGHT_ON", "FLASHLIGHT_OFF" -> Icons.Default.FlashlightOn
                                            "ALARM" -> Icons.Default.Alarm
                                            else -> Icons.Default.Bolt
                                        },
                                        contentDescription = "Executar comando",
                                        tint = if (cmd.isEnabled) primaryColor else Color.Gray,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "\"${cmd.triggerPhrase}\"",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${cmd.actionType}: ${cmd.actionPayload.ifBlank { cmd.assistantReply }}",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }

                                Switch(
                                    checked = cmd.isEnabled,
                                    onCheckedChange = { viewModel.toggleCommand(cmd) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = primaryColor,
                                        checkedTrackColor = primaryColor.copy(alpha = 0.3f),
                                        uncheckedThumbColor = Color.Gray,
                                        uncheckedTrackColor = Color(0xFF1E293B)
                                    )
                                )

                                IconButton(
                                    onClick = { viewModel.deleteCommand(cmd) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Excluir",
                                        tint = Color(0xFFFF5252),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_custom_command_fab"),
            containerColor = primaryColor,
            contentColor = Color.Black
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Adicionar Comando")
        }

        // Add Command Dialog
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                containerColor = Color(0xFF0C1424),
                titleContentColor = Color.White,
                title = {
                    Text("Novo Comando Personalizado", fontWeight = FontWeight.Bold)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = triggerText,
                            onValueChange = { triggerText = it },
                            label = { Text("Quando eu disser (frase de voz)") },
                            placeholder = { Text("Ex: Baixar Instagram") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color(0xFF2E3D56),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        ExposedDropdownMenuBox(
                            expanded = dropdownExpanded,
                            onExpandedChange = { dropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = actionTypes.firstOrNull { it.first == actionType }?.second ?: actionType,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Ação do Sistema") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryColor,
                                    unfocusedBorderColor = Color(0xFF2E3D56),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false },
                                modifier = Modifier.background(Color(0xFF0F1B2E))
                            ) {
                                actionTypes.forEach { (typeKey, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label, color = Color.White) },
                                        onClick = {
                                            actionType = typeKey
                                            dropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        if (actionType != "FLASHLIGHT_ON" && actionType != "FLASHLIGHT_OFF") {
                            OutlinedTextField(
                                value = payloadText,
                                onValueChange = { payloadText = it },
                                label = { Text("Parâmetro da Ação (App / Busca / Hora)") },
                                placeholder = { Text("Ex: com.instagram.android ou 07:30") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryColor,
                                    unfocusedBorderColor = Color(0xFF2E3D56),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                        }

                        OutlinedTextField(
                            value = replyText,
                            onValueChange = { replyText = it },
                            label = { Text("Resposta falada pelo Assistente") },
                            placeholder = { Text("Ex: Executando seu comando, mestre.") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color(0xFF2E3D56),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (triggerText.isNotBlank()) {
                                viewModel.addCustomCommand(
                                    trigger = triggerText.trim(),
                                    actionType = actionType,
                                    payload = payloadText.trim(),
                                    reply = replyText.trim()
                                )
                                triggerText = ""
                                payloadText = ""
                                replyText = ""
                                showAddDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = Color.Black)
                    ) {
                        Text("Criar Comando", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("Cancelar", color = Color.Gray)
                    }
                }
            )
        }
    }
}
