package com.techcare.services.utils

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("TechCarePrefs", Context.MODE_PRIVATE)

    fun saveUserName(name: String) = prefs.edit().putString("userName", name).apply()
    fun getUserName(): String = prefs.getString("userName", "User") ?: "User"

    fun saveUserEmail(email: String) = prefs.edit().putString("userEmail", email).apply()
    fun getUserEmail(): String = prefs.getString("userEmail", "") ?: ""

    fun clearSession() = prefs.edit().clear().apply()
}