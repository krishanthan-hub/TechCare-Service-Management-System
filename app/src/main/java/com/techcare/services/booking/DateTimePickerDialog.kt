package com.techcare.services.booking

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.TextView
import androidx.gridlayout.widget.GridLayout
import com.techcare.services.R
import com.techcare.services.databinding.DialogDatePickerBinding
import java.util.Calendar

class DateTimePickerDialog(
    context: Context,
    private val bookedSlots: Map<String, List<String>>,
    private val onConfirmed: (date: String, time: String) -> Unit
) : Dialog(context) {

    private lateinit var binding: DialogDatePickerBinding
    private var calYear = 0
    private var calMonth = 0
    private var selectedDay = -1
    private var selectedDate = ""
    private var selectedTime = ""
    private var selectedDayCell: TextView? = null
    private var selectedTimeSlot: TextView? = null

    private val monthNames = listOf(
        "January","February","March","April","May","June",
        "July","August","September","October","November","December"
    )

    private val timeSlots = listOf(
        "8:00 AM","9:00 AM","10:00 AM",
        "11:00 AM","1:00 PM","2:00 PM",
        "3:00 PM","4:00 PM","5:00 PM"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = DialogDatePickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Round corners and full width
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        window?.decorView?.background =
            context.getDrawable(R.drawable.bg_dialog_rounded)

        val now = Calendar.getInstance()
        calYear = now.get(Calendar.YEAR)
        calMonth = now.get(Calendar.MONTH)

        buildCalendar()

        binding.btnClose.setOnClickListener { dismiss() }

        binding.btnPrevMonth.setOnClickListener {
            val now2 = Calendar.getInstance()
            if (calYear > now2.get(Calendar.YEAR) ||
                (calYear == now2.get(Calendar.YEAR) &&
                        calMonth > now2.get(Calendar.MONTH))) {
                calMonth--
                if (calMonth < 0) { calMonth = 11; calYear-- }
                selectedDay = -1
                selectedDate = ""
                selectedDayCell = null
                buildCalendar()
                binding.timeSection.visibility = View.GONE
            }
        }

        binding.btnNextMonth.setOnClickListener {
            calMonth++
            if (calMonth > 11) { calMonth = 0; calYear++ }
            selectedDay = -1
            selectedDate = ""
            selectedDayCell = null
            buildCalendar()
            binding.timeSection.visibility = View.GONE
        }

        setupTimeSlots()

        binding.btnConfirmSelection.setOnClickListener {
            if (selectedDate.isNotEmpty() && selectedTime.isNotEmpty()) {
                onConfirmed(selectedDate, selectedTime)
                dismiss()
            }
        }
    }

    private fun buildCalendar() {
        binding.tvMonthYear.text = "${monthNames[calMonth]} $calYear"
        val container = binding.calendarGrid
        container.removeAllViews()

        val cellSize = context.resources.getDimensionPixelSize(R.dimen.calendar_cell_size)

        // Add Day Headers
        listOf("S","M","T","W","T","F","S").forEach { d ->
            val tv = TextView(context).apply {
                text = d
                textSize = 12f
                setTextColor(context.getColor(R.color.text_secondary))
                gravity = Gravity.CENTER
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            val params = GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED),
                GridLayout.spec(GridLayout.UNDEFINED, 1f)
            ).apply {
                width = 0
                height = cellSize
                setMargins(2, 4, 2, 4)
            }
            container.addView(tv, params)
        }

        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, calYear)
            set(Calendar.MONTH, calMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val today = Calendar.getInstance()

        // Empty cells for first week
        repeat(firstDayOfWeek) {
            val empty = TextView(context)
            val params = GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED),
                GridLayout.spec(GridLayout.UNDEFINED, 1f)
            ).apply {
                width = 0
                height = cellSize
            }
            container.addView(empty, params)
        }

        // Day cells
        for (day in 1..daysInMonth) {
            val isPast = calYear < today.get(Calendar.YEAR) ||
                    (calYear == today.get(Calendar.YEAR) &&
                            calMonth < today.get(Calendar.MONTH)) ||
                    (calYear == today.get(Calendar.YEAR) &&
                            calMonth == today.get(Calendar.MONTH) &&
                            day < today.get(Calendar.DAY_OF_MONTH))

            val isTodayDay = calYear == today.get(Calendar.YEAR) &&
                    calMonth == today.get(Calendar.MONTH) &&
                    day == today.get(Calendar.DAY_OF_MONTH)

            val isSelected = day == selectedDay

            val tv = TextView(context).apply {
                text = day.toString()
                textSize = 13f
                gravity = Gravity.CENTER

                when {
                    isSelected -> {
                        background = context.getDrawable(R.drawable.bg_circle_primary)
                        setTextColor(Color.WHITE)
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    }
                    isTodayDay -> {
                        background = context.getDrawable(R.drawable.bg_circle_today)
                        setTextColor(context.getColor(R.color.primary))
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    }
                    isPast -> {
                        setTextColor(Color.parseColor("#CCCCCC"))
                    }
                    else -> {
                        setTextColor(context.getColor(R.color.text_primary))
                    }
                }

                if (!isPast) {
                    setOnClickListener {
                        selectedDayCell?.apply {
                            val wasToday = selectedDay == today.get(Calendar.DAY_OF_MONTH) &&
                                    calMonth == today.get(Calendar.MONTH) &&
                                    calYear == today.get(Calendar.YEAR)
                            background = if (wasToday) context.getDrawable(R.drawable.bg_circle_today) else null
                            setTextColor(if (wasToday) context.getColor(R.color.primary) else context.getColor(R.color.text_primary))
                        }

                        background = context.getDrawable(R.drawable.bg_circle_primary)
                        setTextColor(Color.WHITE)
                        selectedDay = day
                        selectedDate = "$day/${calMonth + 1}/$calYear"
                        selectedDayCell = this

                        binding.tvTimeTitle.text = "Select a Time — ${monthNames[calMonth].take(3)} $day"
                        binding.timeSection.visibility = View.VISIBLE
                        binding.tvDialogTitle.text = "Select a Date & Time"
                        updateTimeSlotStates()
                    }
                }
            }

            val params = GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED),
                GridLayout.spec(GridLayout.UNDEFINED, 1f)
            ).apply {
                width = 0
                height = cellSize
                setMargins(2, 2, 2, 2)
            }
            container.addView(tv, params)
        }
    }

    private fun setupTimeSlots() {
        val slots = listOf(
            binding.slot1, binding.slot2, binding.slot3,
            binding.slot4, binding.slot5, binding.slot6,
            binding.slot7, binding.slot8, binding.slot9
        )
        slots.forEachIndexed { index, slot ->
            slot.text = timeSlots[index]
            slot.tag = timeSlots[index]
            slot.setOnClickListener {
                val time = slot.tag as String
                val bookedForDay = bookedSlots[selectedDate] ?: emptyList()
                if (bookedForDay.contains(time)) return@setOnClickListener

                selectedTimeSlot?.apply {
                    background = context.getDrawable(R.drawable.bg_slot_unselected)
                    setTextColor(context.getColor(R.color.text_primary))
                }
                slot.background = context.getDrawable(R.drawable.bg_slot_selected)
                slot.setTextColor(Color.WHITE)
                selectedTimeSlot = slot
                selectedTime = time
            }
        }
    }

    private fun updateTimeSlotStates() {
        val bookedForDay = bookedSlots[selectedDate] ?: emptyList()
        val slots = listOf(
            binding.slot1, binding.slot2, binding.slot3,
            binding.slot4, binding.slot5, binding.slot6,
            binding.slot7, binding.slot8, binding.slot9
        )
        slots.forEachIndexed { index, slot ->
            val time = timeSlots[index]
            val isBooked = bookedForDay.contains(time)
            if (isBooked) {
                slot.background = context.getDrawable(R.drawable.bg_slot_disabled)
                slot.setTextColor(Color.parseColor("#BBBBBB"))
                slot.paintFlags = slot.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                slot.alpha = 0.5f
            } else {
                slot.background = context.getDrawable(R.drawable.bg_slot_unselected)
                slot.setTextColor(context.getColor(R.color.text_primary))
                slot.paintFlags = slot.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
                slot.alpha = 1f
            }
        }
    }
}