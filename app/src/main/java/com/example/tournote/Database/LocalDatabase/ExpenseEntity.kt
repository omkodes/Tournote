package com.example.tournote.Database.LocalDatabase.Entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.tournote.Functionality.Segments.Expenses.DataClass.ExpensesDataClass

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey
    val expenseId: String,
    val details: String,
    val amount: String, // Consider storing as Double for calculations
    val paidBy: String,
    val timestamp: String, // Consider storing as Long for better sorting/date operations
    val billImageUrl: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val note: String? = null,
    val splitType: String? = "null" // This should probably be the SplitType enum as well
)

// Note: The `splitMembers: ArrayList<MemberShare>?` in your original ExpensesDataClass
// cannot be directly stored as a list of complex objects in a single column in Room.
// You'll handle this with a one-to-many relationship using @Relation.
