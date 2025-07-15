package com.example.tournote.Functionality.Segments.TrackFriends.Interface

import com.example.tournote.Functionality.Segments.ChatRoom.DataClass.FcmResponse
import com.example.tournote.Functionality.Segments.TrackFriends.Data.showAlert
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface alert_interface {

    @POST("showAlert")
    suspend fun saveToken(@Body request: showAlert): Response<FcmResponse>
}