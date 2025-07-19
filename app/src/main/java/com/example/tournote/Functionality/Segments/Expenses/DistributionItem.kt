// In a new file, e.g., models/DistributionItem.kt
package com.yourpackage.app.models // Replace with your actual package name

sealed class DistributionItem {
    data class Owes(val name: String, val amount: Double) : DistributionItem()
    data class Borrowed(val name: String, val amount: Double) : DistributionItem()
}