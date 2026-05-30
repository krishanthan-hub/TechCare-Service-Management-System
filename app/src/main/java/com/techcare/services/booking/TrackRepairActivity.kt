package com.techcare.services.booking

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.techcare.services.databinding.ActivityTrackRepairBinding
import com.techcare.services.support.SupportActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TrackRepairActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTrackRepairBinding
    private val firestore = FirebaseFirestore.getInstance()
    private var currentBookingId = ""
    private var currentStatus = ""

    private val steps = listOf(
        "Received",
        "Technician Assigned",
        "Under Repair",
        "Ready for Pickup",
        "Completed"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrackRepairBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnContactSupport.setOnClickListener {
            startActivity(Intent(this, SupportActivity::class.java))
        }

        binding.btnCancelBooking.setOnClickListener {
            showCancelConfirmation()
        }

        val bookingId = intent.getStringExtra("bookingId") ?: ""
        if (bookingId.isNotEmpty()) {
            currentBookingId = bookingId
            listenToBookingUpdates(bookingId)
        }
    }

    private fun listenToBookingUpdates(bookingId: String) {
        firestore.collection("bookings").document(bookingId)
            .addSnapshotListener(this) { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                
                val serviceName = snapshot.getString("serviceName") ?: ""
                val deviceName = snapshot.getString("deviceName") ?: ""
                val status = snapshot.getString("status") ?: "Received"
                
                currentStatus = status
                showBookingDetails(bookingId, serviceName, deviceName, status)
            }
    }

    private fun showBookingDetails(
        bookingId: String,
        serviceName: String,
        deviceName: String,
        status: String
    ) {
        currentStatus = status
        val shortId = if (bookingId.length > 16)
            "#TC-${bookingId.takeLast(13)}" else "#$bookingId"

        binding.tvBookingRef.text = shortId
        binding.tvServiceDevice.text = "$serviceName — $deviceName"
        binding.tvEstCompletion.text = "Est. Completion: Within 3-5 business days"
        binding.tvStatusBadge.text = status

        val badgeColor = when (status.lowercase()) {
            "received"            -> Color.parseColor("#1565C0")
            "technician assigned" -> Color.parseColor("#6A1B9A")
            "under repair"        -> Color.parseColor("#E65100")
            "ready for pickup"    -> Color.parseColor("#FF9800")
            "completed"           -> Color.parseColor("#2E7D32")
            "cancelled"           -> Color.parseColor("#757575")
            else                  -> Color.parseColor("#1565C0")
        }
        (binding.tvStatusBadge.background as? GradientDrawable)?.setColor(badgeColor)

        buildTimeline(status)

        // Show technician card if assigned or beyond
        val techSteps = listOf(
            "technician assigned", "under repair",
            "ready for pickup", "completed"
        )
        if (status.lowercase() in techSteps) {
            binding.cardTechnician.visibility = View.VISIBLE
            loadTechnician(bookingId)
        } else {
            binding.cardTechnician.visibility = View.GONE
        }

        // Show cancel button only if not started yet
        if (status.equals("Received", true) || status.equals("Technician Assigned", true)) {
            binding.btnCancelBooking.visibility = View.VISIBLE
        } else {
            binding.btnCancelBooking.visibility = View.GONE
        }
    }

    private fun showCancelConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Cancel Booking")
            .setMessage("Are you sure you want to cancel this repair booking?")
            .setPositiveButton("Yes, Cancel") { _, _ ->
                cancelBooking()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun cancelBooking() {
        if (currentBookingId.isEmpty()) return

        firestore.collection("bookings").document(currentBookingId)
            .update("status", "Cancelled")
            .addOnSuccessListener {
                Toast.makeText(this, "Booking cancelled successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to cancel: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadTechnician(bookingId: String) {
        firestore.collection("bookings")
            .document(bookingId)
            .get()
            .addOnSuccessListener { bookingDoc ->
                val techId = bookingDoc.getString("technicianId")
                if (!techId.isNullOrEmpty()) {
                    firestore.collection("technicians")
                        .document(techId)
                        .get()
                        .addOnSuccessListener { techDoc ->
                            val name      = techDoc.getString("name") ?: "TechCare Technician"
                            
                            val rawRating = techDoc.get("rating")
                            val rating = when (rawRating) {
                                is String -> rawRating
                                is Number -> rawRating.toDouble().toString()
                                else -> "0.0"
                            }

                            val specialty = techDoc.getString("specialty") ?: "Repair Specialist"
                            val phone     = techDoc.getString("phone") ?: ""
                            val initials  = techDoc.getString("initials") ?: "TC"

                            binding.tvTechInitials.text = initials
                            binding.tvTechName.text = name
                            binding.tvTechInfo.text = "⭐ $rating  |  $specialty"

                            binding.btnCallTech.setOnClickListener {
                                try {
                                    startActivity(Intent(
                                        Intent.ACTION_DIAL,
                                        android.net.Uri.parse("tel:$phone")
                                    ))
                                } catch (e: Exception) {
                                    Toast.makeText(this, phone, Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                        .addOnFailureListener {
                            setDefaultTechnician()
                        }
                } else {
                    setDefaultTechnician()
                }
            }
            .addOnFailureListener {
                setDefaultTechnician()
            }
    }

    private fun setDefaultTechnician() {
        binding.tvTechInitials.text = "TC"
        binding.tvTechName.text = "To be assigned"
        binding.tvTechInfo.text = "Will be assigned soon"
    }

    private fun buildTimeline(currentStatus: String) {
        binding.timelineContainer.removeAllViews()
        
        // If cancelled, show a simplified or empty timeline
        if (currentStatus.equals("Cancelled", true)) {
            val tv = TextView(this).apply {
                text = "This booking has been cancelled."
                setTextColor(Color.RED)
                gravity = Gravity.CENTER
                setPadding(0, 40, 0, 40)
            }
            binding.timelineContainer.addView(tv)
            return
        }

        // Find the index case-insensitively
        val currentStep = steps.indexOfFirst { it.equals(currentStatus, true) }
            .let { if (it == -1) 0 else it }

        val now = System.currentTimeMillis()
        val stepTimes = listOf(
            now - 3600000 * 5,
            now - 3600000 * 3,
            now - 3600000 * 1,
            now + 3600000 * 5,
            now + 3600000 * 10
        )

        steps.forEachIndexed { index, step ->
            val isDone = index <= currentStep

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.TOP
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 4 }
            }

            val leftCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(40, LinearLayout.LayoutParams.WRAP_CONTENT)
            }

            val dotSize = resources.getDimensionPixelSize(android.R.dimen.app_icon_size) / 2
            val dot = TextView(this).apply {
                text = if (isDone) "✓" else ""
                textSize = 11f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(dotSize, dotSize)
                background = androidx.core.content.ContextCompat.getDrawable(
                    context,
                    if (isDone) com.techcare.services.R.drawable.bg_circle_green
                    else com.techcare.services.R.drawable.bg_circle_grey
                )
            }
            leftCol.addView(dot)

            if (index < steps.size - 1) {
                val line = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(4, 70).apply {
                        gravity = Gravity.CENTER_HORIZONTAL
                        topMargin = 2
                        bottomMargin = 2
                    }
                    setBackgroundColor(
                        if (isDone) getColor(com.techcare.services.R.color.success)
                        else getColor(com.techcare.services.R.color.divider)
                    )
                }
                leftCol.addView(line)
            }

            val rightCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                ).apply {
                    marginStart = 16
                    bottomMargin = if (index < steps.size - 1) 8 else 0
                }
            }

            val stepTitle = TextView(this).apply {
                text = step
                textSize = 14f
                setTextColor(
                    if (isDone) getColor(com.techcare.services.R.color.text_primary)
                    else getColor(com.techcare.services.R.color.text_secondary)
                )
                if (index == currentStep) setTypeface(null, Typeface.BOLD)
            }
            rightCol.addView(stepTitle)

            if (isDone) {
                val timeText = TextView(this).apply {
                    text = formatTime(stepTimes[index])
                    textSize = 11f
                    setTextColor(getColor(com.techcare.services.R.color.text_secondary))
                }
                rightCol.addView(timeText)
            }

            row.addView(leftCol)
            row.addView(rightCol)
            binding.timelineContainer.addView(row)
        }
    }

    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM, h:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}