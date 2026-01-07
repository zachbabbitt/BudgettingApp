package com.example.budgettingtogether

import java.util.Date
import java.util.UUID

data class Income(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val amount: Double,
    val source: String,
    val date: Date = Date()
)
