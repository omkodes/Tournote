package com.example.tournote

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class UserModel(
    val uid: String,
    val email: String,
    var name: String,
    val phoneNumber: String,
    var profilePic: String? = "null"
):Parcelable
