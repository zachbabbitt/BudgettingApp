package com.example.budgettingtogether.storage.source

import com.example.budgettingtogether.limits.BudgetLimit
import com.example.budgettingtogether.limits.BudgetLimitDao
import kotlinx.coroutines.flow.Flow

class LocalBudgetLimitSource(private val dao: BudgetLimitDao) : IBudgetLimitSource {
    override fun getAllLimits(userGuid: String) = dao.getAllLimits(userGuid)
    override suspend fun getAllLimitsOnce(userGuid: String) = dao.getAllLimitsOnce(userGuid)
    override suspend fun getLimitForCategory(userGuid: String, category: String) = dao.getLimitForCategory(userGuid, category)
    override suspend fun insertOrUpdate(budgetLimit: BudgetLimit) = dao.insertOrUpdate(budgetLimit)
    override suspend fun updateAll(limits: List<BudgetLimit>) = dao.updateAll(limits)
    override suspend fun delete(userGuid: String, category: String) = dao.delete(userGuid, category)

    override fun getAllLimits(userGuids: List<String>) = dao.getAllLimits(userGuids)
    override suspend fun getAllLimitsOnce(userGuids: List<String>) = dao.getAllLimitsOnce(userGuids)
    override suspend fun getLimitForCategory(userGuids: List<String>, category: String) = dao.getLimitForCategory(userGuids, category)
}
