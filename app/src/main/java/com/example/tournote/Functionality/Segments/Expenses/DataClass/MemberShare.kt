package com.example.tournote.Functionality.Segments.Expenses.DataClass

import android.os.Parcel
import android.os.Parcelable

data class MemberShare(
    val memberUid: String,
    val memberName: String,
    val shareAmount: Double,
    val shareType: SplitType, // The SplitType enum
    val originalInputValue: Double? = null, // Stores the exact amount or percentage entered
    var paid: Boolean? = false,

    var partialPayment: Double?=0.00,///if during data retrival it value is 0.00 -> payment not done, "originalValue" -> full payment done
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readDouble(),
        SplitType.valueOf(parcel.readString()!!),
        parcel.readValue(Double::class.java.classLoader) as? Double,
        parcel.readValue(Boolean::class.java.classLoader) as? Boolean
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(memberUid)
        parcel.writeString(memberName)
        parcel.writeDouble(shareAmount)
        parcel.writeString(shareType.name)
        parcel.writeValue(originalInputValue)
        parcel.writeValue(paid)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<MemberShare> {
        override fun createFromParcel(parcel: Parcel): MemberShare = MemberShare(parcel)

        override fun newArray(size: Int): Array<MemberShare?> = arrayOfNulls(size)
    }
}
