package com.example.budgettingtogether

import androidx.room.Entity
import androidx.room.PrimaryKey
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
    val isRecurring: Boolean = false,
    val notes: String = ""
)
