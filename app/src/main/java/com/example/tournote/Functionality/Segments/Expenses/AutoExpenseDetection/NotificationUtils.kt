package com.example.tournote.Functionality.Segments.Expenses.AutoExpenseDetection

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput

object NotificationUtils {
    private const val CHANNEL_ID = "expense_alert_channel"
    private const val CHANNEL_NAME = "Expense Alerts"
    private const val REPLY_KEY = "expense_description"

    fun showNotificationWithReply(context: Context, amount: String) {
        createChannel(context)

        // Remote input (like WhatsApp reply)
        val remoteInput = RemoteInput.Builder(REPLY_KEY)
            .setLabel("Enter description")
            .build()

        val replyIntent = Intent(context, ReplyReceiver::class.java).apply {
            putExtra("amount", amount)
        }
        val replyPendingIntent = PendingIntent.getBroadcast(
            context, 100, replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val action = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_send,
            "Add Description",
            replyPendingIntent
        ).addRemoteInput(remoteInput).build()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Expense of $amount Detected!")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Expense of $amount detected. Tap 'Add Description' to label it."))

            .addAction(action)
            .setOngoing(true)  // 🔒 make it sticky
            .setAutoCancel(false)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(101, notification)
    }

    fun showConfirmation(context: Context, amount: String, description: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.checkbox_on_background)
            .setContentTitle("Expense Registered")
            .setContentText("Expense of $amount for $description registered successfully.")
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(102, notification)
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for detected expenses"
                enableLights(true)
                lightColor = Color.BLUE
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
