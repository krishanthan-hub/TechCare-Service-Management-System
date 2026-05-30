package com.techcare.services.services

data class ServiceItem(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val description: String = "",
    val price: Long = 0,
    val rating: Double = 0.0,
    val icon: String = "🔧"
)