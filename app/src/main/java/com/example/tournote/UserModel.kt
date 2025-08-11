package com.example.tournote

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class UserModel(
    var uid: String = "",
    var email: String = "",
    var name: String = "",
    var phoneNumber: String = "",
    var profilePic: String? = "null"
):Parcelable
