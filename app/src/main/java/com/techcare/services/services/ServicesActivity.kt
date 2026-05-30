package com.techcare.services.services

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.techcare.services.R
import com.techcare.services.booking.BookingActivity
import com.techcare.services.booking.MyBookingsActivity
import com.techcare.services.databinding.ActivityServicesBinding
import com.techcare.services.home.HomeActivity
import com.techcare.services.notifications.NotificationsActivity
import com.techcare.services.profile.ProfileActivity

class ServicesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityServicesBinding
    private val db = FirebaseFirestore.getInstance()
    private var allServices = listOf<ServiceItem>()
    private val categories = listOf("All", "Mobile", "Laptop", "TV", "AC", "Fridge", "Washer")
    private var selectedCategory = "All"
    private val chipViews = mutableListOf<TextView>()
    private lateinit var serviceAdapter: ServiceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityServicesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        
        setupRecyclerView()
        setupChips()
        loadServices()
        setupNavigation()

        val category = intent.getStringExtra("category")
        if (!category.isNullOrEmpty()) {
            selectedCategory = category
            updateChipStates()
        }
    }

    private fun setupRecyclerView() {
        serviceAdapter = ServiceAdapter(emptyList()) { service ->
            startActivity(Intent(this, BookingActivity::class.java).apply {
                putExtra("serviceId", service.id)
                putExtra("serviceName", service.name)
                putExtra("serviceCategory", service.category)
                putExtra("servicePrice", service.price)
            })
        }
        binding.rvServices.layoutManager = LinearLayoutManager(this)
        binding.rvServices.adapter = serviceAdapter
    }

    private fun setupNavigation() {
        binding.navHome.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            })
        }
        binding.navServices.setOnClickListener { /* already here */ }
        binding.navBookings.setOnClickListener {
            startActivity(Intent(this, MyBookingsActivity::class.java))
        }
        binding.navAlerts.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }
        binding.navProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    private fun setupChips() {
        categories.forEach { category ->
            val chip = TextView(this).apply {
                text = category
                textSize = 12f
                gravity = Gravity.CENTER
                val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginEnd = 1
                    marginStart = 1
                }
                layoutParams = params
                setPadding(0, 24, 0, 24)
                setOnClickListener {
                    selectedCategory = category
                    updateChipStates()
                    filterByCategory(category)
                }
            }
            chipViews.add(chip)
            binding.chipGroup.addView(chip)
        }
        updateChipStates()
    }

    private fun updateChipStates() {
        chipViews.forEachIndexed { index, chip ->
            val isSelected = categories[index] == selectedCategory
            if (isSelected) {
                chip.setTextColor(Color.WHITE)
                chip.background = getDrawable(R.drawable.bg_button_primary)
                chip.setTypeface(null, Typeface.BOLD)
            } else {
                chip.setTextColor(Color.parseColor("#1565C0"))
                chip.background = getDrawable(R.drawable.bg_outline_button)
                chip.setTypeface(null, Typeface.NORMAL)
            }
        }
    }

    private fun filterByCategory(category: String) {
        val filtered = if (category == "All") allServices
        else allServices.filter { it.category == category }
        serviceAdapter.updateList(filtered)
    }

    private fun loadServices() {
        db.collection("services").get().addOnSuccessListener { docs ->
            allServices = docs.map { doc ->
                ServiceItem(
                    id = doc.id,
                    name = doc.getString("name") ?: "",
                    category = doc.getString("category") ?: "",
                    description = doc.getString("description") ?: "",
                    price = doc.getLong("price") ?: 0,
                    rating = doc.getDouble("rating") ?: 0.0,
                    icon = doc.getString("icon") ?: "🔧"
                )
            }
            
            if (selectedCategory != "All") {
                filterByCategory(selectedCategory)
            } else {
                serviceAdapter.updateList(allServices)
            }
        }
    }
}