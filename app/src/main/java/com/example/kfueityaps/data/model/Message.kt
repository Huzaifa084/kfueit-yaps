package com.example.kfueityaps.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val id: Long? = null,
    val conversation_id: String,
    val sender_id: String,
    val content: String,
    val created_at: String? = null
)
