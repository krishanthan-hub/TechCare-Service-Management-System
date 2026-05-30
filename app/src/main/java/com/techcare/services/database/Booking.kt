package com.techcare.services.database

data class Booking(
    val id: String = "",
    val userId: String = "",
    val serviceId: String = "",
    val serviceName: String = "",
    val deviceName: String = "",
    val issue: String = "",
    val photoUrl: String = "",
    val method: String = "pickup",
    val dateTime: String = "",
    val status: String = "Received",
    val technicianName: String = "",
    val technicianRating: Double = 0.0,
    val technicianId: String = "",
    val estimatedCompletion: String = "Within 3-5 business days",
    val cost: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)