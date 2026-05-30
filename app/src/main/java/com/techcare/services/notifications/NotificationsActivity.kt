package com.techcare.services.notifications

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.techcare.services.booking.MyBookingsActivity
import com.techcare.services.databinding.ActivityNotificationsBinding
import com.techcare.services.databinding.ItemNotificationBinding
import com.techcare.services.home.HomeActivity
import com.techcare.services.profile.ProfileActivity
import com.techcare.services.services.ServicesActivity
import java.util.Calendar

data class NotificationItem(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "",
    val isRead: Boolean = false,
    val timestamp: Long = 0L
)

sealed class NotifListItem {
    data class Header(val label: String) : NotifListItem()
    data class Item(val notif: NotificationItem) : NotifListItem()
}

class NotificationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationsBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var notifAdapter: NotifAdapter
    private val lastProcessedStatus = mutableMapOf<String, String>()

    private val statusesToNotify = listOf(
        "Under Repair",
        "Ready for Pickup",
        "Completed"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        loadNotifications()
        setupNavigation()
        startStatusObserver()

        binding.tvMarkAllRead.setOnClickListener { markAllRead() }
    }

    private fun setupRecyclerView() {
        notifAdapter = NotifAdapter(emptyList())
        binding.rvNotifications.layoutManager = LinearLayoutManager(this)
        binding.rvNotifications.adapter = notifAdapter
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
            startActivity(Intent(this, MyBookingsActivity::class.java))
        }

        binding.navAlerts.setOnClickListener { }

        binding.navProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    private fun loadNotifications() {
        val uid = auth.currentUser?.uid ?: return

        binding.tvEmpty.visibility = View.VISIBLE
        binding.tvEmpty.text = "Loading..."
        binding.rvNotifications.visibility = View.GONE

        db.collection("notifications")
            .whereEqualTo("userId", uid)
            .addSnapshotListener(this) { docs, error ->
                if (error != null) {
                    binding.tvEmpty.text = "Error: ${error.message}"
                    return@addSnapshotListener
                }

                if (docs == null || docs.isEmpty) {
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.tvEmpty.text = "No notifications yet."
                    binding.rvNotifications.visibility = View.GONE
                    return@addSnapshotListener
                }

                val list = docs.map { doc ->
                    NotificationItem(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        message = doc.getString("message") ?: "",
                        type = doc.getString("type") ?: "",
                        isRead = doc.getBoolean("isRead") ?: false,
                        timestamp = doc.getLong("timestamp") ?: 0L
                    )
                }.sortedByDescending { it.timestamp }

                binding.tvEmpty.visibility = View.GONE
                binding.rvNotifications.visibility = View.VISIBLE
                notifAdapter.updateList(groupByDay(list))
            }
    }

    private fun startStatusObserver() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("bookings")
            .whereEqualTo("userId", uid)
            .addSnapshotListener(this) { snapshots, _ ->
                if (snapshots == null) return@addSnapshotListener

                for (dc in snapshots.documentChanges) {
                    val doc = dc.document
                    val bookingId = doc.id
                    val status = doc.getString("status")?.trim() ?: ""
                    val serviceName = doc.getString("serviceName") ?: "Repair"

                    if (status.equals("Technician Assigned", true)) {
                        lastProcessedStatus[bookingId] = status
                        continue
                    }

                    if (dc.type == DocumentChange.Type.ADDED) {
                        lastProcessedStatus[bookingId] = status

                        if (statusesToNotify.any { it.equals(status, true) }) {
                            createStatusNotificationIfMissing(uid, bookingId, serviceName, status)
                        }
                    }

                    if (dc.type == DocumentChange.Type.MODIFIED) {
                        val previousStatus = lastProcessedStatus[bookingId]

                        if (previousStatus == status) continue
                        lastProcessedStatus[bookingId] = status

                        if (statusesToNotify.any { it.equals(status, true) }) {
                            createStatusNotificationIfMissing(uid, bookingId, serviceName, status)
                        }
                    }
                }
            }
    }

    private fun createStatusNotificationIfMissing(
        uid: String,
        bookingId: String,
        serviceName: String,
        status: String
    ) {
        if (status.equals("Technician Assigned", true)) return

        db.collection("notifications")
            .whereEqualTo("userId", uid)
            .whereEqualTo("bookingId", bookingId)
            .whereEqualTo("status", status)
            .get()
            .addOnSuccessListener { existing ->
                if (existing.isEmpty) {
                    db.collection("notifications").add(
                        mapOf(
                            "userId" to uid,
                            "bookingId" to bookingId,
                            "status" to status,
                            "title" to "Repair Update: $status",
                            "message" to "Your $serviceName status has been updated to: $status",
                            "type" to "repair_update",
                            "timestamp" to System.currentTimeMillis(),
                            "isRead" to false
                        )
                    )
                }
            }
    }

    private fun groupByDay(list: List<NotificationItem>): List<NotifListItem> {
        val result = mutableListOf<NotifListItem>()

        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val yesterday = today - 86400000L
        val thisWeek = today - 518400000L

        val sections = listOf(
            "Today" to list.filter { it.timestamp >= today },
            "Yesterday" to list.filter { it.timestamp in yesterday until today },
            "This Week" to list.filter { it.timestamp in thisWeek until yesterday },
            "Older" to list.filter { it.timestamp < thisWeek }
        )

        sections.forEach { (label, items) ->
            if (items.isNotEmpty()) {
                result.add(NotifListItem.Header(label))
                items.forEach { result.add(NotifListItem.Item(it)) }
            }
        }

        return result
    }

    private fun markAllRead() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("notifications")
            .whereEqualTo("userId", uid)
            .whereEqualTo("isRead", false)
            .get()
            .addOnSuccessListener { docs ->
                val batch = db.batch()
                docs.forEach { batch.update(it.reference, "isRead", true) }
                batch.commit()
            }
    }

    inner class NotifAdapter(private var list: List<NotifListItem>) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val TYPE_HEADER = 0
        private val TYPE_ITEM = 1

        inner class HeaderVH(parent: ViewGroup) : RecyclerView.ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_1, parent, false)
        ) {
            val tv = itemView.findViewById<android.widget.TextView>(android.R.id.text1)
        }

        inner class ItemVH(val b: ItemNotificationBinding) :
            RecyclerView.ViewHolder(b.root)

        override fun getItemViewType(position: Int): Int {
            return if (list[position] is NotifListItem.Header) TYPE_HEADER else TYPE_ITEM
        }

        override fun getItemCount() = list.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return if (viewType == TYPE_HEADER) {
                HeaderVH(parent)
            } else {
                ItemVH(
                    ItemNotificationBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (val item = list[position]) {
                is NotifListItem.Header -> {
                    (holder as HeaderVH).tv.apply {
                        text = item.label
                        textSize = 13f
                        setTextColor(Color.GRAY)
                        setPadding(32, 24, 32, 8)
                        setTypeface(null, android.graphics.Typeface.BOLD)
                    }
                }

                is NotifListItem.Item -> {
                    val h = holder as ItemVH
                    val notif = item.notif

                    with(h.b) {
                        tvTitle.text = notif.title
                        tvMessage.text = notif.message

                        tvTime.text = if (notif.timestamp > 0) {
                            DateUtils.getRelativeTimeSpanString(
                                notif.timestamp,
                                System.currentTimeMillis(),
                                DateUtils.SECOND_IN_MILLIS
                            )
                        } else {
                            ""
                        }

                        unreadDot.visibility = if (notif.isRead) View.GONE else View.VISIBLE

                        cardNotif.setCardBackgroundColor(
                            if (notif.isRead) Color.WHITE else Color.parseColor("#EBF3FB")
                        )
                    }
                }
            }
        }

        fun updateList(newList: List<NotifListItem>) {
            val diffCallback = object : DiffUtil.Callback() {
                override fun getOldListSize() = list.size
                override fun getNewListSize() = newList.size

                override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean {
                    val old = list[oldPos]
                    val new = newList[newPos]

                    return if (old is NotifListItem.Item && new is NotifListItem.Item) {
                        old.notif.id == new.notif.id
                    } else if (old is NotifListItem.Header && new is NotifListItem.Header) {
                        old.label == new.label
                    } else {
                        false
                    }
                }

                override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean {
                    return list[oldPos] == newList[newPos]
                }
            }

            val diffResult = DiffUtil.calculateDiff(diffCallback)
            list = newList
            diffResult.dispatchUpdatesTo(this)
        }
    }
}