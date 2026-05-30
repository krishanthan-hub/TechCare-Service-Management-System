package com.techcare.services.utils

import android.util.Patterns

object ValidationUtils {

    fun isValidEmail(email: String): Boolean {
        return email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun isValidPhone(phone: String): Boolean {
        val cleaned = phone.replace(Regex("[\\s\\-()]"), "")
        return cleaned.matches(Regex("^[0-9]{10,12}$"))
    }

    fun isValidEmailOrPhone(input: String): Boolean {
        return isValidEmail(input) || isValidPhone(input)
    }

    fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }

    fun isValidName(name: String): Boolean {
        return name.trim().length >= 2
    }

    fun isValidIssue(issue: String): Boolean {
        return issue.trim().length >= 10
    }
}