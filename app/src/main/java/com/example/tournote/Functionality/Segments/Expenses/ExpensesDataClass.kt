package com.example.tournote.Functionality.Segments.Expenses

data class ExpensesDataClass(
    val details : String,
    val amount : String,
    val paidBy : String,
    val timestamp : String,
    val billImageUrl : String ? =null,
    val latitude: Double? = null,
    val longitude: Double? = null
)
