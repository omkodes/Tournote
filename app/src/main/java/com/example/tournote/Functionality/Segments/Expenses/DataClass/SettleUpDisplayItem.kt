// File: com.example.tournote.Functionality.Segments.Expenses.DataClass.SettleUpDisplayItem.kt
package com.example.tournote.Functionality.Segments.Expenses.DataClass

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize // Add this annotation to automatically implement Parcelable
data class SettleUpDisplayItem(
    val memberUid: String,
    val memberName: String,
    val memberProfilePicUrl: String?, // URL for the profile picture
    val expenseDetails: String,
    val shareAmount: Double,
    val expenseId: String, // To potentially link back to the original expense

    var partialPayment: Double?=0.00,
) : Parcelable // Declare that it implements Parcelable
