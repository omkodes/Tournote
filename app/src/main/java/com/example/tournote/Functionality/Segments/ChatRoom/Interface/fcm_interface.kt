package com.example.tournote.Functionality.Segments.ChatRoom.Interface

import com.example.tournote.Functionality.Segments.ChatRoom.DataClass.FcmResponse
import com.example.tournote.Functionality.Segments.ChatRoom.DataClass.fcmSave
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface fcm_interface {
    @POST("saveToken")
    suspend fun saveToken(@Body request: fcmSave): Response<FcmResponse>
}
