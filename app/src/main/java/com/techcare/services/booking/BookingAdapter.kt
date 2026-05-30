package com.techcare.services.booking

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.techcare.services.database.Booking
import com.techcare.services.databinding.ItemBookingBinding

class BookingAdapter(private var list: List<Booking>) :
    RecyclerView.Adapter<BookingAdapter.VH>() {

    inner class VH(val b: ItemBookingBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemBookingBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val booking = list[position]
        with(holder.b) {
            tvBookingId.text = booking.id
            tvServiceName.text = booking.serviceName
            tvDeviceName.text = booking.deviceName
            tvStatus.text = booking.status
            tvDateTime.text = booking.dateTime
            tvCost.text = "LKR ${"%,d".format(booking.cost)}"

            val progress = when (booking.status) {
                "Received" -> 1
                "Technician Assigned" -> 2
                "Under Repair" -> 3
                "Ready for Pickup" -> 4
                "Completed" -> 5
                else -> 0
            }
            progressBar.progress = progress

            // Badge color
            val badgeColor = when (booking.status) {
                "Received" -> android.graphics.Color.parseColor("#1565C0")
                "Technician Assigned" -> android.graphics.Color.parseColor("#6A1B9A")
                "Under Repair" -> android.graphics.Color.parseColor("#E65100")
                "Ready for Pickup" -> android.graphics.Color.parseColor("#FF9800")
                "Completed" -> android.graphics.Color.parseColor("#2E7D32")
                "Cancelled" -> android.graphics.Color.parseColor("#C62828")
                else -> android.graphics.Color.parseColor("#666666")
            }
            try {
                (tvStatus.background as? android.graphics.drawable.GradientDrawable)
                    ?.setColor(badgeColor)
            } catch (e: Exception) { }

            // Track button — navigate to TrackRepairActivity
            btnTrack.setOnClickListener {
                val context = holder.itemView.context
                val intent = android.content.Intent(context, TrackRepairActivity::class.java).apply {
                    putExtra("bookingId", booking.id)
                    putExtra("serviceName", booking.serviceName)
                    putExtra("deviceName", booking.deviceName)
                    putExtra("status", booking.status)
                }
                context.startActivity(intent)
            }
        }
    }

    fun updateList(newList: List<Booking>) {
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize() = list.size
            override fun getNewListSize() = newList.size
            override fun areItemsTheSame(oldPos: Int, newPos: Int) = 
                list[oldPos].id == newList[newPos].id
            override fun areContentsTheSame(oldPos: Int, newPos: Int) = 
                list[oldPos] == newList[newPos]
        }
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        list = newList
        diffResult.dispatchUpdatesTo(this)
    }
}