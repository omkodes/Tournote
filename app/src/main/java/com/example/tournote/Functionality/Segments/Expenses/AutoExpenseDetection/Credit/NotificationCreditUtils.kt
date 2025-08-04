package com.example.tournote.Functionality.Segments.Expenses.AutoExpenseDetection.Credit

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.tournote.Functionality.Segments.Expenses.AutoExpenseDetection.Debit.NotificationDebitUtils
import com.example.tournote.Functionality.Segments.Expenses.AutoExpenseDetection.SmsGroupSelectionActivity
import com.example.tournote.R

object NotificationCreditUtils {
    private const val CHANNEL_ID_PROMINENT = "prominent_expense_channel"
    private const val CHANNEL_NAME_PROMINENT = "Urgent Expense Actions"

    fun showConfirmation(context: Context?, amount: String){
        NotificationDebitUtils.createProminentChannel(context) // Ensure the prominent channel exists

        // Intent to launch SmsGroupSelectionActivity when the confirmation notification is tapped
        val launchActivityIntent = Intent(context, SmsGroupSelectionActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP // Use 'flags' property
            //putExtra("amount", amount)
            putExtra("functionality", "SettlingExpense")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            200, // Unique request code for this PendingIntent
            launchActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context,
            CHANNEL_ID_PROMINENT
        ) // Use prominent channel
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use your app's icon
            .setContentTitle("Action Required: Register Credit!")
            .setContentText("Tap to assign the credit of $amount to previous expenses.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("An credit of $amount has been detected."))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Highest priority
            // .setFullScreenIntent(pendingIntent, true) // Optional: Highly intrusive, use with extreme caution!
            .setAutoCancel(true) // Still auto-cancel on click
            .setContentIntent(pendingIntent) // Set the intent to be launched when notification is tapped
            .setLights(Color.RED, 3000, 1000) // Red light, on for 3s, off for 1s
            // Removed: .setVibrate(longArrayOf(1000, 1000, 1000, 1000))
            .build()

        val manager = context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(102, notification)
    }

    fun createProminentChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val prominentChannel = NotificationChannel(
                CHANNEL_ID_PROMINENT, CHANNEL_NAME_PROMINENT,
                NotificationManager.IMPORTANCE_HIGH // MAX for the most prominent
            ).apply {
                description = "Urgent notifications requiring user action for credit registration."
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