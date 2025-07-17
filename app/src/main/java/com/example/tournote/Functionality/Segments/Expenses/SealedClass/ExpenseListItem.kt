package com.example.tournote.Functionality.Segments.Expenses.SealedClass

import com.example.tournote.Functionality.Segments.Expenses.DataClass.ExpensesDataClass

// Sealed class to represent different types of items in the RecyclerView
sealed class ExpenseListItem {
    data class ExpenseItem(val expense: ExpensesDataClass) : ExpenseListItem()
    data class MonthHeader(val monthYear: String) : ExpenseListItem()
}