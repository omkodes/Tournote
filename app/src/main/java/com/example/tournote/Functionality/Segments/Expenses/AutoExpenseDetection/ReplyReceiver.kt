package com.example.tournote.Functionality.Segments.Expenses.AutoExpenseDetection

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import android.util.Log

class ReplyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val remoteInput = RemoteInput.getResultsFromIntent(intent)
        val description = remoteInput?.getCharSequence("expense_description")?.toString()
        val amount = intent.getStringExtra("amount") ?: "Unknown"

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(101) // cancel the sticky notification

        if (!description.isNullOrEmpty()) {
            Log.d("ReplyReceiver", "User entered: $description for $amount")
            NotificationUtils.showConfirmation(context, amount, description)

            // 2. Now launch SmsGroupSelectionActivity
            val activityIntent = Intent(context, SmsGroupSelectionActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("amount", amount)
                putExtra("description", description)
            }

            context.startActivity(activityIntent)
            // Optional: Save to local DB here
        }
    }
}
