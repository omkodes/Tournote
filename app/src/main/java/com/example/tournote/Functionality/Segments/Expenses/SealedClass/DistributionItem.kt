package com.example.tournote.Functionality.Segments.Expenses.SealedClass

sealed class DistributionItem {
    data class Owes(val name: String, val amount: Double) : DistributionItem()
    data class Borrowed(val name: String, val amount: Double) : DistributionItem()
}