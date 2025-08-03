package com.example.tournote.Functionality.Segments.Expenses.AutoExpenseDetection

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import android.os.Handler
import android.os.Looper


class SmsReceiver : BroadcastReceiver() {


    override fun onReceive(context: Context, intent: Intent) {
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION == intent.action) {
            val bundle = intent.extras
            val pdus = bundle?.get("pdus") as? Array<*>
            val format = bundle?.getString("format")

            pdus?.forEach { pdu ->
                val msg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    SmsMessage.createFromPdu(pdu as ByteArray, format)
                } else {
                    SmsMessage.createFromPdu(pdu as ByteArray)
                }

                val msgBody = msg.messageBody

                if (containsExpenseKeyword(msgBody)) {
                    val amount = extractAmount(msgBody)
                    val displayText = if (amount != null) {
                        "Expense of $amount detected from SMS"
                    } else {
                        "Expense detected from SMS"
                    }

                    Log.d("SmsReceiver", displayText)
                    val handler = Handler(Looper.getMainLooper())

                    handler.postDelayed({
                        NotificationUtils.showNotificationWithReply(context, amount ?: "Unknown amount")
                    }, 1000) // delay in milliseconds (e.g., 1000ms = 1 second)
                }
            }
        }
    }

    private fun containsExpenseKeyword(body: String): Boolean {
        val lower = body.lowercase()
        return listOf("debited", "deducted", "txn", "payment alert", "spent", "purchased").any { lower.contains(it) }
    }

    private fun extractAmount(body: String): String? {
        // First try to find amount with currency prefix
        val currencyRegex = Regex("""(?:INR|Rs\.?|₹|USD|\$|£|€)\s*([\d,]+(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE)
        val currencyMatch = currencyRegex.find(body)
        if (currencyMatch != null) {
            return currencyMatch.groupValues[1]
        }

        // If no currency prefix found, look for standalone decimal numbers
        // This regex finds numbers with 1-2 decimal places (including .0)
        val standaloneRegex = Regex("""(?<!\d)\d+\.\d{1,2}(?!\d)""")
        val standaloneMatch = standaloneRegex.find(body)
        if (standaloneMatch != null) {
            return standaloneMatch.value
        }

        return null
    }
}