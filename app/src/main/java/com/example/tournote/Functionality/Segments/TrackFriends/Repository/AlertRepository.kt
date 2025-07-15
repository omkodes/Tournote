package com.example.tournote.Functionality.Segments.TrackFriends.Repository

import android.util.Log
import com.example.tournote.Functionality.Segments.ChatRoom.DataClass.FcmResponse
import com.example.tournote.Functionality.Segments.ChatRoom.Object.APIClient
import com.example.tournote.Functionality.Segments.TrackFriends.Data.showAlert

class AlertRepository {

    suspend fun showAlertAPI(user_name: String, user_id: String,group_id: String, group_name: String): Result<FcmResponse> {
        return try {

            val request = showAlert(user_name = user_name, user_id = user_id,group_id = group_id, group_name = group_name)

            val res = APIClient.alert.saveToken(request)
            if (res.isSuccessful) {
                val body = res.body()
                if (body?.success == true) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("FCM save failed: ${body?.error ?: body?.message}"))
                }
            } else {
                val errorBody = res.errorBody()?.string()
                Result.failure(Exception("HTTP ${res.code()} — ${errorBody ?: "Unknown error"}"))
            }

        } catch (e: Exception) {
            Log.e("FCM_SAVE", "Exception during FCM save", e)
            Result.failure(e)
        }
    }
}