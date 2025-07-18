package com.example.tournote.Functionality.Segments.Expenses.DataClass

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class UserModelForSplitter(
    val uid: String? = null,
    val email: String? = null,//. is not replaced with ,
    val name: String? = null,
    val phoneNumber: String? = null,
    val profilePic: String? = "null",
    var isSelected: Boolean = true,
    var exactAmount: Double = 0.0,
    var percentage: Double = 0.0
):Parcelable
