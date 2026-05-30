package com.techcare.services.profile

import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.techcare.services.R
import com.techcare.services.auth.LoginActivity
import com.techcare.services.booking.MyBookingsActivity
import com.techcare.services.database.AppDatabase
import com.techcare.services.databinding.ActivityProfileBinding
import com.techcare.services.home.HomeActivity
import com.techcare.services.notifications.NotificationsActivity
import com.techcare.services.services.ServicesActivity
import com.techcare.services.support.SupportActivity
import com.techcare.services.utils.SessionManager
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private var isDevicesExpanded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadProfile()
        loadActiveRepairs()
        setupMenuItems()
        setupNavigation()
    }

    private fun loadActiveRepairs() {
        val uid = auth.currentUser?.uid ?: return

        firestore.collection("bookings")
            .whereEqualTo("userId", uid)
            .whereIn("status", listOf(
                "Received",
                "Technician Assigned",
                "Under Repair",
                "Ready for Pickup"
            ))
            .get()
            .addOnSuccessListener { docs ->
                val count = docs.size()
                binding.tvActiveRepairs.text = count.toString()
            }
            .addOnFailureListener {
                binding.tvActiveRepairs.text = "0"
            }
    }

    private fun setupNavigation() {
        binding.navHome.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }
        binding.navServices.setOnClickListener {
            startActivity(Intent(this, ServicesActivity::class.java))
            finish()
        }
        binding.navBookings.setOnClickListener {
            startActivity(Intent(this, MyBookingsActivity::class.java))
            finish()
        }
        binding.navAlerts.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
            finish()
        }
        binding.navProfile.setOnClickListener {
            // Already on Profile
        }
    }

    private fun setupMenuItems() {
        binding.menuSavedDevices.setOnClickListener {
            toggleSavedDevices()
        }
        binding.menuMyBookings.setOnClickListener {
            startActivity(Intent(this, MyBookingsActivity::class.java))
            finish()
        }
        binding.menuNotifSettings.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
            finish()
        }
        binding.menuSupport.setOnClickListener {
            startActivity(Intent(this, SupportActivity::class.java))
        }
        binding.menuLogout.setOnClickListener { confirmLogout() }
    }

    private fun toggleSavedDevices() {
        isDevicesExpanded = !isDevicesExpanded
        if (isDevicesExpanded) {
            binding.layoutSavedDevicesList.visibility = View.VISIBLE
            binding.tvSavedDevicesChevron.animate().rotation(90f).setDuration(200).start()
        } else {
            binding.layoutSavedDevicesList.visibility = View.GONE
            binding.tvSavedDevicesChevron.animate().rotation(0f).setDuration(200).start()
        }
    }

    private fun loadProfile() {
        val uid = auth.currentUser?.uid ?: return
        lifecycleScope.launch {
            val user = AppDatabase.userDao.getUser(uid)
            user?.let {
                binding.tvName.text = it.name
                binding.tvEmail.text = it.email
                binding.tvInitial.text = it.name.firstOrNull()?.uppercase() ?: "?"
                binding.tvBookingCount.text = it.bookingCount.toString()
                
                // Hardcoded devices as per user request to match the sketch
                val devices = listOf("iPhone 16 Pro", "Samsung S24 Ultra")
                binding.tvDevices.text = "2"
                populateDevicesList(devices)
            }
        }
    }

    private fun populateDevicesList(devices: List<String>) {
        binding.layoutSavedDevicesList.removeAllViews()
        
        if (devices.isEmpty()) {
            val emptyTv = TextView(this).apply {
                text = "No saved devices"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                setPadding(16.dpToPx(), 8.dpToPx(), 16.dpToPx(), 8.dpToPx())
            }
            binding.layoutSavedDevicesList.addView(emptyTv)
            return
        }

        devices.forEachIndexed { index, deviceName ->
            val itemView = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                
                // Add Device Name
                val textView = TextView(context).apply {
                    text = deviceName
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                    setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                    setPadding(16.dpToPx(), 12.dpToPx(), 16.dpToPx(), 12.dpToPx())
                }
                addView(textView)

                // Add Divider except for the last item
                if (index < devices.size - 1) {
                    val divider = View(context).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            1.dpToPx()
                        ).apply {
                            setMargins(16.dpToPx(), 0, 0, 0)
                        }
                        setBackgroundColor(ContextCompat.getColor(context, R.color.divider))
                    }
                    addView(divider)
                }
            }
            binding.layoutSavedDevicesList.addView(itemView)
        }
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    private fun confirmLogout() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Logout") { _, _ ->
                auth.signOut()
                SessionManager(this).clearSession()
                startActivity(Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}