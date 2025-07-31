package com.example.tournote.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.tournote.Functionality.Segments.Expenses.DataClass.SplitType

@Entity(tableName = "member_shares")
data class MemberShareEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0, // Primary key for MemberShareEntity
    val expenseId: String, // Foreign key to link to ExpenseEntity
    val memberUid: String,
    val memberName: String,
    val shareAmount: Double,
    val shareType: SplitType, // Room will handle this enum automatically
    val originalInputValue: Double? = null,
    var paid: Boolean? = false,
    var partialPayment: Double? = 0.00
)