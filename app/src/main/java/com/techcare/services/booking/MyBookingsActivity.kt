package com.techcare.services.booking

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.techcare.services.R
import com.techcare.services.database.Booking
import com.techcare.services.databinding.ActivityMyBookingsBinding
import com.techcare.services.home.HomeActivity
import com.techcare.services.notifications.NotificationsActivity
import com.techcare.services.profile.ProfileActivity
import com.techcare.services.services.ServicesActivity

class MyBookingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyBookingsBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var allBookings = listOf<Booking>()
    private lateinit var bookingAdapter: BookingAdapter
    private var selectedTabIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyBookingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        setupRecyclerView()
        setupTabs()
        setupNavigation()
        loadBookings()
    }

    private fun setupRecyclerView() {
        bookingAdapter = BookingAdapter(emptyList())
        binding.rvBookings.layoutManager = LinearLayoutManager(this)
        binding.rvBookings.adapter = bookingAdapter
    }

    private fun loadBookings() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("bookings")
            .whereEqualTo("userId", uid)
            .addSnapshotListener(this) { docs, error ->
                if (error != null || docs == null) return@addSnapshotListener

                allBookings = docs.mapNotNull { doc ->
                    doc.toObject(Booking::class.java)
                }.sortedByDescending { it.createdAt }

                refreshCurrentTab()
            }
    }

    private fun setupTabs() {
        binding.tabActive.setOnClickListener {
            selectedTabIndex = 0
            refreshCurrentTab()
        }

        binding.tabCompleted.setOnClickListener {
            selectedTabIndex = 1
            refreshCurrentTab()
        }

        binding.tabCancelled.setOnClickListener {
            selectedTabIndex = 2
            refreshCurrentTab()
        }
    }

    private fun refreshCurrentTab() {
        setTabSelected(selectedTabIndex)

        val filteredList = when (selectedTabIndex) {
            0 -> filterActive(allBookings)
            1 -> allBookings.filter { it.status.trim().equals("Completed", ignoreCase = true) }
            2 -> allBookings.filter { it.status.trim().equals("Cancelled", ignoreCase = true) }
            else -> filterActive(allBookings)
        }

        showBookings(filteredList)
    }

    private fun filterActive(list: List<Booking>): List<Booking> {
        return list.filter {
            val status = it.status.trim()
            !status.equals("Completed", ignoreCase = true) &&
                    !status.equals("Cancelled", ignoreCase = true)
        }
    }

    private fun setTabSelected(index: Int) {
        val tabs = listOf(binding.tabActive, binding.tabCompleted, binding.tabCancelled)

        tabs.forEachIndexed { i, tab ->
            if (i == index) {
                tab.setTextColor(getColor(R.color.primary))
                tab.setBackgroundColor(getColor(R.color.accent_light))
            } else {
                tab.setTextColor(getColor(R.color.text_secondary))
                tab.setBackgroundColor(getColor(R.color.white))
            }
        }
    }

    private fun showBookings(list: List<Booking>) {
        if (list.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.rvBookings.visibility = View.GONE
        } else {
            binding.tvEmpty.visibility = View.GONE
            binding.rvBookings.visibility = View.VISIBLE
        }

        bookingAdapter.updateList(list)
    }

    private fun setupNavigation() {
        binding.navHome.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            })
        }

        binding.navServices.setOnClickListener {
            startActivity(Intent(this, ServicesActivity::class.java))
        }

        binding.navBookings.setOnClickListener {
            // Already on bookings screen
        }

        binding.navAlerts.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }

        binding.navProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }
}