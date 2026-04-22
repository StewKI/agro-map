package com.example.agro_map.data.model

data class Report(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: ReportCategory = ReportCategory.OTHER,
    val photoUrl: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val authorId: String = "",
    val authorUsername: String = "",
    val createdAt: Long = 0L
)
