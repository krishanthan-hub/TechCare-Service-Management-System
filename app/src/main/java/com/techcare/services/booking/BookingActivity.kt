package com.techcare.services.booking

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.techcare.services.R
import com.techcare.services.database.AppDatabase
import com.techcare.services.database.Booking
import com.techcare.services.databinding.ActivityBookingBinding
import com.techcare.services.utils.ValidationUtils
import kotlinx.coroutines.launch

class BookingActivity : AppCompatActivity() {

    private var uploadedPhotoUri: android.net.Uri? = null
    private val photoPickerLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            uploadedPhotoUri = it
            binding.ivUploadedPhoto.setImageURI(it)
            binding.ivUploadedPhoto.visibility = View.VISIBLE
        }
    }

    private lateinit var binding: ActivityBookingBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var selectedMethod = "pickup"
    private var serviceId = ""
    private var serviceName = ""
    private var serviceCategory = ""
    private var servicePrice = 0L
    private var selectedDate = ""
    private var selectedTime = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        serviceId = intent.getStringExtra("serviceId") ?: ""
        serviceName = intent.getStringExtra("serviceName") ?: ""
        serviceCategory = intent.getStringExtra("serviceCategory") ?: ""
        servicePrice = intent.getLongExtra("servicePrice", 0L)

        binding.tvServiceTitle.text = "$serviceName — $serviceCategory"
        binding.btnBack.setOnClickListener { finish() }

        loadDevices()
        setupMethodToggle()

        binding.etDateTime.setOnClickListener { showDateTimeDialog() }
        binding.photoUploadBox.setOnClickListener {
            photoPickerLauncher.launch("image/*")
        }
        binding.btnConfirm.setOnClickListener { validateAndBook() }
    }

    private fun showDateTimeDialog() {
        db.collection("bookings").get()
            .addOnSuccessListener { docs ->
                val map = mutableMapOf<String, MutableList<String>>()
                docs.forEach { doc ->
                    val dt = doc.getString("dateTime") ?: ""
                    val parts = dt.split(" at ")
                    if (parts.size == 2) {
                        val date = parts[0].trim()
                        val time = parts[1].trim()
                        map.getOrPut(date) { mutableListOf() }.add(time)
                    }
                }
                openDialog(map)
            }
            .addOnFailureListener { openDialog(emptyMap()) }
    }

    private fun openDialog(bookedSlots: Map<String, List<String>>) {
        val dialog = DateTimePickerDialog(this, bookedSlots) { date, time ->
            selectedDate = date
            selectedTime = time
            val display = "$date at $time"
            binding.etDateTime.text = display
            binding.etDateTime.setTextColor(getColor(R.color.text_primary))
            binding.tvSelectedDateTime.text = display
            binding.tvSelectedDateTime.visibility = View.VISIBLE
        }
        dialog.show()
    }

    private fun loadDevices() {
        val devices = when (serviceCategory.lowercase()) {
            "mobile" -> listOf("Samsung Galaxy S22", "iPhone 13", "iPhone 12", "Xiaomi Redmi Note 12", "OnePlus 11", "Other Mobile")
            "laptop" -> listOf("Dell Inspiron 15", "HP EliteBook", "Lenovo Legion 5", "Asus VivoBook 15", "Apple MacBook Pro", "Other Laptop")
            "tv" -> listOf("Samsung Smart TV 43\"", "Samsung QLED TV", "LG OLED TV 55\"", "Hisense 55\" TV", "TCL 4K TV 50\"", "Other TV")
            "ac" -> listOf("Samsung Split AC 2.0 Ton", "LG Dual Inverter 1.0 Ton", "Mitsubishi Split AC 2.0 Ton", "Panasonic Inverter 1.5 Ton", "Other AC")
            "fridge" -> listOf("LG Double Door 260L", "Abans Double Door 260L", "Singer Double Door 250L", "Hitachi Double Door 290L", "Other Fridge")
            "washer" -> listOf("Samsung Front Load 7kg", "LG Top Load 8kg", "Singer Top Load 7kg", "Haier Front Load 7kg", "Other Washing Machine")
            else -> listOf("Other Device")
        }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, devices)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerDevice.adapter = adapter
    }

    private fun setupMethodToggle() {
        setPickupSelected()
        binding.btnPickup.setOnClickListener {
            selectedMethod = "pickup"
            setPickupSelected()
        }
        binding.btnDropoff.setOnClickListener {
            selectedMethod = "dropoff"
            setDropoffSelected()
        }
    }

    private fun setPickupSelected() {
        binding.btnPickup.setBackgroundResource(R.drawable.bg_button_primary)
        binding.btnPickup.setTextColor(Color.WHITE)
        binding.btnDropoff.setBackgroundResource(R.drawable.bg_outline_button)
        binding.btnDropoff.setTextColor(Color.parseColor("#1565C0"))
    }

    private fun setDropoffSelected() {
        binding.btnDropoff.setBackgroundResource(R.drawable.bg_button_primary)
        binding.btnDropoff.setTextColor(Color.WHITE)
        binding.btnPickup.setBackgroundResource(R.drawable.bg_outline_button)
        binding.btnPickup.setTextColor(Color.parseColor("#1565C0"))
    }

    private fun saveInitialNotification(uid: String, bookingId: String, serviceName: String) {
        db.collection("notifications").add(mapOf(
            "userId" to uid,
            "title" to "Booking Confirmed! 📋",
            "message" to "Your booking for $serviceName has been confirmed. ID: $bookingId",
            "type" to "booking_confirmed",
            "isRead" to false,
            "timestamp" to System.currentTimeMillis()
        ))
    }

    /**
     * Automatically assigns a technician after 2 minutes of booking.
     * Updates status in Firestore and adds a notification entry.
     */
    private fun scheduleTechnicianAutoAssignment(
        uid: String,
        bookingId: String,
        serviceName: String,
        category: String
    ) {
        Handler(Looper.getMainLooper()).postDelayed({

            db.collection("bookings")
                .document(bookingId)
                .get()
                .addOnSuccessListener { snapshot ->

                    if (snapshot.exists() &&
                        snapshot.getString("status") == "Received"
                    ) {

                        val technicianId = when (category.lowercase()) {
                            "mobile" -> "tech001"
                            "laptop" -> "tech002"
                            "tv" -> "tech003"
                            "ac" -> "tech004"
                            "fridge" -> "tech005"
                            "washer" -> "tech006"
                            else -> "tech001"
                        }

                        db.collection("bookings")
                            .document(bookingId)
                            .update(
                                mapOf(
                                    "status" to "Technician Assigned",
                                    "technicianId" to technicianId
                                )
                            )
                            .addOnSuccessListener {

                                // Create only ONE notification
                                db.collection("notifications").add(
                                    mapOf(
                                        "userId" to uid,
                                        "bookingId" to bookingId,
                                        "status" to "Technician Assigned",
                                        "title" to "Repair Update: Technician Assigned",
                                        "message" to "Your $serviceName status has been updated to: Technician Assigned",
                                        "type" to "repair_update",
                                        "isRead" to false,
                                        "timestamp" to System.currentTimeMillis()
                                    )
                                )
                            }
                    }
                }

        }, 120000)
    }
    private fun validateAndBook() {
        val device = binding.spinnerDevice.selectedItem?.toString() ?: ""
        val issue = binding.etIssue.text.toString().trim()
        var isValid = true

        if (!ValidationUtils.isValidIssue(issue)) {
            binding.tilIssue.error = "Please describe the issue (at least 10 characters)"
            isValid = false
        } else binding.tilIssue.error = null

        if (selectedDate.isEmpty() || selectedTime.isEmpty()) {
            Toast.makeText(this, "Please select date and time", Toast.LENGTH_SHORT).show()
            isValid = false
        }
        if (!isValid) return

        setLoading(true)

        val uid = auth.currentUser?.uid ?: return
        val bookingId = "TC-${System.currentTimeMillis()}"
        val dateTime = "$selectedDate at $selectedTime"
        
        val booking = Booking(
            id = bookingId, userId = uid, serviceId = serviceId,
            serviceName = serviceName, deviceName = device, issue = issue,
            method = selectedMethod, dateTime = dateTime,
            status = "Received", cost = servicePrice,
            technicianId = "", 
            estimatedCompletion = "Within 3-5 business days"
        )

        lifecycleScope.launch {
            val success = AppDatabase.bookingDao.createBooking(booking)
            if (success) {
                AppDatabase.userDao.updateBookingCount(uid)
                saveInitialNotification(uid, bookingId, serviceName)
                
                // Firestore write
                db.collection("bookings").document(bookingId).set(mapOf(
                    "id" to bookingId,
                    "userId" to uid,
                    "serviceId" to serviceId,
                    "serviceName" to serviceName,
                    "deviceName" to device,
                    "issue" to issue,
                    "method" to selectedMethod,
                    "dateTime" to dateTime,
                    "status" to "Received",
                    "cost" to servicePrice,
                    "technicianId" to "",
                    "createdAt" to System.currentTimeMillis()
                )).addOnSuccessListener {
                    // Re-start the 2-minute timer for auto technician assignment
                    scheduleTechnicianAutoAssignment(uid, bookingId, serviceName, serviceCategory)
                }

                setLoading(false)
                startActivity(
                    Intent(this@BookingActivity, ConfirmationActivity::class.java).apply {
                        putExtra("bookingId", bookingId)
                        putExtra("serviceName", serviceName)
                        putExtra("deviceName", device)
                        putExtra("method", selectedMethod)
                        putExtra("dateTime", dateTime)
                        putExtra("cost", servicePrice)
                    }
                )
                finish()
            } else {
                setLoading(false)
                Toast.makeText(this@BookingActivity, "Booking failed. Check internet.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.btnConfirm.isEnabled = !loading
        binding.btnConfirm.text = if (loading) "Booking..." else "CONFIRM BOOKING"
    }
}
