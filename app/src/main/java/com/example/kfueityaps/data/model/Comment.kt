package com.example.kfueityaps.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Comment(
    val id: Long? = null,
    val post_id: Long,
    val user_id: String,
    val content: String,
    val created_at: String? = null,
    val profiles: Profile? = null
)
