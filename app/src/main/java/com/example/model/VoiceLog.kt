package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "voice_logs")
data class VoiceLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userQuery: String,
    val assistantResponse: String,
    val intentType: String,
    val isSuccess: Boolean = true,
    val executionDetails: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isOfflineProcessed: Boolean = true
)
