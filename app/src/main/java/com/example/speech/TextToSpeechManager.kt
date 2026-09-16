package com.example.speech

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import com.example.data.VoiceGender
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TextToSpeechManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var currentGender: VoiceGender = VoiceGender.FEMALE

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _speechAmplitude = MutableStateFlow(0f)
    val speechAmplitude: StateFlow<Float> = _speechAmplitude.asStateFlow()

    private var onDoneCallback: (() -> Unit)? = null

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            tts?.let { engine ->
                val result = engine.setLanguage(Locale.forLanguageTag("pt-BR"))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    engine.setLanguage(Locale.getDefault())
                }

                applyVoiceGender(currentGender)

                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                        val cb = onDoneCallback
                        onDoneCallback = null
                        cb?.invoke()
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                        val cb = onDoneCallback
                        onDoneCallback = null
                        cb?.invoke()
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        _isSpeaking.value = false
                        val cb = onDoneCallback
                        onDoneCallback = null
                        cb?.invoke()
                    }
                })
            }
        }
    }

    fun applyVoiceGender(gender: VoiceGender) {
        currentGender = gender
        if (!isInitialized || tts == null) return
        try {
            val engine = tts ?: return
            val voices = engine.voices
            if (!voices.isNullOrEmpty()) {
                val ptVoices = voices.filter {
                    it.locale.language.equals("pt", ignoreCase = true) ||
                    it.locale.language.equals(Locale.getDefault().language, ignoreCase = true)
                }

                val matchedVoice: Voice? = if (gender == VoiceGender.FEMALE) {
                    ptVoices.firstOrNull {
                        it.name.contains("female", ignoreCase = true) ||
                        it.name.contains("mulher", ignoreCase = true) ||
                        it.name.contains("afs", ignoreCase = true) ||
                        it.name.contains("f0", ignoreCase = true) ||
                        it.name.contains("pt-br-x-afs", ignoreCase = true)
                    } ?: ptVoices.firstOrNull()
                } else {
                    ptVoices.firstOrNull {
                        it.name.contains("male", ignoreCase = true) ||
                        it.name.contains("homem", ignoreCase = true) ||
                        it.name.contains("ptd", ignoreCase = true) ||
                        it.name.contains("m0", ignoreCase = true) ||
                        it.name.contains("pt-br-x-ptd", ignoreCase = true)
                    } ?: ptVoices.lastOrNull()
                }

                if (matchedVoice != null) {
                    engine.voice = matchedVoice
                }
            }
        } catch (e: Throwable) {
            // Ignore voice picker error on custom ROMs
        }
    }

    fun speak(
        text: String,
        pitch: Float = 1.0f,
        speed: Float = 1.0f,
        gender: VoiceGender = currentGender,
        onDone: (() -> Unit)? = null
    ) {
        if (!isInitialized || tts == null) {
            onDone?.invoke()
            return
        }
        stop()
        this.onDoneCallback = onDone

        applyVoiceGender(gender)

        // Effective pitch: adjusted based on gender
        val effectivePitch = if (gender == VoiceGender.FEMALE) {
            (pitch * 1.12f).coerceIn(0.5f, 2.0f)
        } else {
            (pitch * 0.85f).coerceIn(0.5f, 2.0f)
        }

        tts?.setPitch(effectivePitch)
        tts?.setSpeechRate(speed)

        val params = Bundle()
        val utteranceId = "aura_utterance_${System.currentTimeMillis()}"
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun stop() {
        if (isInitialized) {
            tts?.stop()
            _isSpeaking.value = false
            onDoneCallback = null
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
