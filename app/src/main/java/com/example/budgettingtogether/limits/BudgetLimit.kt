package com.example.budgettingtogether.limits

import androidx.room.Entity

@Entity(tableName = "budget_limits", primaryKeys = ["category", "userGuid"])
data class BudgetLimit(
    val category: String,
    val limitAmount: Double,
    val currencyCode: String = "USD",
    val userGuid: String = ""
)
