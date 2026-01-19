package com.example.budgettingtogether

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExchangeRateDao {
    @Query("SELECT * FROM exchange_rates ORDER BY currencyName")
    fun getAllRates(): Flow<List<ExchangeRate>>

    @Query("SELECT * FROM exchange_rates ORDER BY currencyName")
    suspend fun getAllRatesOnce(): List<ExchangeRate>

    @Query("SELECT * FROM exchange_rates WHERE currencyCode = :code")
    suspend fun getRate(code: String): ExchangeRate?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rates: List<ExchangeRate>)

    @Query("DELETE FROM exchange_rates")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM exchange_rates")
    suspend fun getCount(): Int
}
