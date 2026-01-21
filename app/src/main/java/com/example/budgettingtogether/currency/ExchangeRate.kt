package com.example.budgettingtogether.currency

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exchange_rates")
data class ExchangeRate(
    @PrimaryKey
    val currencyCode: String,
    val rateToUsd: Double,
    val currencyName: String,
    val symbol: String
)
