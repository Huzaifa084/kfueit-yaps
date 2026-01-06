package com.example.kfueityaps.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String,
    val full_name: String,
    val email: String,
    val avatar_url: String? = null
)
