package com.techcare.services

import android.annotation.SuppressLint
import com.techcare.services.home.HomeActivity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.techcare.services.auth.LoginActivity

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Handler(Looper.getMainLooper()).postDelayed({
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                // Already logged in → go to Home
                startActivity(Intent(this, HomeActivity::class.java))
            } else {
                // Not logged in → go to Log in
                startActivity(Intent(this, LoginActivity::class.java))
            }
            finish()
        }, 2000) // 2 second splash
    }
}