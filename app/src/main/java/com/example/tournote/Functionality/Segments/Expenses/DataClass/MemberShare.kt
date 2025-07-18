// File: com.example.tournote.Functionality.Segments.Expenses.DataClass.MemberShare.kt
package com.example.tournote.Functionality.Segments.Expenses.DataClass

import android.os.Parcel
import android.os.Parcelable

data class MemberShare(
    val memberUid: String,
    val memberName: String,
    val shareAmount: Double,
    val shareType: SplitType, // The SplitType enum
    val originalInputValue: Double? = null // Stores the exact amount or percentage entered
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readDouble(),
        SplitType.valueOf(parcel.readString()!!),
        parcel.readValue(Double::class.java.classLoader) as? Double
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(memberUid)
        parcel.writeString(memberName)
        parcel.writeDouble(shareAmount)
        parcel.writeString(shareType.name) // Write enum name as string
        parcel.writeValue(originalInputValue)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MemberShare> {
        override fun createFromParcel(parcel: Parcel): MemberShare {
            return MemberShare(parcel)
        }

        override fun newArray(size: Int): Array<MemberShare?> {
            return arrayOfNulls(size)
        }
    }
}