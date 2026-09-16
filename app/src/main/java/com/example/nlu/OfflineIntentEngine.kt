package com.example.nlu

import com.example.model.CustomCommand
import com.example.model.VoiceNote
import com.example.system.SystemActionExecutor
import com.example.system.SystemExecutionResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

sealed class NluResult {
    data class Executed(
        val intentType: String,
        val assistantResponse: String,
        val executionDetails: String,
        val isOffline: Boolean = true,
        val noteToSave: VoiceNote? = null
    ) : NluResult()

    data class NeedsOnlineAi(
        val query: String
    ) : NluResult()
}

class OfflineIntentEngine(
    private val systemActionExecutor: SystemActionExecutor
) {

    suspend fun processCommand(
        rawQuery: String,
        assistantName: String,
        customCommands: List<CustomCommand> = emptyList(),
        forceOffline: Boolean = false
    ): NluResult {
        val query = rawQuery.trim()
        val lower = query.lowercase()

        // 1. Check custom user-defined commands first
        for (cmd in customCommands) {
            if (cmd.isEnabled && (lower.contains(cmd.triggerPhrase.lowercase()) || lower == cmd.triggerPhrase.lowercase())) {
                val execResult = executeCustomCommand(cmd)
                val reply = if (cmd.assistantReply.isNotBlank()) {
                    cmd.assistantReply.replace("{name}", assistantName)
                } else {
                    "Executando comando personalizado: ${cmd.triggerPhrase}"
                }
                return NluResult.Executed(
                    intentType = "CUSTOM_COMMAND",
                    assistantResponse = reply,
                    executionDetails = "Custom: ${cmd.actionType} -> ${cmd.actionPayload}"
                )
            }
        }

        // 1.5. Voice Gender & Vocal Profile Switch
        if (lower.contains("voz masculina") || lower.contains("voz de homem") || lower.contains("mudar para homem") || lower.contains("voz do jarvis")) {
            return NluResult.Executed(
                intentType = "CHANGE_VOICE_MALE",
                assistantResponse = "$assistantName: Voz alterada para perfil Masculino com sucesso. Responderei agora com tom masculino.",
                executionDetails = "Voice Gender: MALE"
            )
        }
        if (lower.contains("voz feminina") || lower.contains("voz de mulher") || lower.contains("mudar para mulher") || lower.contains("voz da aura")) {
            return NluResult.Executed(
                intentType = "CHANGE_VOICE_FEMALE",
                assistantResponse = "$assistantName: Voz alterada para perfil Feminino com sucesso. Responderei agora com tom feminino.",
                executionDetails = "Voice Gender: FEMALE"
            )
        }

        // 1.6. Download Aura App onto Mobile / Baixar no Celular
        if (lower.contains("baixar no celular") || lower.contains("instalar no celular") || lower.contains("baixar este app") || lower.contains("baixar o aura") || lower.contains("como baixar")) {
            return NluResult.Executed(
                intentType = "NAVIGATE_DOWNLOAD_APP",
                assistantResponse = "$assistantName: Abrindo a central de download do aplicativo. Você pode baixar o arquivo APK ou compartilhar o link para instalar em seu celular.",
                executionDetails = "Nav: DOWNLOAD_APP"
            )
        }

        // 2. Play Store Download / Search Intent (explicit user requirement!)
        // Examples: "selecione play store e baixe este app whatsapp", "baixe o app telegram", "instalar instagram", "abrir play store e baixar netflix"
        val playStorePatterns = listOf(
            Regex("(?:selecione\\s+)?(?:play\\s*store|loja|google\\s*play)(?:\\s+e\\s+)?(?:baixe|baixar|instalar|instale|procure|procurar)?(?:\\s+este\\s+app|\\s+o\\s+app|\\s+o\\s+aplicativo)?\\s*(.*)", RegexOption.IGNORE_CASE),
            Regex("(?:baixe|baixar|instale|instalar|download)(?:\\s+o\\s+app|\\s+o\\s+aplicativo|\\s+o|\\s+a)?\\s+([a-zA-Z0-9_\\s]+)", RegexOption.IGNORE_CASE),
            Regex("abrir\\s+play\\s*store(?:\\s+e\\s+baixar)?\\s*(.*)", RegexOption.IGNORE_CASE)
        )

        for (pattern in playStorePatterns) {
            val match = pattern.find(query)
            if (match != null) {
                var appName = match.groupValues[1].trim()
                appName = appName.replace(Regex("^(e\\s+baixe|e\\s+baixar|este\\s+app|app|aplicativo)\\s+", RegexOption.IGNORE_CASE), "")
                if (appName.isBlank()) appName = "jogos e apps populares"

                val result = systemActionExecutor.openPlayStoreAppOrSearch(appName)
                val response = when (result) {
                    is SystemExecutionResult.Success -> "$assistantName: Abrindo a Google Play Store para baixar $appName."
                    is SystemExecutionResult.Failure -> "$assistantName: Falha ao acessar a Play Store: ${result.errorMessage}"
                }
                return NluResult.Executed("PLAY_STORE_DOWNLOAD", response, "Download target: $appName")
            }
        }

        // 3. Flashlight / Lanterna
        if (lower.contains("lanterna") || lower.contains("flash")) {
            val turnOff = lower.contains("desligar") || lower.contains("apagar") || lower.contains("desative")
            val turnOn = lower.contains("ligar") || lower.contains("acender") || lower.contains("ative")
            val forceState = if (turnOff) false else if (turnOn) true else null

            val result = systemActionExecutor.toggleFlashlight(forceState)
            val reply = when (result) {
                is SystemExecutionResult.Success -> "$assistantName: ${result.message}"
                is SystemExecutionResult.Failure -> "$assistantName: ${result.errorMessage}"
            }
            return NluResult.Executed("FLASHLIGHT", reply, "Flashlight toggled")
        }

        // 4. Open Application
        val openAppPattern = Regex("^(?:abrir|abra|iniciar|inicie|executar|execute)\\s+(?:o\\s+app\\s+|o\\s+aplicativo\\s+|o\\s+|a\\s+)?([a-zA-Z0-9_\\s]+)", RegexOption.IGNORE_CASE)
        val openMatch = openAppPattern.find(query)
        if (openMatch != null) {
            val targetApp = openMatch.groupValues[1].trim()
            val result = systemActionExecutor.openApp(targetApp)
            val reply = when (result) {
                is SystemExecutionResult.Success -> "$assistantName: ${result.message}"
                is SystemExecutionResult.Failure -> "$assistantName: ${result.errorMessage}"
            }
            return NluResult.Executed("OPEN_APP", reply, "Open: $targetApp")
        }

        // 5. Alarm / Alarme
        if (lower.contains("alarme") || lower.contains("despertador")) {
            val timePattern = Regex("(\\d{1,2})\\s*(?:e|:|h|horas?)?\\s*(\\d{0,2})", RegexOption.IGNORE_CASE)
            val timeMatch = timePattern.find(query)
            if (timeMatch != null) {
                val hour = timeMatch.groupValues[1].toIntOrNull() ?: 7
                val minute = timeMatch.groupValues[2].toIntOrNull() ?: 0
                val result = systemActionExecutor.setAlarm(hour, minute, "Alarme Aura IA")
                val reply = when (result) {
                    is SystemExecutionResult.Success -> "$assistantName: Alarme definido para ${String.format("%02d:%02d", hour, minute)}."
                    is SystemExecutionResult.Failure -> "$assistantName: ${result.errorMessage}"
                }
                return NluResult.Executed("SET_ALARM", reply, "Alarm: $hour:$minute")
            } else {
                val result = systemActionExecutor.setAlarm(7, 0, "Alarme Aura IA")
                return NluResult.Executed("SET_ALARM", "$assistantName: Abrindo configuração de alarme.", "Alarm standard")
            }
        }

        // 6. Timer / Temporizador
        if (lower.contains("timer") || lower.contains("temporizador") || lower.contains("cronômetro")) {
            val minutePattern = Regex("(\\d+)\\s*(?:minutos?|min|m)", RegexOption.IGNORE_CASE)
            val secondPattern = Regex("(\\d+)\\s*(?:segundos?|seg|s)", RegexOption.IGNORE_CASE)

            val minMatch = minutePattern.find(query)
            val secMatch = secondPattern.find(query)

            val mins = minMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
            val secs = secMatch?.groupValues?.get(1)?.toIntOrNull() ?: (if (mins == 0) 60 else 0)
            val totalSeconds = (mins * 60) + secs

            val result = systemActionExecutor.setTimer(totalSeconds)
            val reply = when (result) {
                is SystemExecutionResult.Success -> "$assistantName: ${result.message}"
                is SystemExecutionResult.Failure -> "$assistantName: ${result.errorMessage}"
            }
            return NluResult.Executed("SET_TIMER", reply, "Timer: $totalSeconds sec")
        }

        // 7. Voice Notes / Anotações
        val notePattern = Regex("^(?:anotar|anote|criar\\s+nota|salvar\\s+nota|lembrete|gravar\\s+ideia)\\s*(.*)", RegexOption.IGNORE_CASE)
        val noteMatch = notePattern.find(query)
        if (noteMatch != null) {
            val noteContent = noteMatch.groupValues[1].trim().ifBlank { query }
            val title = if (noteContent.length > 25) noteContent.take(25) + "..." else noteContent
            val note = VoiceNote(
                title = title,
                content = noteContent,
                category = "Voz"
            )
            return NluResult.Executed(
                intentType = "VOICE_NOTE",
                assistantResponse = "$assistantName: Anotação gravada com sucesso: \"$noteContent\".",
                executionDetails = "Note: $title",
                noteToSave = note
            )
        }

        // 8. Time / Date Queries
        if (lower.contains("que horas") || lower.contains("horário atual") || lower.contains("hora é")) {
            val timeStr = SimpleDateFormat("HH:mm", Locale.forLanguageTag("pt-BR")).format(Date())
            return NluResult.Executed("TIME_QUERY", "$assistantName: Agora são exatamente $timeStr.", "Time: $timeStr")
        }

        if (lower.contains("que dia") || lower.contains("qual a data") || lower.contains("dia de hoje")) {
            val dateStr = SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("pt-BR")).format(Date())
            return NluResult.Executed("DATE_QUERY", "$assistantName: Hoje é $dateStr.", "Date: $dateStr")
        }

        // 9. Simple Mathematical Calculator
        val mathPattern = Regex("(?:quanto é|calcular|calcule|calcula|valor de)\\s*([0-9+\\-*/xX÷\\.\\s\\(\\)]+)", RegexOption.IGNORE_CASE)
        val mathMatch = mathPattern.find(query)
        if (mathMatch != null) {
            val expression = mathMatch.groupValues[1].trim()
            val mathResult = evaluateSimpleMath(expression)
            if (mathResult != null) {
                return NluResult.Executed("MATH_CALC", "$assistantName: O resultado de $expression é $mathResult.", "Calc: $expression = $mathResult")
            }
        }

        // 10. Web Search
        val searchPattern = Regex("^(?:pesquisar|pesquise|buscar|busque|procurar|procure|google)\\s+(?:por\\s+|sobre\\s+)?(.*)", RegexOption.IGNORE_CASE)
        val searchMatch = searchPattern.find(query)
        if (searchMatch != null) {
            val term = searchMatch.groupValues[1].trim()
            val result = systemActionExecutor.performWebSearch(term)
            val reply = when (result) {
                is SystemExecutionResult.Success -> "$assistantName: ${result.message}"
                is SystemExecutionResult.Failure -> "$assistantName: ${result.errorMessage}"
            }
            return NluResult.Executed("WEB_SEARCH", reply, "Search: $term")
        }

        // 11. System Settings Quick Actions
        if (lower.contains("wifi") || lower.contains("wi-fi")) {
            val result = systemActionExecutor.openSettings(android.provider.Settings.ACTION_WIFI_SETTINGS, "Wi-Fi")
            return NluResult.Executed("SETTINGS", "$assistantName: ${result}", "WiFi Settings")
        }
        if (lower.contains("bluetooth")) {
            val result = systemActionExecutor.openSettings(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS, "Bluetooth")
            return NluResult.Executed("SETTINGS", "$assistantName: ${result}", "Bluetooth Settings")
        }
        if (lower.contains("bateria")) {
            val result = systemActionExecutor.openSettings(android.provider.Settings.ACTION_BATTERY_SAVER_SETTINGS, "Bateria")
            return NluResult.Executed("SETTINGS", "$assistantName: Ajustes de economia de bateria abertos.", "Battery Settings")
        }

        // 12. WhatsApp message intent
        val whatsappPattern = Regex("(?:mandar|enviar|mande|envie)\\s+(?:uma\\s+)?mensagem\\s+(?:no\\s+whatsapp|pelo\\s+whatsapp)?(?:\\s+para\\s+([a-zA-Z0-9_\\s]+))?(?:\\s+dizendo\\s+|\\s+falando\\s+|:\\s*)(.*)", RegexOption.IGNORE_CASE)
        val wMatch = whatsappPattern.find(query)
        if (wMatch != null) {
            val contact = wMatch.groupValues[1].trim()
            val textMsg = wMatch.groupValues[2].trim()
            systemActionExecutor.sendWhatsAppMessage(textMsg.ifBlank { "Olá!" })
            return NluResult.Executed("WHATSAPP_MSG", "$assistantName: Abrindo WhatsApp para enviar a mensagem.", "WhatsApp: $contact - $textMsg")
        }

        // 13. Phone Call / Dialer
        val callPattern = Regex("^(?:ligar|ligue|fazer\\s+ligação|chamar)\\s+(?:para\\s+)?([0-9\\+\\s\\-\\(\\)]{3,})", RegexOption.IGNORE_CASE)
        val callMatch = callPattern.find(query)
        if (callMatch != null) {
            val number = callMatch.groupValues[1].replace(Regex("[^0-9+]"), "")
            systemActionExecutor.dialPhoneNumber(number)
            return NluResult.Executed("CALL_PHONE", "$assistantName: Discando para o número $number.", "Call: $number")
        }

        // 14. If forceOffline is true or general question fallback in offline mode
        if (forceOffline) {
            val offlineFallback = generateOfflineChatFallback(query, assistantName)
            return NluResult.Executed("OFFLINE_CHAT", offlineFallback, "Offline NLU Response")
        }

        // Otherwise delegate to online Gemini AI
        return NluResult.NeedsOnlineAi(query)
    }

    private fun executeCustomCommand(cmd: CustomCommand): SystemExecutionResult {
        return when (cmd.actionType) {
            "PLAY_STORE" -> systemActionExecutor.openPlayStoreAppOrSearch(cmd.actionPayload)
            "OPEN_APP" -> systemActionExecutor.openApp(cmd.actionPayload)
            "WEB_SEARCH" -> systemActionExecutor.performWebSearch(cmd.actionPayload)
            "FLASHLIGHT_ON" -> systemActionExecutor.toggleFlashlight(true)
            "FLASHLIGHT_OFF" -> systemActionExecutor.toggleFlashlight(false)
            "ALARM" -> {
                val parts = cmd.actionPayload.split(":")
                val h = parts.getOrNull(0)?.toIntOrNull() ?: 7
                val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
                systemActionExecutor.setAlarm(h, m, cmd.triggerPhrase)
            }
            else -> SystemExecutionResult.Success(cmd.assistantReply)
        }
    }

    private fun evaluateSimpleMath(expr: String): Double? {
        return try {
            val cleaned = expr.replace("x", "*", ignoreCase = true)
                .replace("X", "*")
                .replace("÷", "/")
                .replace(",", ".")
                .replace("\\s".toRegex(), "")

            // Support basic binary operators: +, -, *, /
            val addMatch = cleaned.split("+")
            if (addMatch.size == 2) {
                return (addMatch[0].toDoubleOrNull() ?: return null) + (addMatch[1].toDoubleOrNull() ?: return null)
            }
            val subMatch = cleaned.split("-")
            if (subMatch.size == 2 && subMatch[0].isNotBlank()) {
                return (subMatch[0].toDoubleOrNull() ?: return null) - (subMatch[1].toDoubleOrNull() ?: return null)
            }
            val mulMatch = cleaned.split("*")
            if (mulMatch.size == 2) {
                return (mulMatch[0].toDoubleOrNull() ?: return null) * (mulMatch[1].toDoubleOrNull() ?: return null)
            }
            val divMatch = cleaned.split("/")
            if (divMatch.size == 2) {
                val denominator = divMatch[1].toDoubleOrNull() ?: return null
                if (denominator == 0.0) return null
                return (divMatch[0].toDoubleOrNull() ?: return null) / denominator
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun generateOfflineChatFallback(query: String, assistantName: String): String {
        val lower = query.lowercase()
        return when {
            lower.contains("olá") || lower.contains("oi") || lower.contains("ola") || lower.contains("bom dia") || lower.contains("boa tarde") || lower.contains("boa noite") ->
                "Olá! Sou $assistantName, seu assistente neural inteligente. Estou pronto para executar seus comandos de voz e tarefas do sistema no modo offline seguro."
            lower.contains("quem é você") || lower.contains("qual seu nome") || lower.contains("o que você faz") ->
                "Eu sou $assistantName, uma inteligência artificial projetada para automação de tarefas no Android por voz, com suporte offline para total privacidade e velocidade."
            lower.contains("ajuda") || lower.contains("comandos") || lower.contains("o que posso falar") ->
                "Você pode dizer: 'Baixar aplicativo na Play Store', 'Definir alarme às 07:00', 'Ligar lanterna', 'Anotar ideia', 'Abrir YouTube', 'Digitar por voz' ou criar comandos personalizados."
            lower.contains("obrigado") || lower.contains("valeu") || lower.contains("obrigada") ->
                "Às suas ordens! Sempre pronto para ajudar."
            else ->
                "$assistantName (Modo Offline): Reconheci \"$query\". Você pode executar tarefas do sistema, abrir apps, baixar na Play Store ou conectar-se à internet para respostas complexas da IA."
        }
    }
}
