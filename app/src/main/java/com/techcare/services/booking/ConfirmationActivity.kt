package com.techcare.services.booking

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.techcare.services.databinding.ActivityConfirmationBinding
import com.techcare.services.home.HomeActivity

class ConfirmationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConfirmationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfirmationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val bookingId = intent.getStringExtra("bookingId") ?: ""
        val serviceName = intent.getStringExtra("serviceName") ?: ""
        val deviceName = intent.getStringExtra("deviceName") ?: ""
        val method = intent.getStringExtra("method") ?: "pickup"
        val dateTime = intent.getStringExtra("dateTime") ?: ""
        val cost = intent.getLongExtra("cost", 0L)

        // Format booking ID short like #TC-20240315-001
        val shortId = if (bookingId.length > 15) "#${bookingId.takeLast(15)}" else "#$bookingId"

        binding.tvBookingId.text = shortId
        binding.tvService.text = serviceName
        binding.tvDevice.text = deviceName
        binding.tvMethod.text = if (method == "pickup") "Home Pickup" else "Drop-off"
        binding.tvDateTime.text = dateTime
        binding.tvStatus.text = "Received"
        binding.tvCost.text = "LKR ${"%,d".format(cost)}"

        binding.btnMyBookings.setOnClickListener {
            startActivity(Intent(this, TrackRepairActivity::class.java).apply {
                putExtra("bookingId", bookingId)
                putExtra("serviceName", serviceName)
                putExtra("deviceName", deviceName)
                putExtra("status", "Received")
            })
            finish()
        }

        binding.btnHome.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }
}