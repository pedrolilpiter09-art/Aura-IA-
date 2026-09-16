package com.example.data

import com.example.model.CustomCommand
import com.example.model.VoiceLog
import com.example.model.VoiceNote
import kotlinx.coroutines.flow.Flow

class AuraRepository(
    private val voiceLogDao: VoiceLogDao,
    private val customCommandDao: CustomCommandDao,
    private val voiceNoteDao: VoiceNoteDao
) {
    val allLogs: Flow<List<VoiceLog>> = voiceLogDao.getAllLogs()
    val allCommands: Flow<List<CustomCommand>> = customCommandDao.getAllCommands()
    val allNotes: Flow<List<VoiceNote>> = voiceNoteDao.getAllNotes()

    suspend fun insertLog(log: VoiceLog): Long = voiceLogDao.insertLog(log)
    suspend fun deleteLog(id: Long) = voiceLogDao.deleteLogById(id)
    suspend fun clearLogs() = voiceLogDao.clearAllLogs()

    suspend fun getActiveCommands(): List<CustomCommand> = customCommandDao.getActiveCommands()
    suspend fun insertCommand(command: CustomCommand): Long = customCommandDao.insertCommand(command)
    suspend fun updateCommand(command: CustomCommand) = customCommandDao.updateCommand(command)
    suspend fun deleteCommand(command: CustomCommand) = customCommandDao.deleteCommand(command)
    suspend fun deleteCommandById(id: Long) = customCommandDao.deleteCommandById(id)

    suspend fun insertNote(note: VoiceNote): Long = voiceNoteDao.insertNote(note)
    suspend fun deleteNote(note: VoiceNote) = voiceNoteDao.deleteNote(note)
    suspend fun deleteNoteById(id: Long) = voiceNoteDao.deleteNoteById(id)

    suspend fun seedInitialCommandsIfEmpty() {
        val active = customCommandDao.getActiveCommands()
        if (active.isEmpty()) {
            customCommandDao.insertCommand(
                CustomCommand(
                    triggerPhrase = "Protocolo Noturno",
                    actionType = "FLASHLIGHT_ON",
                    actionPayload = "OFF",
                    assistantReply = "Protocolo Noturno ativado. Modulando brilho e ativando modo silencioso."
                )
            )
            customCommandDao.insertCommand(
                CustomCommand(
                    triggerPhrase = "Baixar WhatsApp",
                    actionType = "PLAY_STORE",
                    actionPayload = "com.whatsapp",
                    assistantReply = "Abrindo a Google Play Store para download imediato do WhatsApp."
                )
            )
            customCommandDao.insertCommand(
                CustomCommand(
                    triggerPhrase = "Buscar Música",
                    actionType = "WEB_SEARCH",
                    actionPayload = "musicas mais tocadas 2026",
                    assistantReply = "Pesquisando os maiores sucessos musicais para você."
                )
            )
        }
    }
}
