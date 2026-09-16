package com.example.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class SpeechState {
    IDLE,
    LISTENING,
    PROCESSING,
    ERROR
}

class SpeechRecognitionManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null

    private val _speechState = MutableStateFlow(SpeechState.IDLE)
    val speechState: StateFlow<SpeechState> = _speechState.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val _rmsAmplitude = MutableStateFlow(0f)
    val rmsAmplitude: StateFlow<Float> = _rmsAmplitude.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var onResultListener: ((String) -> Unit)? = null
    private var isContinuous = false

    init {
        initRecognizer()
    }

    private fun initRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(createRecognitionListener())
                }
            } catch (e: Exception) {
                Log.e("SpeechRecognizer", "Error creating SpeechRecognizer", e)
            }
        }
    }

    private fun createRecognitionListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _speechState.value = SpeechState.LISTENING
            _errorMessage.value = null
        }

        override fun onBeginningOfSpeech() {
            _speechState.value = SpeechState.LISTENING
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Normalize -2dB..10dB to 0..1 range
            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
            _rmsAmplitude.value = normalized
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            _speechState.value = SpeechState.PROCESSING
            _rmsAmplitude.value = 0f
        }

        override fun onError(error: Int) {
            _rmsAmplitude.value = 0f
            val message = when (error) {
                SpeechRecognizer.ERROR_NO_MATCH -> "Nenhum comando de voz identificado."
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Tempo de escuta esgotado."
                SpeechRecognizer.ERROR_AUDIO -> "Falha no microfone de áudio."
                SpeechRecognizer.ERROR_CLIENT -> "Erro do cliente de reconhecimento."
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permissão de microfone necessária."
                SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Sem conexão de rede. Usando modo offline."
                else -> "Aguardando novo comando de voz."
            }
            _errorMessage.value = message
            _speechState.value = SpeechState.IDLE
        }

        override fun onResults(results: Bundle?) {
            _speechState.value = SpeechState.IDLE
            _rmsAmplitude.value = 0f
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val bestMatch = matches?.firstOrNull() ?: ""
            _partialText.value = bestMatch
            if (bestMatch.isNotBlank()) {
                onResultListener?.invoke(bestMatch)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val partial = matches?.firstOrNull() ?: ""
            if (partial.isNotBlank()) {
                _partialText.value = partial
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    fun startListening(
        preferOffline: Boolean = false,
        continuous: Boolean = false,
        onResult: (String) -> Unit
    ) {
        this.onResultListener = onResult
        this.isContinuous = continuous
        _partialText.value = ""
        _errorMessage.value = null

        if (speechRecognizer == null) {
            initRecognizer()
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.forLanguageTag("pt-BR").toString())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            if (preferOffline) {
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
        }

        try {
            speechRecognizer?.startListening(intent)
            _speechState.value = SpeechState.LISTENING
        } catch (e: Exception) {
            _errorMessage.value = "Não foi possível iniciar o microfone."
            _speechState.value = SpeechState.ERROR
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.e("SpeechRecognizer", "Error stopping listener", e)
        }
        _speechState.value = SpeechState.IDLE
        _rmsAmplitude.value = 0f
    }

    fun cancel() {
        try {
            speechRecognizer?.cancel()
        } catch (e: Exception) {
            Log.e("SpeechRecognizer", "Error canceling listener", e)
        }
        _speechState.value = SpeechState.IDLE
        _rmsAmplitude.value = 0f
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e("SpeechRecognizer", "Error destroying speech recognizer", e)
        }
        speechRecognizer = null
    }
}
