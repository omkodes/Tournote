package com.example.tournote

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class UserModel(
    val uid: String? = null,
    val email: String? = null,//. is not replaced with ,
    var name: String? = null,
    val phoneNumber: String? = null,
    var profilePic: String? = "null"
):Parcelable
