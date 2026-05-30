package com.techcare.services.auth

import com.techcare.services.home.HomeActivity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.techcare.services.database.AppDatabase
import com.techcare.services.database.User
import com.techcare.services.databinding.ActivityRegisterBinding
import com.techcare.services.utils.SessionManager
import com.techcare.services.utils.ValidationUtils
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val auth = FirebaseAuth.getInstance()
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)

        binding.btnRegister.setOnClickListener { performRegister() }
        binding.tvSignIn.setOnClickListener { finish() }
    }

    private fun performRegister() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()
        var isValid = true

        if (!ValidationUtils.isValidName(name)) {
            binding.tilName.error = "Enter your full name"
            isValid = false
        } else binding.tilName.error = null

        if (!ValidationUtils.isValidEmail(email)) {
            binding.tilEmail.error = "Enter a valid email"
            isValid = false
        } else binding.tilEmail.error = null

        if (!ValidationUtils.isValidPhone(phone)) {
            binding.tilPhone.error = "Enter valid 10-digit phone"
            isValid = false
        } else binding.tilPhone.error = null

        if (!ValidationUtils.isValidPassword(password)) {
            binding.tilPassword.error = "Min 6 characters"
            isValid = false
        } else binding.tilPassword.error = null

        if (password != confirmPassword) {
            binding.tilConfirmPassword.error = "Passwords do not match"
            isValid = false
        } else binding.tilConfirmPassword.error = null

        if (!isValid) return

        setLoading(true)

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user!!.uid
                val user = User(
                    uid = uid, name = name, email = email, phone = phone,
                    savedDevices = listOf("My Device"), bookingCount = 0, rating = 0.0
                )
                lifecycleScope.launch {
                    val saved = AppDatabase.userDao.saveUser(user)
                    if (saved) {
                        session.saveUserName(name)
                        session.saveUserEmail(email)
                        setLoading(false)

                        Toast.makeText(this@RegisterActivity, "Registration Successful", Toast.LENGTH_SHORT).show()

                        startActivity(Intent(this@RegisterActivity, HomeActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        })
                    } else {
                        setLoading(false)
                        Toast.makeText(this@RegisterActivity, "Profile creation failed. Please check connection.", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .addOnFailureListener { e ->
                setLoading(false)
                binding.tilEmail.error = e.message ?: "Registration failed"
            }
    }

    private fun setLoading(loading: Boolean) {
        binding.btnRegister.isEnabled = !loading
        binding.btnRegister.text = if (loading) "Processing..." else "CREATE ACCOUNT"
    }
}