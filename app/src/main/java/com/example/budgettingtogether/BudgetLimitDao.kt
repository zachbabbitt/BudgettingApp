package com.example.budgettingtogether

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetLimitDao {
    @Query("SELECT * FROM budget_limits ORDER BY category ASC")
    fun getAllLimits(): Flow<List<BudgetLimit>>

    @Query("SELECT * FROM budget_limits WHERE category = :category")
    suspend fun getLimitForCategory(category: String): BudgetLimit?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(budgetLimit: BudgetLimit)

    @Query("DELETE FROM budget_limits WHERE category = :category")
    suspend fun delete(category: String)
}
