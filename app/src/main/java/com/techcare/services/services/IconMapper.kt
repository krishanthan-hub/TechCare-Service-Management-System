package com.techcare.services.services

object IconMapper {
    fun getIconForCategory(category: String): String {
        return when (category) {
            "Mobile" -> "ic_mobile.svg"
            "Laptop" -> "ic_laptop.svg"
            "TV" -> "ic_tv.svg"
            "AC" -> "ic_ac.svg"
            "Fridge" -> "ic_fridge.svg"
            "Washer" -> "ic_washer.svg"
            else -> "ic_mobile.svg"
        }
    }

    // You can add a more specific mapper for individual services here
    fun getIconForService(serviceName: String, category: String): String {
        return when (serviceName) {
            "Screen Replacement" -> "ic_screen_repair.svg"
            "Battery Replacement" -> "ic_battery.svg"
            "Display Repair" -> "ic_display.svg"
            "Software Update" -> "ic_software.svg"
            "Charging Port Repair" -> "ic_charging.svg"
            "Water Damage Repair" -> "ic_water_damage.svg"
            "Speaker Repair" -> "ic_speaker.svg"
            else -> getIconForCategory(category)
        }
    }
}