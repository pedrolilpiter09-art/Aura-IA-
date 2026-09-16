package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiAiService
import com.example.data.AssistantPreferences
import com.example.data.AuraDatabase
import com.example.data.AuraRepository
import com.example.data.ThemeAccent
import com.example.data.VoiceGender
import com.example.model.CustomCommand
import com.example.model.DialogueSender
import com.example.model.DialogueTurn
import com.example.model.VoiceLog
import com.example.model.VoiceNote
import com.example.nlu.NluResult
import com.example.nlu.OfflineIntentEngine
import com.example.speech.SpeechRecognitionManager
import com.example.speech.SpeechState
import com.example.speech.TextToSpeechManager
import com.example.system.SystemActionExecutor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AuraScreen {
    ASSISTANT_HUD,
    VOICE_TYPING,
    CUSTOM_COMMANDS,
    VOICE_NOTES,
    DOWNLOAD_APP,
    SETTINGS
}

data class AssistantUiState(
    val assistantName: String = "Aura",
    val userName: String = "Comandante",
    val currentScreen: AuraScreen = AuraScreen.ASSISTANT_HUD,
    val isAdvancedVoiceMode: Boolean = false,
    val lastUserQuery: String = "",
    val lastAssistantResponse: String = "Olá! Como posso ajudar você hoje?",
    val isListening: Boolean = false,
    val isSpeaking: Boolean = false,
    val isProcessing: Boolean = false,
    val isContinuousDialogueMode: Boolean = true,
    val conversationHistory: List<DialogueTurn> = emptyList(),
    val audioAmplitude: Float = 0f,
    val liveTranscript: String = "",
    val statusMessage: String = "ChatGPT Voice Ativo",
    val isOfflineMode: Boolean = false,
    val themeAccent: ThemeAccent = ThemeAccent.CYAN,
    val voiceGender: VoiceGender = VoiceGender.FEMALE,
    val voicePitch: Float = 1.0f,
    val voiceSpeed: Float = 1.05f,
    val autoSpeak: Boolean = true,
    val dictationText: String = "",
    val dictationWordCount: Int = 0,
    val isDictating: Boolean = false
)

class AuraViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AuraDatabase.getDatabase(application)
    private val repository = AuraRepository(db.voiceLogDao(), db.customCommandDao(), db.voiceNoteDao())
    private val prefs = AssistantPreferences(application)
    private val systemExecutor = SystemActionExecutor(application)
    private val nluEngine = OfflineIntentEngine(systemExecutor)
    private val geminiService = GeminiAiService()

    private val speechManager = SpeechRecognitionManager(application)
    private val ttsManager = TextToSpeechManager(application)

    val logs = repository.allLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val commands = repository.allCommands.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val notes = repository.allNotes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(
        AssistantUiState(
            assistantName = prefs.assistantName,
            userName = prefs.userName,
            isOfflineMode = prefs.isOfflineOnly,
            themeAccent = prefs.themeAccent,
            voiceGender = prefs.voiceGender,
            voicePitch = prefs.voicePitch,
            voiceSpeed = prefs.voiceSpeed,
            autoSpeak = prefs.autoSpeakResponses,
            isContinuousDialogueMode = true,
            conversationHistory = listOf(
                DialogueTurn(
                    sender = DialogueSender.ASSISTANT,
                    text = "Olá! Eu sou ${prefs.assistantName}. O diálogo contínuo por voz está ativado: pode falar comigo como duas pessoas conversando!"
                )
            ),
            lastAssistantResponse = "Olá! Eu sou ${prefs.assistantName}. Pode falar comigo por voz continuamente!"
        )
    )
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    init {
        ttsManager.applyVoiceGender(prefs.voiceGender)

        viewModelScope.launch {
            repository.seedInitialCommandsIfEmpty()
        }

        // Observe speech recognition state & amplitude
        viewModelScope.launch {
            combine(
                speechManager.speechState,
                speechManager.rmsAmplitude,
                speechManager.partialText,
                ttsManager.isSpeaking
            ) { speechState, amplitude, partial, isTtsSpeaking ->
                val listening = speechState == SpeechState.LISTENING
                val processing = speechState == SpeechState.PROCESSING
                val dynamicAmp = if (isTtsSpeaking) (0.3f + (System.currentTimeMillis() % 1000) / 2000f) else amplitude

                _uiState.value = _uiState.value.copy(
                    isListening = listening,
                    isProcessing = processing,
                    isSpeaking = isTtsSpeaking,
                    audioAmplitude = dynamicAmp,
                    liveTranscript = if (listening || processing) partial else _uiState.value.liveTranscript,
                    statusMessage = when {
                        listening -> "Ouvindo sua voz..."
                        processing -> "Processando comando neural..."
                        isTtsSpeaking -> "${_uiState.value.assistantName} falando (${_uiState.value.voiceGender.displayName})..."
                        else -> if (_uiState.value.isOfflineMode) "Modo Offline (Privacidade Total)" else "Núcleo IA Online & Ativo"
                    }
                )
            }.collect {}
        }
    }

    fun setScreen(screen: AuraScreen) {
        _uiState.value = _uiState.value.copy(currentScreen = screen)
    }

    fun setAdvancedVoiceMode(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isAdvancedVoiceMode = enabled)
        if (enabled) {
            startListeningForQuery()
        } else {
            ttsManager.stop()
            speechManager.stopListening()
        }
    }

    fun toggleAdvancedVoiceMode() {
        val next = !_uiState.value.isAdvancedVoiceMode
        setAdvancedVoiceMode(next)
    }

    fun stopSpeaking() {
        ttsManager.stop()
    }

    fun toggleContinuousDialogue() {
        val nextMode = !_uiState.value.isContinuousDialogueMode
        _uiState.value = _uiState.value.copy(
            isContinuousDialogueMode = nextMode,
            statusMessage = if (nextMode) "Modo Diálogo Ativo (2 Pessoas)" else "Modo Diálogo Pausado"
        )
    }

    fun clearDialogue() {
        _uiState.value = _uiState.value.copy(
            conversationHistory = listOf(
                DialogueTurn(
                    sender = DialogueSender.ASSISTANT,
                    text = "Conversa reiniciada. Pode falar comigo por voz!"
                )
            ),
            lastAssistantResponse = "Como posso ajudar agora?"
        )
    }

    fun speakTurn(text: String) {
        ttsManager.speak(
            text = text,
            pitch = _uiState.value.voicePitch,
            speed = _uiState.value.voiceSpeed,
            gender = _uiState.value.voiceGender
        )
    }

    fun toggleListening() {
        if (_uiState.value.isListening) {
            speechManager.stopListening()
        } else {
            ttsManager.stop()
            speechManager.startListening(preferOffline = _uiState.value.isOfflineMode) { resultText ->
                processUserQuery(resultText)
            }
        }
    }

    fun startListeningForQuery() {
        ttsManager.stop()
        speechManager.startListening(preferOffline = _uiState.value.isOfflineMode) { resultText ->
            processUserQuery(resultText)
        }
    }

    fun stopListening() {
        speechManager.stopListening()
    }

    fun processUserQuery(query: String) {
        if (query.isBlank()) return
        val currentName = _uiState.value.assistantName

        // Add user turn immediately to conversation history
        val userTurn = DialogueTurn(sender = DialogueSender.USER, text = query)
        val currentHistory = _uiState.value.conversationHistory + userTurn

        _uiState.value = _uiState.value.copy(
            lastUserQuery = query,
            liveTranscript = query,
            conversationHistory = currentHistory,
            isProcessing = true,
            statusMessage = "Analisando comando..."
        )

        viewModelScope.launch {
            val activeCustomCommands = repository.getActiveCommands()
            val nluResult = nluEngine.processCommand(
                rawQuery = query,
                assistantName = currentName,
                customCommands = activeCustomCommands,
                forceOffline = _uiState.value.isOfflineMode
            )

            when (nluResult) {
                is NluResult.Executed -> {
                    handleExecutionSuccess(query, nluResult.assistantResponse, nluResult.intentType, nluResult.executionDetails, nluResult.isOffline, nluResult.noteToSave)
                }
                is NluResult.NeedsOnlineAi -> {
                    val geminiResponse = geminiService.askGemini(
                        prompt = query,
                        assistantName = currentName
                    )
                    handleExecutionSuccess(query, geminiResponse, "GEMINI_AI_CHAT", "Cloud AI Reasoning", false, null)
                }
            }
        }
    }

    private suspend fun handleExecutionSuccess(
        userQuery: String,
        response: String,
        intentType: String,
        details: String,
        isOffline: Boolean,
        noteToSave: VoiceNote?
    ) {
        // Save note if returned
        noteToSave?.let { repository.insertNote(it) }

        // React to specific assistant navigation/voice mode intents
        when (intentType) {
            "CHANGE_VOICE_MALE" -> {
                prefs.voiceGender = VoiceGender.MALE
                _uiState.value = _uiState.value.copy(voiceGender = VoiceGender.MALE)
                ttsManager.applyVoiceGender(VoiceGender.MALE)
            }
            "CHANGE_VOICE_FEMALE" -> {
                prefs.voiceGender = VoiceGender.FEMALE
                _uiState.value = _uiState.value.copy(voiceGender = VoiceGender.FEMALE)
                ttsManager.applyVoiceGender(VoiceGender.FEMALE)
            }
            "NAVIGATE_DOWNLOAD_APP" -> {
                _uiState.value = _uiState.value.copy(currentScreen = AuraScreen.DOWNLOAD_APP)
            }
        }

        // Insert log in Room
        repository.insertLog(
            VoiceLog(
                userQuery = userQuery,
                assistantResponse = response,
                intentType = intentType,
                isSuccess = true,
                executionDetails = details,
                isOfflineProcessed = isOffline
            )
        )

        val assistantTurn = DialogueTurn(sender = DialogueSender.ASSISTANT, text = response)
        val updatedHistory = _uiState.value.conversationHistory + assistantTurn

        _uiState.value = _uiState.value.copy(
            lastAssistantResponse = response,
            conversationHistory = updatedHistory,
            isProcessing = false,
            statusMessage = "Aura respondendo por voz..."
        )

        if (_uiState.value.autoSpeak) {
            ttsManager.speak(
                text = response,
                pitch = _uiState.value.voicePitch,
                speed = _uiState.value.voiceSpeed,
                gender = _uiState.value.voiceGender,
                onDone = {
                    if (_uiState.value.isContinuousDialogueMode && _uiState.value.currentScreen == AuraScreen.ASSISTANT_HUD) {
                        viewModelScope.launch {
                            delay(450)
                            _uiState.value = _uiState.value.copy(statusMessage = "Sua vez de falar... 🎙️")
                            startListeningForQuery()
                        }
                    }
                }
            )
        }
    }

    // Voice Dictation Methods ("Digitar por voz")
    fun toggleDictation() {
        if (_uiState.value.isDictating) {
            speechManager.stopListening()
            _uiState.value = _uiState.value.copy(isDictating = false)
        } else {
            _uiState.value = _uiState.value.copy(isDictating = true)
            speechManager.startListening(preferOffline = _uiState.value.isOfflineMode, continuous = true) { transcript ->
                val newText = if (_uiState.value.dictationText.isBlank()) {
                    transcript
                } else {
                    "${_uiState.value.dictationText} $transcript"
                }
                val wordCount = newText.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size
                _uiState.value = _uiState.value.copy(
                    dictationText = newText,
                    dictationWordCount = wordCount
                )
            }
        }
    }

    fun appendDictationPunctuation(symbol: String) {
        val updated = "${_uiState.value.dictationText}$symbol "
        val count = updated.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size
        _uiState.value = _uiState.value.copy(dictationText = updated, dictationWordCount = count)
    }

    fun clearDictation() {
        _uiState.value = _uiState.value.copy(dictationText = "", dictationWordCount = 0)
    }

    fun copyDictationText() {
        systemExecutor.copyToClipboard(_uiState.value.dictationText, "Texto Ditado por Voz")
    }

    fun shareDictationText() {
        systemExecutor.shareText(_uiState.value.dictationText, "Compartilhar texto ditado")
    }

    fun saveDictationAsNote() {
        if (_uiState.value.dictationText.isNotBlank()) {
            val text = _uiState.value.dictationText
            val title = if (text.length > 25) text.take(25) + "..." else text
            viewModelScope.launch {
                repository.insertNote(VoiceNote(title = title, content = text, category = "Ditado"))
                _uiState.value = _uiState.value.copy(statusMessage = "Nota salva com sucesso")
            }
        }
    }

    // Custom Commands
    fun addCustomCommand(trigger: String, actionType: String, payload: String, reply: String) {
        viewModelScope.launch {
            repository.insertCommand(
                CustomCommand(
                    triggerPhrase = trigger,
                    actionType = actionType,
                    actionPayload = payload,
                    assistantReply = reply,
                    isEnabled = true
                )
            )
        }
    }

    fun toggleCommand(command: CustomCommand) {
        viewModelScope.launch {
            repository.updateCommand(command.copy(isEnabled = !command.isEnabled))
        }
    }

    fun deleteCommand(command: CustomCommand) {
        viewModelScope.launch {
            repository.deleteCommand(command)
        }
    }

    // Voice Notes
    fun addNote(title: String, content: String, category: String = "Geral") {
        viewModelScope.launch {
            repository.insertNote(VoiceNote(title = title, content = content, category = category))
        }
    }

    fun deleteNote(note: VoiceNote) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    fun speakNote(note: VoiceNote) {
        ttsManager.speak(
            text = note.content,
            pitch = _uiState.value.voicePitch,
            speed = _uiState.value.voiceSpeed,
            gender = _uiState.value.voiceGender
        )
    }

    // Settings Updates
    fun updateVoiceGender(gender: VoiceGender) {
        prefs.voiceGender = gender
        _uiState.value = _uiState.value.copy(voiceGender = gender)
        ttsManager.applyVoiceGender(gender)
        ttsManager.speak(
            text = "Perfil de voz alterado para ${gender.displayName}. Pronto para te responder por voz!",
            pitch = _uiState.value.voicePitch,
            speed = _uiState.value.voiceSpeed,
            gender = gender
        )
    }

    fun toggleAutoSpeak(enabled: Boolean) {
        prefs.autoSpeakResponses = enabled
        _uiState.value = _uiState.value.copy(autoSpeak = enabled)
    }

    fun updateAssistantName(newName: String) {
        val trimmed = newName.trim().ifBlank { "Aura" }
        prefs.assistantName = trimmed
        _uiState.value = _uiState.value.copy(assistantName = trimmed)
    }

    fun updateUserName(newUserName: String) {
        val trimmed = newUserName.trim().ifBlank { "Comandante" }
        prefs.userName = trimmed
        _uiState.value = _uiState.value.copy(userName = trimmed)
    }

    fun updateVoicePitch(pitch: Float) {
        prefs.voicePitch = pitch
        _uiState.value = _uiState.value.copy(voicePitch = pitch)
    }

    fun updateVoiceSpeed(speed: Float) {
        prefs.voiceSpeed = speed
        _uiState.value = _uiState.value.copy(voiceSpeed = speed)
    }

    fun toggleOfflineMode(enabled: Boolean) {
        prefs.isOfflineOnly = enabled
        _uiState.value = _uiState.value.copy(isOfflineMode = enabled)
    }

    fun updateThemeAccent(accent: ThemeAccent) {
        prefs.themeAccent = accent
        _uiState.value = _uiState.value.copy(themeAccent = accent)
    }

    fun testTtsVoice() {
        ttsManager.speak(
            text = "Olá ${_uiState.value.userName}! Eu sou ${_uiState.value.assistantName}, calibrando módulo de voz neural ${if (_uiState.value.voiceGender == VoiceGender.FEMALE) "feminina" else "masculina"}.",
            pitch = _uiState.value.voicePitch,
            speed = _uiState.value.voiceSpeed,
            gender = _uiState.value.voiceGender
        )
    }

    fun shareAppDownloadLink(url: String) {
        val shareMessage = "🤖 Conheça o Aura IA: Assistente Android com Comandos de Voz, Download na Play Store e IA Offline!\n\nBaixe e instale no seu celular: $url"
        systemExecutor.shareText(shareMessage, "Compartilhar Aura IA")
    }

    fun copyDownloadLink(url: String) {
        systemExecutor.copyToClipboard(url, "Link de Instalação Aura IA")
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.destroy()
        ttsManager.shutdown()
    }
}
