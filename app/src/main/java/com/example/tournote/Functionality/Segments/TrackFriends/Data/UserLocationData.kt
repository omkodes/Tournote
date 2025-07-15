package com.example.tournote.Functionality.Segments.TrackFriends.Data

data class UserLocationData(
    val isRefreshing: Boolean = false,
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val timestamp: Long = 0L // Firebase timestamps are typically Long
)