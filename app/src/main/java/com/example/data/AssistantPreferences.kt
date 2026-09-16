package com.example.data

import android.content.Context
import android.content.SharedPreferences

enum class VoiceGender(val displayName: String, val defaultPitch: Float, val iconLabel: String) {
    FEMALE("Voz Feminina", 1.18f, "👩"),
    MALE("Voz Masculina", 0.85f, "👨")
}

enum class ThemeAccent(val displayName: String, val primaryHex: Long, val secondaryHex: Long) {
    CYAN("Ciber Neon", 0xFF00F0FF, 0xFF7928CA),
    PURPLE("Holo Púrpura", 0xFFA855F7, 0xFF00F0FF),
    EMERALD("Matriz Esmeralda", 0xFF00FF88, 0xFF00E5FF),
    AMBER("Solar Ambar", 0xFFFFB800, 0xFFFF4500)
}

class AssistantPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("aura_ia_prefs", Context.MODE_PRIVATE)

    var assistantName: String
        get() = prefs.getString(KEY_ASSISTANT_NAME, "Aura") ?: "Aura"
        set(value) = prefs.edit().putString(KEY_ASSISTANT_NAME, value.trim()).apply()

    var userName: String
        get() = prefs.getString(KEY_USER_NAME, "Comandante") ?: "Comandante"
        set(value) = prefs.edit().putString(KEY_USER_NAME, value.trim()).apply()

    var voiceGender: VoiceGender
        get() {
            val name = prefs.getString(KEY_VOICE_GENDER, VoiceGender.FEMALE.name) ?: VoiceGender.FEMALE.name
            return try {
                VoiceGender.valueOf(name)
            } catch (e: Exception) {
                VoiceGender.FEMALE
            }
        }
        set(value) = prefs.edit().putString(KEY_VOICE_GENDER, value.name).apply()

    var voicePitch: Float
        get() = prefs.getFloat(KEY_VOICE_PITCH, 1.18f)
        set(value) = prefs.edit().putFloat(KEY_VOICE_PITCH, value).apply()

    var voiceSpeed: Float
        get() = prefs.getFloat(KEY_VOICE_SPEED, 1.05f)
        set(value) = prefs.edit().putFloat(KEY_VOICE_SPEED, value).apply()

    var isOfflineOnly: Boolean
        get() = prefs.getBoolean(KEY_OFFLINE_ONLY, false)
        set(value) = prefs.edit().putBoolean(KEY_OFFLINE_ONLY, value).apply()

    var autoSpeakResponses: Boolean
        get() = prefs.getBoolean(KEY_AUTO_SPEAK, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_SPEAK, value).apply()

    var themeAccent: ThemeAccent
        get() {
            val name = prefs.getString(KEY_THEME_ACCENT, ThemeAccent.CYAN.name) ?: ThemeAccent.CYAN.name
            return try {
                ThemeAccent.valueOf(name)
            } catch (e: Exception) {
                ThemeAccent.CYAN
            }
        }
        set(value) = prefs.edit().putString(KEY_THEME_ACCENT, value.name).apply()

    var continuousListening: Boolean
        get() = prefs.getBoolean(KEY_CONTINUOUS_LISTENING, false)
        set(value) = prefs.edit().putBoolean(KEY_CONTINUOUS_LISTENING, value).apply()

    companion object {
        private const val KEY_ASSISTANT_NAME = "key_assistant_name"
        private const val KEY_USER_NAME = "key_user_name"
        private const val KEY_VOICE_GENDER = "key_voice_gender"
        private const val KEY_VOICE_PITCH = "key_voice_pitch"
        private const val KEY_VOICE_SPEED = "key_voice_speed"
        private const val KEY_OFFLINE_ONLY = "key_offline_only"
        private const val KEY_AUTO_SPEAK = "key_auto_speak"
        private const val KEY_THEME_ACCENT = "key_theme_accent"
        private const val KEY_CONTINUOUS_LISTENING = "key_continuous_listening"
    }
}
