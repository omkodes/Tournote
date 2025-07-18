// File: com.example.tournote.Functionality.Segments.Expenses.DataClass.SplitMemberDisplayData.kt
package com.example.tournote.Functionality.Segments.Expenses.DataClass

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SplitMemberDisplayData(
    val memberUid: String,
    val memberName: String,         // E.g., "Pranav P." or "You"
    val profilePicUrl: String?,     // URL of the profile picture
    val shareAmount: Double,        // The amount this member owes/is owed
    val isCurrentUser: Boolean      // Flag to easily check if this is the current user
) : Parcelable