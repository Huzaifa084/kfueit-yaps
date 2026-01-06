package com.example.kfueityaps.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Conversation(
    val id: String? = null,
    val user1_id: String,
    val user2_id: String,
    val status: String = "REQUESTED",
    val last_message: String? = null,
    val updated_at: String? = null,
    // Joined profile data (not always present)
    val user1_profile: Profile? = null,
    val user2_profile: Profile? = null
)
