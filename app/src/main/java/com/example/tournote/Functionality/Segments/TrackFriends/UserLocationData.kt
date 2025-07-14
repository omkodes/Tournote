package com.example.tournote.Functionality.Model // Adjust package as needed

data class UserLocationData(
    val isRefreshing: Boolean = false,
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val timestamp: Long = 0L // Firebase timestamps are typically Long
)