package com.example.budgettingtogether

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date
import java.util.UUID

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val amount: Double,
    val category: String,
    val date: Date = Date(),
    val recurringType: RecurringType = RecurringType.NONE
)
