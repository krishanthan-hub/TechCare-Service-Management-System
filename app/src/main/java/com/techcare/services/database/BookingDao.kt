package com.techcare.services.database

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class BookingDao {
    private val db = FirebaseFirestore.getInstance()
    private val col = db.collection("bookings")

    suspend fun createBooking(booking: Booking): Boolean {
        return try {
            col.document(booking.id).set(booking).await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun getBookingsByUser(userId: String): List<Booking> {
        return try {
            col.whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get().await()
                .mapNotNull { it.toObject(Booking::class.java) }
        } catch (e: Exception) {
            // Fallback without orderBy if index not ready
            try {
                col.whereEqualTo("userId", userId)
                    .get().await()
                    .mapNotNull { it.toObject(Booking::class.java) }
                    .sortedByDescending { it.createdAt }
            } catch (e2: Exception) { emptyList() }
        }
    }

    suspend fun getBookingById(bookingId: String): Booking? {
        return try {
            val doc = col.document(bookingId).get().await()
            if (doc.exists()) doc.toObject(Booking::class.java) else null
        } catch (e: Exception) { null }
    }

    // FIXED — removed whereNotIn which was causing slow/failed queries
    suspend fun getActiveBooking(userId: String): Booking? {
        return try {
            // Get ALL bookings for user then filter locally — much faster
            val all = col.whereEqualTo("userId", userId)
                .get().await()
                .mapNotNull { it.toObject(Booking::class.java) }

            // Filter out completed and cancelled locally
            all.filter { it.status != "Completed" && it.status != "Cancelled" }
                .maxByOrNull { it.createdAt }
        } catch (e: Exception) { null }
    }
}