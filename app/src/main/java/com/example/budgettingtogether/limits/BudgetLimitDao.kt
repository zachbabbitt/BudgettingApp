package com.example.budgettingtogether.limits

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetLimitDao {
    @Query("SELECT * FROM budget_limits WHERE userGuid = :userGuid ORDER BY category ASC")
    fun getAllLimits(userGuid: String): Flow<List<BudgetLimit>>

    @Query("SELECT * FROM budget_limits WHERE userGuid = :userGuid ORDER BY category ASC")
    suspend fun getAllLimitsOnce(userGuid: String): List<BudgetLimit>

    @Query("SELECT * FROM budget_limits WHERE userGuid = :userGuid AND category = :category")
    suspend fun getLimitForCategory(userGuid: String, category: String): BudgetLimit?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(budgetLimit: BudgetLimit)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateAll(limits: List<BudgetLimit>)

    @Query("DELETE FROM budget_limits WHERE userGuid = :userGuid AND category = :category")
    suspend fun delete(userGuid: String, category: String)

    @Query("SELECT * FROM budget_limits WHERE userGuid IN (:userGuids) ORDER BY category ASC")
    fun getAllLimits(userGuids: List<String>): Flow<List<BudgetLimit>>

    @Query("SELECT * FROM budget_limits WHERE userGuid IN (:userGuids) ORDER BY category ASC")
    suspend fun getAllLimitsOnce(userGuids: List<String>): List<BudgetLimit>

    @Query("SELECT * FROM budget_limits WHERE userGuid IN (:userGuids) AND category = :category")
    suspend fun getLimitForCategory(userGuids: List<String>, category: String): BudgetLimit?
}
