package com.example.agro_map.data.model

data class Comment(
    val id: String = "",
    val text: String = "",
    val rating: Int? = null,
    val authorId: String = "",
    val authorUsername: String = "",
    val createdAt: Long = 0L
)
