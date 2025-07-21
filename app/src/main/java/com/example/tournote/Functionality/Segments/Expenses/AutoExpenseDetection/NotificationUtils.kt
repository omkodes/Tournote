package com.example.tournote.Functionality.Segments.Expenses.AutoExpenseDetection

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.example.tournote.R // Make sure R is imported

object NotificationUtils {
    private const val CHANNEL_ID_DEFAULT = "expense_alert_channel"
    private const val CHANNEL_NAME_DEFAULT = "Expense Alerts (Default)"
    private const val CHANNEL_ID_PROMINENT = "prominent_expense_channel"
    private const val CHANNEL_NAME_PROMINENT = "Urgent Expense Actions"

    private const val REPLY_KEY = "expense_description"

    fun showNotificationWithReply(context: Context, amount: String) {
        createDefaultChannel(context) // Use the default channel

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

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_DEFAULT) // Use default channel
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use your app's icon
            .setContentTitle("Expense of $amount Detected!")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Expense of $amount detected. Tap 'Add Description' to label it and proceed."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(action)
            .setOngoing(false)
            .setAutoCancel(false) // Don't cancel automatically, user needs to reply
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(101, notification)
    }

    fun showConfirmation(context: Context, amount: String, description: String) {
        createProminentChannel(context) // Ensure the prominent channel exists

        // Intent to launch SmsGroupSelectionActivity when the confirmation notification is tapped
        val launchActivityIntent = Intent(context, SmsGroupSelectionActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("amount", amount)
            putExtra("description", description)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            200, // Unique request code for this PendingIntent
            launchActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_PROMINENT) // Use prominent channel
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use your app's icon
            .setContentTitle("Action Required: Register Expense!")
            .setContentText("Tap to finalize the expense of $amount for '$description' by assigning it to a group.")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("An expense of $amount for '$description' has been detected. To ensure accurate financial tracking, please tap this notification to categorize and register it to one of your groups. Your prompt action is appreciated."))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Highest priority
            // .setFullScreenIntent(pendingIntent, true) // Optional: Highly intrusive, use with extreme caution!
            .setAutoCancel(true) // Still auto-cancel on click
            .setContentIntent(pendingIntent) // Set the intent to be launched when notification is tapped
            .setLights(Color.RED, 3000, 1000) // Red light, on for 3s, off for 1s
            // Removed: .setVibrate(longArrayOf(1000, 1000, 1000, 1000))
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(102, notification)
    }

    private fun createDefaultChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_DEFAULT, CHANNEL_NAME_DEFAULT,
                NotificationManager.IMPORTANCE_HIGH // High for the initial reply
            ).apply {
                description = "Notifications for initial expense detection and reply."
                enableLights(true)
                lightColor = Color.BLUE
                // Removed: enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createProminentChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val prominentChannel = NotificationChannel(
                CHANNEL_ID_PROMINENT, CHANNEL_NAME_PROMINENT,
                NotificationManager.IMPORTANCE_HIGH // MAX for the most prominent
            ).apply {
                description = "Urgent notifications requiring user action for expense registration."
                enableLights(true)
                lightColor = Color.RED // Different color for prominent
                // Removed: enableVibration(true)
                // Removed: vibrationPattern = longArrayOf(1000, 1000, 1000, 1000)
                // If you want it to make noise even in Do Not Disturb mode for very critical cases
                // setBypassDnd(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(prominentChannel)
        }
    }
}