package com.example.kfueityaps.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Post(
    val id: Long? = null,
    val user_id: String,
    val category: String,
    val content: String?,
    val media_urls: List<String>? = null,
    val document_url: String? = null,
    val document_name: String? = null,
    val created_at: String? = null,
    val profiles: Profile? = null,
    var likes: List<Like>? = emptyList(),
    var comments: List<Comment>? = emptyList()
)
