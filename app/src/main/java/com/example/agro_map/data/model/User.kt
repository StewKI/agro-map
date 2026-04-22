package com.example.agro_map.data.model

data class User(
    val uid: String = "",
    val username: String = "",
    val fullName: String = "",
    val phone: String = "",
    val photoUrl: String = "",
    val points: Int = 0,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)
