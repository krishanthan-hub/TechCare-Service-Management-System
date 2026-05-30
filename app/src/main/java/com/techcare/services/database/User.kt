package com.techcare.services.database

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val savedDevices: List<String> = listOf("Samsung Galaxy S22", "My Laptop"),
    val bookingCount: Int = 0,
    val rating: Double = 4.9
)