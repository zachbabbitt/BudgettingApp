package com.example.budgettingtogether.limits

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budget_limits")
data class BudgetLimit(
    @PrimaryKey
    val category: String,
    val limitAmount: Double
)
