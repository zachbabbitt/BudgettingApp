package com.example.budgettingtogether.storage.source

import com.example.budgettingtogether.limits.BudgetLimit
import kotlinx.coroutines.flow.Flow

interface IBudgetLimitSource {
    fun getAllLimits(userGuid: String): Flow<List<BudgetLimit>>
    suspend fun getAllLimitsOnce(userGuid: String): List<BudgetLimit>
    suspend fun getLimitForCategory(userGuid: String, category: String): BudgetLimit?
    suspend fun insertOrUpdate(budgetLimit: BudgetLimit)
    suspend fun updateAll(limits: List<BudgetLimit>)
    suspend fun delete(userGuid: String, category: String)

    fun getAllLimits(userGuids: List<String>): Flow<List<BudgetLimit>>
    suspend fun getAllLimitsOnce(userGuids: List<String>): List<BudgetLimit>
    suspend fun getLimitForCategory(userGuids: List<String>, category: String): BudgetLimit?
}
