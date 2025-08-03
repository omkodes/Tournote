package com.example.tournote.Functionality.Segments.Expenses.AutoExpenseDetection

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import com.example.tournote.Functionality.Segments.Expenses.AutoExpenseDetection.Debit.NotificationDebitUtils


class SmsCreditReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if(Telephony.Sms.Intents.SMS_RECEIVED_ACTION== intent?.action){
            val bundle = intent?.extras
            val pdus = bundle?.get("pdus") as? Array<*>
            val format = bundle?.getString("format")

            pdus?.forEach { pdu->
                val msg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    SmsMessage.createFromPdu(pdu as ByteArray, format)
                } else {
                    SmsMessage.createFromPdu(pdu as ByteArray)
                }

                val msgBody = msg.messageBody

                if(containsCreditKeyword(msgBody)) {
                    val amount = extractAmount(msgBody)
                    val displayText = if (amount != null) {
                        NotificationCreditUtils.showConfirmation(context,amount)
                        "Credit of $amount detected from SMS"
                    } else {
                        "Credit detected from SMS"//when there os error in fetching amount details
                    }

                    Log.d("SmsReceiver", displayText)

                }

            }
        }
    }


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

private fun containsCreditKeyword(msgBody: String): Boolean {
    val lower = msgBody.lowercase()
    return listOf("credited", "received", "deposited", "refund", "reversed", "amount received", "payment received", "paid in", "fund credited", "cashback credited", "collect").any { lower.contains(it) }
}
