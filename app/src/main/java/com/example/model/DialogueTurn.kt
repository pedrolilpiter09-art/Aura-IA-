package com.example.model

enum class DialogueSender {
    USER,
    ASSISTANT
}

data class DialogueTurn(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: DialogueSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSpokenVoice: Boolean = true
)
