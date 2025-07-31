package com.example.tournote.database

import androidx.room.Embedded
import androidx.room.Relation
import com.example.tournote.Database.LocalDatabase.Entity.ExpenseEntity

data class ExpenseWithMembers(
    @Embedded
    val expense: ExpenseEntity,

    @Relation(
        parentColumn = "expenseId",
        entityColumn = "expenseId"
    )
    val members: List<MemberShareEntity>
)