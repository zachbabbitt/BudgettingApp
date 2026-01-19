package com.example.budgettingtogether

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_preferences")
data class UserPreferences(
    @PrimaryKey
    val id: Int = 1,
    val defaultCurrencyCode: String = "USD",
    val lastRatesUpdate: Long = 0L
)
