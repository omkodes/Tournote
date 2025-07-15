package com.example.tournote.Functionality.Segments.Expenses

// Sealed class to represent different types of items in the RecyclerView
sealed class ExpenseListItem {
    data class ExpenseItem(val expense: ExpensesDataClass) : ExpenseListItem()
    data class MonthHeader(val monthYear: String) : ExpenseListItem()
}