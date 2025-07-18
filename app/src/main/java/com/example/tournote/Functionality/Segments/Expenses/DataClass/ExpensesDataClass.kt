package com.example.tournote.Functionality.Segments.Expenses.DataClass

data class ExpensesDataClass(
    val expenseId : String?=null,
    val details : String,
    val amount : String,
    val paidBy : String,
    val timestamp : String,
    val billImageUrl : String ? =null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val note : String?= null,
    val splitType : String ?= "null",
    val splitMembers: ArrayList<MemberShare>? = null
)