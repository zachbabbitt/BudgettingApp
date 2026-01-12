package com.example.budgettingtogether

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budget_limits")
data class BudgetLimit(
    @PrimaryKey
    val category: String,
    val limitAmount: Double
)
