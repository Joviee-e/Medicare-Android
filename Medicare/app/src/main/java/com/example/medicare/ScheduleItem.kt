package com.example.medicare

data class ScheduleItem(
    val name: String,
    val dose: String,
    val time: String,
    val status: String // "Taken", "Upcoming", "Missed"
)
