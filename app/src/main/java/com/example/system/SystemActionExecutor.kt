package com.example.system

import android.app.SearchManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.AlarmClock
import android.provider.Settings
import android.util.Log
import java.net.URLEncoder

sealed class SystemExecutionResult {
    data class Success(val message: String, val details: String = "") : SystemExecutionResult()
    data class Failure(val errorMessage: String) : SystemExecutionResult()
}

class SystemActionExecutor(private val context: Context) {

    private var isTorchOn = false

    fun openPlayStoreAppOrSearch(query: String): SystemExecutionResult {
        return try {
            val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
            val playStoreIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://search?q=$encodedQuery")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (playStoreIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(playStoreIntent)
            } else {
                val webIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://play.google.com/store/search?q=$encodedQuery")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(webIntent)
            }
            vibrateShort()
            SystemExecutionResult.Success(
                message = "Abrindo Google Play Store para download de '$query'.",
                details = "PlayStore Search: $query"
            )
        } catch (e: Exception) {
            Log.e("SystemActionExecutor", "Error opening Play Store", e)
            SystemExecutionResult.Failure("Falha ao abrir a Play Store: ${e.localizedMessage}")
        }
    }

    fun openPlayStorePackage(packageName: String): SystemExecutionResult {
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://details?id=$packageName")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                val webIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(webIntent)
            }
            vibrateShort()
            SystemExecutionResult.Success(
                message = "Abrindo página de instalação de '$packageName'.",
                details = "PlayStore Package: $packageName"
            )
        } catch (e: Exception) {
            SystemExecutionResult.Failure("Não foi possível acessar a Play Store.")
        }
    }

    fun openApp(appNameOrPackage: String): SystemExecutionResult {
        val query = appNameOrPackage.trim().lowercase()

        val knownPackages = mapOf(
            "youtube" to "com.google.android.youtube",
            "whatsapp" to "com.whatsapp",
            "chrome" to "com.android.chrome",
            "spotify" to "com.spotify.music",
            "instagram" to "com.instagram.android",
            "tiktok" to "com.zhiliaoapp.musically",
            "netflix" to "com.netflix.mediaclient",
            "telegram" to "org.telegram.messenger",
            "camera" to null, // handle via action
            "calculadora" to null,
            "configurações" to null,
            "relogio" to null,
            "alarme" to null,
            "galeria" to null
        )

        // Try direct launch by package
        val targetPackage = knownPackages[query] ?: if (query.contains(".")) query else null

        if (targetPackage != null) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(targetPackage)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                vibrateShort()
                return SystemExecutionResult.Success("Aplicativo '$appNameOrPackage' iniciado com sucesso.")
            }
        }

        // Handle generic system intents
        return when {
            query.contains("camera") || query.contains("câmera") || query.contains("foto") -> {
                val intent = Intent("android.media.action.IMAGE_CAPTURE").apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                safelyStartIntent(intent, "Câmera iniciada.")
            }
            query.contains("config") || query.contains("ajustes") -> {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                safelyStartIntent(intent, "Configurações do sistema abertas.")
            }
            query.contains("relogio") || query.contains("relógio") || query.contains("alarme") -> {
                val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                safelyStartIntent(intent, "Alarmes e relógio abertos.")
            }
            query.contains("calculadora") -> {
                val calcIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_APP_CALCULATOR)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                safelyStartIntent(calcIntent, "Calculadora iniciada.", fallback = {
                    openPlayStoreAppOrSearch("calculadora")
                })
            }
            query.contains("galeria") || query.contains("fotos") -> {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    type = "image/*"
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                safelyStartIntent(intent, "Galeria aberta.")
            }
            else -> {
                // If not found locally, suggest downloading from Play Store!
                openPlayStoreAppOrSearch(appNameOrPackage)
            }
        }
    }

    fun toggleFlashlight(forceState: Boolean? = null): SystemExecutionResult {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            if (cameraManager != null) {
                val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                    val chars = cameraManager.getCameraCharacteristics(id)
                    chars.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                }
                if (cameraId != null) {
                    val newState = forceState ?: !isTorchOn
                    cameraManager.setTorchMode(cameraId, newState)
                    isTorchOn = newState
                    vibrateShort()
                    val statusText = if (newState) "Lanterna ativada." else "Lanterna desativada."
                    return SystemExecutionResult.Success(statusText)
                }
            }
            SystemExecutionResult.Failure("Flash/Lanterna não encontrado no dispositivo.")
        } catch (e: Exception) {
            Log.e("SystemActionExecutor", "Flashlight error", e)
            SystemExecutionResult.Failure("Não foi possível controlar a lanterna: ${e.localizedMessage}")
        }
    }

    fun setAlarm(hour: Int, minutes: Int, message: String = "Alarme Aura IA"): SystemExecutionResult {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minutes)
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            vibrateShort()
            val timeStr = String.format("%02d:%02d", hour, minutes)
            SystemExecutionResult.Success("Alarme configurado com precisão para $timeStr.")
        } catch (e: Exception) {
            SystemExecutionResult.Failure("Falha ao definir alarme: ${e.localizedMessage}")
        }
    }

    fun setTimer(seconds: Int, message: String = "Timer Aura IA"): SystemExecutionResult {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            vibrateShort()
            val mins = seconds / 60
            val secs = seconds % 60
            val durationStr = if (mins > 0) "$mins min e $secs seg" else "$secs segundos"
            SystemExecutionResult.Success("Temporizador iniciado para $durationStr.")
        } catch (e: Exception) {
            SystemExecutionResult.Failure("Falha ao iniciar temporizador.")
        }
    }

    fun performWebSearch(query: String): SystemExecutionResult {
        return try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(SearchManager.QUERY, query)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${URLEncoder.encode(query, "UTF-8")}"))
                browserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(browserIntent)
            }
            vibrateShort()
            SystemExecutionResult.Success("Pesquisando na web: '$query'.")
        } catch (e: Exception) {
            SystemExecutionResult.Failure("Falha ao executar pesquisa na web.")
        }
    }

    fun dialPhoneNumber(phoneNumber: String): SystemExecutionResult {
        return try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phoneNumber")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            vibrateShort()
            SystemExecutionResult.Success("Abrindo discador para $phoneNumber.")
        } catch (e: Exception) {
            SystemExecutionResult.Failure("Não foi possível discar.")
        }
    }

    fun sendWhatsAppMessage(message: String, phone: String = ""): SystemExecutionResult {
        return try {
            val encodedMessage = URLEncoder.encode(message, "UTF-8")
            val uri = if (phone.isNotBlank()) {
                Uri.parse("https://api.whatsapp.com/send?phone=$phone&text=$encodedMessage")
            } else {
                Uri.parse("https://api.whatsapp.com/send?text=$encodedMessage")
            }
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            vibrateShort()
            SystemExecutionResult.Success("Preparando envio de mensagem via WhatsApp.")
        } catch (e: Exception) {
            SystemExecutionResult.Failure("WhatsApp não encontrado ou erro ao enviar.")
        }
    }

    fun openSettings(action: String, title: String): SystemExecutionResult {
        return try {
            val intent = Intent(action).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            vibrateShort()
            SystemExecutionResult.Success("Ajustes de $title abertos.")
        } catch (e: Exception) {
            SystemExecutionResult.Failure("Falha ao acessar configurações de $title.")
        }
    }

    fun copyToClipboard(text: String, label: String = "Aura IA Texto"): SystemExecutionResult {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(label, text)
            clipboard.setPrimaryClip(clip)
            vibrateShort()
            SystemExecutionResult.Success("Texto copiado para a área de transferência.")
        } catch (e: Exception) {
            SystemExecutionResult.Failure("Erro ao copiar texto.")
        }
    }

    fun shareText(text: String, title: String = "Compartilhar com Aura IA"): SystemExecutionResult {
        return try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val chooser = Intent.createChooser(intent, title).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(chooser)
            SystemExecutionResult.Success("Painel de compartilhamento aberto.")
        } catch (e: Exception) {
            SystemExecutionResult.Failure("Falha ao compartilhar texto.")
        }
    }

    private fun safelyStartIntent(intent: Intent, successMsg: String, fallback: (() -> SystemExecutionResult)? = null): SystemExecutionResult {
        return try {
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                vibrateShort()
                SystemExecutionResult.Success(successMsg)
            } else if (fallback != null) {
                fallback.invoke()
            } else {
                SystemExecutionResult.Failure("Nenhum aplicativo compatível encontrado no dispositivo.")
            }
        } catch (e: Exception) {
            SystemExecutionResult.Failure("Erro ao executar ação: ${e.localizedMessage}")
        }
    }

    private fun vibrateShort() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(50)
                }
            }
        } catch (e: Exception) {
            // ignore
        }
    }
}
