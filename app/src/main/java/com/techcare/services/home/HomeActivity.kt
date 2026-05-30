package com.techcare.services.home

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.techcare.services.R
import com.techcare.services.booking.BookingActivity
import com.techcare.services.booking.MyBookingsActivity
import com.techcare.services.booking.TrackRepairActivity
import com.techcare.services.database.AppDatabase
import com.techcare.services.databinding.ActivityHomeBinding
import com.techcare.services.notifications.NotificationsActivity
import com.techcare.services.profile.ProfileActivity
import com.techcare.services.services.ServiceAdapter
import com.techcare.services.services.ServiceItem
import com.techcare.services.services.ServicesActivity
import kotlinx.coroutines.launch
import java.util.Calendar

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var allServices = listOf<ServiceItem>()
    private val lastProcessedStatus = mutableMapOf<String, String>()
    
    private lateinit var popularAdapter: ServiceAdapter
    private lateinit var searchAdapter: ServiceAdapter

    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null
    
    private var activeBookingListener: ListenerRegistration? = null
    private var statusListener: ListenerRegistration? = null

    private val CHANNEL_ID = "repair_updates"

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _: Boolean -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)


        checkNotificationPermission()
        createNotificationChannel()
        setupRecyclerViews()
        setupGreeting()
        loadAllServices()
        setupNavigation()
        setupDeviceChips()
        setupSearch()
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun setupRecyclerViews() {
        popularAdapter = ServiceAdapter(emptyList()) { service -> openBooking(service) }
        binding.rvServices.layoutManager = LinearLayoutManager(this)
        binding.rvServices.adapter = popularAdapter
        binding.rvServices.setHasFixedSize(false)

        searchAdapter = ServiceAdapter(emptyList()) { service ->
            binding.searchDropdown.visibility = View.GONE
            binding.etSearch.clearFocus()
            binding.etSearch.setText("")
            openBooking(service)
        }
        binding.rvSearchResults.layoutManager = LinearLayoutManager(this)
        binding.rvSearchResults.adapter = searchAdapter
    }

    private fun openBooking(service: ServiceItem) {
        startActivity(Intent(this, BookingActivity::class.java).apply {
            putExtra("serviceId", service.id)
            putExtra("serviceName", service.name)
            putExtra("serviceCategory", service.category)
            putExtra("servicePrice", service.price)
        })
    }

    override fun onResume() {
        super.onResume()
        // Re-attach real-time observers to handle updates correctly
        startActiveBookingObserver()
        startStatusObserver()
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchRunnable?.let { searchHandler.removeCallbacks(it) }
                searchRunnable = Runnable { 
                    performSearch(s.toString().trim().lowercase())
                }
                searchHandler.postDelayed(searchRunnable!!, 300)
            }
        })

        binding.etSearch.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) binding.searchDropdown.visibility = View.GONE
        }
    }

    private fun performSearch(query: String) {
        if (query.isEmpty()) {
            binding.searchDropdown.visibility = View.GONE
            binding.tvNoResults.visibility = View.GONE
            return
        }

        val filtered = allServices.filter {
            it.name.lowercase().contains(query) ||
            it.category.lowercase().contains(query) ||
            it.description.lowercase().contains(query)
        }

        binding.searchDropdown.visibility = View.VISIBLE
        binding.searchDropdown.bringToFront()

        if (filtered.isEmpty()) {
            binding.tvNoResults.visibility = View.VISIBLE
            binding.rvSearchResults.visibility = View.GONE
        } else {
            binding.tvNoResults.visibility = View.GONE
            binding.rvSearchResults.visibility = View.VISIBLE
            searchAdapter.updateList(filtered)
        }
    }

    private fun loadAllServices() {
        val popularKeywords = listOf("Screen Replacement", "Battery Replacement")

        db.collection("services").addSnapshotListener(this) { docs, _ ->
            if (docs == null) return@addSnapshotListener
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
            
            var popular = allServices.filter { service ->
                popularKeywords.any { word -> service.name.contains(word, ignoreCase = true) }
            }

            if (popular.isEmpty() && allServices.isNotEmpty()) {
                popular = allServices.take(2)
            }

            popularAdapter.updateList(popular)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupGreeting() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            if (doc != null && doc.exists()) {
                val firstName = doc.getString("name")?.split(" ")?.first() ?: "there"
                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                val time = when {
                    hour < 12 -> "Morning"
                    hour < 17 -> "Afternoon"
                    else -> "Evening"
                }
                binding.tvGreeting.text = "Good $time, $firstName!"
                binding.tvAvatar.text = firstName.firstOrNull()?.uppercase() ?: "U"
            }
        }
    }

    private fun setupNavigation() {
        binding.navHome.setOnClickListener { /* Already here */ }
        binding.navServices.setOnClickListener {
            startActivity(Intent(this, ServicesActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT })
        }
        binding.navBookings.setOnClickListener {
            startActivity(Intent(this, MyBookingsActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT })
        }
        binding.navAlerts.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT })
        }
        binding.navProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT })
        }
    }

    @SuppressLint("SetTextI18n")
    private fun startActiveBookingObserver() {
        val uid = auth.currentUser?.uid ?: return
        
        activeBookingListener?.remove()
        
        activeBookingListener = db.collection("bookings")
            .whereEqualTo("userId", uid)
            .whereIn("status", listOf("Received", "Technician Assigned", "Under Repair", "Ready for Pickup"))
            .addSnapshotListener(this) { snapshots, _ ->
                if (snapshots != null && !snapshots.isEmpty) {
                    val doc = snapshots.documents[0]
                    binding.cardActiveBooking.visibility = View.VISIBLE
                    binding.tvActiveDevice.text = "${doc.getString("serviceName")} — ${doc.getString("deviceName")}"
                    binding.tvActiveStatus.text = "Status: ${doc.getString("status")}"
                    binding.tvTrackLink.setOnClickListener {
                        startActivity(Intent(this, TrackRepairActivity::class.java).apply {
                            putExtra("bookingId", doc.id)
                            putExtra("serviceName", doc.getString("serviceName"))
                            putExtra("deviceName", doc.getString("deviceName"))
                            putExtra("status", doc.getString("status"))
                        })
                    }
                } else {
                    binding.cardActiveBooking.visibility = View.GONE
                }
            }
    }

    private fun startStatusObserver() {
        val uid = auth.currentUser?.uid ?: return
        
        statusListener?.remove()
        
        statusListener = db.collection("bookings")
            .whereEqualTo("userId", uid)
            .addSnapshotListener(this) { snapshots, _ ->
                if (snapshots == null) return@addSnapshotListener
                for (dc in snapshots.documentChanges) {
                    val doc = dc.document
                    val status = doc.getString("status") ?: ""
                    val serviceName = doc.getString("serviceName") ?: "Repair"

                    if (dc.type == DocumentChange.Type.MODIFIED) {
                        if (lastProcessedStatus[doc.id] == status) continue
                        lastProcessedStatus[doc.id] = status

                        val title = "Repair Update: $status"
                        val message = "Your $serviceName status has been updated to: $status"
                        
                        showSystemNotification(title, message)
                        createNotification(uid, title, message)
                        syncLocalDatabase(doc.id, status)
                    } else if (dc.type == DocumentChange.Type.ADDED) {
                        lastProcessedStatus[doc.id] = status
                    }
                }
            }
    }

    private fun syncLocalDatabase(bookingId: String, status: String) {
        lifecycleScope.launch {
            val booking = AppDatabase.bookingDao.getBookingById(bookingId)
            booking?.let {
                AppDatabase.bookingDao.createBooking(it.copy(status = status))
            }
        }
    }

    private fun showSystemNotification(title: String, message: String) {
        val intent = Intent(this, NotificationsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_nav_alerts)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), builder.build())
            }
        } catch (e: SecurityException) {
            // Permission missing
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Repair Updates"
            val descriptionText = "Notifications for status changes of your repairs"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(uid: String, title: String, message: String) {
        db.collection("notifications").add(mapOf(
            "userId" to uid, 
            "title" to title, 
            "message" to message,
            "type" to "repair_update",
            "timestamp" to System.currentTimeMillis(), 
            "isRead" to false
        ))
    }

    private fun setupDeviceChips() {
        val chips = mapOf(
            binding.chipMobile to "Mobile",
            binding.chipLaptop to "Laptop",
            binding.chipTV to "TV",
            binding.chipAC to "AC",
            binding.chipFridge to "Fridge",
            binding.chipWasher to "Washer"
        )
        chips.forEach { (chip, category) ->
            chip.setOnClickListener {
                startActivity(Intent(this, ServicesActivity::class.java).apply {
                    putExtra("category", category)
                    flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                })
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        activeBookingListener?.remove()
        statusListener?.remove()
    }
}
