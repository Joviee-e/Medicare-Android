package com.example.medicare

data class PharmacyItem(
    val placeId: String,
    val name: String,
    val rating: String,
    val details: String,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val phoneNumber: String? = null,
    val website: String? = null,
    val isOpen: Boolean? = null,
    val isMock: Boolean = false
)
