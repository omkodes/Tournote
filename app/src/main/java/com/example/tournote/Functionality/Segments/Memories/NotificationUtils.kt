package com.example.tournote.Functionality.Segments.Memories

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.tournote.R

object NotificationUtils{
private const val CHANNEL_ID = "upload_channel"
private const val CHANNEL_NAME = "Media Uploads"

    fun createUploadNotification(context: Context, content: String): Notification {
    createChannelIfNeeded(context)
    return NotificationCompat.Builder(context, CHANNEL_ID)
        .setContentTitle("Uploading Memories")
        .setContentText(content)
        .setSmallIcon(R.drawable.logo) // Replace with your own drawable
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(true)
        .build()
    }

    fun showUploadCompleteNotification(context: Context, groupName: String) {
    createChannelIfNeeded(context)
    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setContentTitle("Upload Complete")
        .setContentText("Media uploaded to group: $groupName")
        .setSmallIcon(R.drawable.baseline_download_24) // Replace with your own drawable
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .build()

    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    fun showUploadFailedNotification(context: Context, groupName: String, reason: String) {
    createChannelIfNeeded(context)
    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setContentTitle("Upload Failed")
        .setContentText("Group: $groupName. Reason: $reason")
        .setSmallIcon(R.drawable.mark) // Replace with your own drawable
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .build()

    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createChannelIfNeeded(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)
    }

    }
}
