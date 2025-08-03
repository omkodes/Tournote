package com.example.tournote.Functionality.Segments.Expenses.AutoExpenseDetection.Debit

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.app.RemoteInput

class ReplyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val remoteInput = RemoteInput.getResultsFromIntent(intent)
        val description = remoteInput?.getCharSequence("expense_description")?.toString()
        val amount = intent.getStringExtra("amount") ?: "Unknown"

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(101) // cancel the sticky notification (the one with the reply action)

        if (!description.isNullOrEmpty()) {
            Log.d("ReplyReceiver", "User entered: $description for $amount")

            // Now show the confirmation notification, which will launch the activity on tap
            NotificationDebitUtils.showConfirmation(context, amount, description)

            // DO NOT launch SmsGroupSelectionActivity directly from here.
            // It will be launched when the user taps the confirmation notification.
        } else {
            // Handle case where description is empty, maybe re-show the original notification or a different message
            Toast.makeText(context, "Description cannot be empty.", Toast.LENGTH_SHORT).show()
            // Optionally, re-show the reply notification or inform the user
            NotificationDebitUtils.showNotificationWithReply(context, amount) // Re-show if description is mandatory
        }
    }
}