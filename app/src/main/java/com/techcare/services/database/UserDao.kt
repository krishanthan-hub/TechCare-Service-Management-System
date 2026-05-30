package com.techcare.services.database

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserDao {
    private val db = FirebaseFirestore.getInstance()
    private val col = db.collection("users")

    suspend fun saveUser(user: User): Boolean {
        return try {
            col.document(user.uid).set(user).await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun getUser(uid: String): User? {
        return try {
            val doc = col.document(uid).get().await()
            if (doc.exists()) doc.toObject(User::class.java) else null
        } catch (e: Exception) { null }
    }

    suspend fun updateBookingCount(uid: String) {
        try {
            col.document(uid).update(
                "bookingCount",
                com.google.firebase.firestore.FieldValue.increment(1)
            ).await()
        } catch (e: Exception) { }
    }
}