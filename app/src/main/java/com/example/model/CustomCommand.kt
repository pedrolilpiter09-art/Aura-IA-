package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_commands")
data class CustomCommand(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val triggerPhrase: String,
    val actionType: String, // "PLAY_STORE", "OPEN_APP", "WEB_SEARCH", "ALARM", "FLASHLIGHT", "TEXT_RESPONSE", "CUSTOM_URL"
    val actionPayload: String, // e.g. package name, query, search term, response text
    val assistantReply: String,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
