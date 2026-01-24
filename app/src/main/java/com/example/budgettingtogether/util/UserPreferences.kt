package com.example.budgettingtogether.util

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_preferences")
data class UserPreferences(
    @PrimaryKey
    val id: Int = 1,
    val defaultCurrencyCodeExpenses: String = "USD",
    val defaultCurrencyCodeTracking: String = "USD",
    val defaultCurrencyCodeIncome: String = "USD",
    val lastRatesUpdate: Long = 0L,
    val lastRecurringGenerationMonth: Int = -1,
    val lastRecurringGenerationYear: Int = -1
)
