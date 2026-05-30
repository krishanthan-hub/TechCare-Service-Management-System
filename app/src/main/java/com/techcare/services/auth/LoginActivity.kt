package com.techcare.services.auth

import com.techcare.services.home.HomeActivity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.techcare.services.R
import com.techcare.services.database.AppDatabase
import com.techcare.services.databinding.ActivityLoginBinding
import com.techcare.services.utils.SessionManager
import com.techcare.services.utils.ValidationUtils
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        session = SessionManager(this)

        binding.btnLogin.setOnClickListener { performLogin() }

        binding.btnCreateAccount.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.tvForgotPassword.setOnClickListener {
            val input = binding.etEmailPhone.text.toString().trim()
            if (input.contains("@")) {
                auth.sendPasswordResetEmail(input).addOnSuccessListener {
                    Toast.makeText(this, "Reset link sent to $input", Toast.LENGTH_LONG).show()
                }
            } else {
                binding.tilEmailPhone.error = "Enter your email address to reset password"
            }
        }
    }

    private fun performLogin() {
        val input = binding.etEmailPhone.text.toString().trim()
        val password = binding.etPassword.text.toString()
        var isValid = true

        if (!ValidationUtils.isValidEmailOrPhone(input)) {
            binding.tilEmailPhone.error = "Enter a valid email or 10-digit phone number"
            isValid = false
        } else binding.tilEmailPhone.error = null

        if (!ValidationUtils.isValidPassword(password)) {
            binding.tilPassword.error = "Password must be at least 6 characters"
            isValid = false
        } else binding.tilPassword.error = null

        if (!isValid) return

        setLoading(true)

        // Convert phone to email format if phone was entered
        val email = if (input.contains("@")) input else "$input@techcare.lk"

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user!!.uid
                lifecycleScope.launch {
                    val user = AppDatabase.userDao.getUser(uid)
                    session.saveUserName(user?.name ?: "User")
                    session.saveUserEmail(user?.email ?: email)
                    setLoading(false)
                    Toast.makeText(this@LoginActivity, "Login Successful", Toast.LENGTH_SHORT).show()

                    startActivity(Intent(this@LoginActivity, HomeActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                }
            }
            .addOnFailureListener {
                setLoading(false)
                binding.tilPassword.error = "Invalid email or password. Please try again."
            }
    }

    private fun setLoading(loading: Boolean) {
        binding.btnLogin.isEnabled = !loading
        binding.btnLogin.text = if (loading) "Logging in..." else "LOG IN"
    }
}