package com.example.tournote.Functionality.Segments.ChatRoom.DataClass

data class FcmResponse(
    var success: Boolean,
    var message: String="",
    var data: fcmSave? = null, // Will be null on error
    var error: String? = null,      // Will be null on success
    var timestamp: String? = null
)
