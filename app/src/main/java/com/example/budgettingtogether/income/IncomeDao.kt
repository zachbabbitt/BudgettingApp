package com.example.budgettingtogether.income

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface IncomeDao {
    @Query("SELECT * FROM income WHERE userGuid = :userGuid ORDER BY date DESC")
    fun getAllIncome(userGuid: String): Flow<List<Income>>

    @Query("SELECT * FROM income WHERE userGuid = :userGuid AND recurringType != 'NONE' ORDER BY date DESC")
    fun getRecurringIncome(userGuid: String): Flow<List<Income>>

    @Query("SELECT * FROM income WHERE userGuid = :userGuid AND recurringType = :type ORDER BY date DESC")
    fun getIncomeByRecurringType(userGuid: String, type: String): Flow<List<Income>>

    @Query("SELECT * FROM income WHERE userGuid = :userGuid AND source = :source ORDER BY date DESC")
    fun getIncomeBySource(userGuid: String, source: String): Flow<List<Income>>

    @Insert
    suspend fun insert(income: Income)

    @Delete
    suspend fun delete(income: Income)

    @Query("DELETE FROM income WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT SUM(amount) FROM income WHERE userGuid = :userGuid")
    fun getTotalAmount(userGuid: String): Flow<Double?>

    @Query("SELECT * FROM income WHERE userGuid IN (:userGuids) ORDER BY date DESC")
    fun getAllIncome(userGuids: List<String>): Flow<List<Income>>

    @Query("SELECT * FROM income WHERE userGuid IN (:userGuids) AND recurringType != 'NONE' ORDER BY date DESC")
    fun getRecurringIncome(userGuids: List<String>): Flow<List<Income>>

    @Query("SELECT SUM(amount) FROM income WHERE userGuid IN (:userGuids)")
    fun getTotalAmount(userGuids: List<String>): Flow<Double?>

    @Query("SELECT * FROM income WHERE userGuid IN (:userGuids) AND source = :source ORDER BY date DESC")
    fun getIncomeBySource(userGuids: List<String>, source: String): Flow<List<Income>>

    @Query("SELECT * FROM income WHERE userGuid IN (:userGuids) AND recurringType = :type ORDER BY date DESC")
    fun getIncomeByRecurringType(userGuids: List<String>, type: String): Flow<List<Income>>
}
