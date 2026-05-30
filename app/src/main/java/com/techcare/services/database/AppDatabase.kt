package com.techcare.services.database

// Central access point for all DAOs
object AppDatabase {
    val userDao = UserDao()
    val bookingDao = BookingDao()
}