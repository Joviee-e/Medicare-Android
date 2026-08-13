package com.example.medicare

data class MedicineItem(
    val name: String,
    val dose: String,
    val info: String,
    val statusColor: String, // "Teal", "Red", "Grey"
    val statusType: String // "Next", "Missed", "Taken"
)
