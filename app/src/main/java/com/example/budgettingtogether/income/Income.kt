package com.example.budgettingtogether.income

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.budgettingtogether.util.RecurringType
import java.util.Date
import java.util.UUID

@Entity(tableName = "income")
data class Income(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val amount: Double,
    val source: String,
    val date: Date = Date(),
    val recurringType: RecurringType = RecurringType.NONE,
    val notes: String = "",
    val originalAmount: Double? = null,
    val originalCurrency: String? = null
)
