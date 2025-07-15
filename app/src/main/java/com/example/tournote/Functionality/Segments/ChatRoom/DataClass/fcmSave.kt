package com.example.tournote.Functionality.Segments.ChatRoom.DataClass

import com.google.gson.annotations.SerializedName

data class fcmSave(
    @SerializedName("user_id")
    val userId: String,

    @SerializedName("token")
    val token: String,
    var updated_at: String? = "",
)
