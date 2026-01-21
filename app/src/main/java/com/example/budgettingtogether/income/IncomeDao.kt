package com.example.budgettingtogether.income

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface IncomeDao {
    @Query("SELECT * FROM income ORDER BY date DESC")
    fun getAllIncome(): Flow<List<Income>>

    @Query("SELECT * FROM income WHERE recurringType != 'NONE' ORDER BY date DESC")
    fun getRecurringIncome(): Flow<List<Income>>

    @Query("SELECT * FROM income WHERE recurringType = :type ORDER BY date DESC")
    fun getIncomeByRecurringType(type: String): Flow<List<Income>>

    @Query("SELECT * FROM income WHERE source = :source ORDER BY date DESC")
    fun getIncomeBySource(source: String): Flow<List<Income>>

    @Insert
    suspend fun insert(income: Income)

    @Delete
    suspend fun delete(income: Income)

    @Query("SELECT SUM(amount) FROM income")
    fun getTotalAmount(): Flow<Double?>
}
