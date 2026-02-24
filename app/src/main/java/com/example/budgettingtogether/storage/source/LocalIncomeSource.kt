package com.example.budgettingtogether.storage.source

import com.example.budgettingtogether.income.Income
import com.example.budgettingtogether.income.IncomeDao
import kotlinx.coroutines.flow.Flow

class LocalIncomeSource(private val dao: IncomeDao) : IIncomeSource {
    override fun getAllIncome(userGuid: String) = dao.getAllIncome(userGuid)
    override fun getRecurringIncome(userGuid: String) = dao.getRecurringIncome(userGuid)
    override fun getIncomeByRecurringType(userGuid: String, type: String) = dao.getIncomeByRecurringType(userGuid, type)
    override fun getIncomeBySource(userGuid: String, source: String) = dao.getIncomeBySource(userGuid, source)
    override suspend fun insert(income: Income) = dao.insert(income)
    override suspend fun delete(income: Income) = dao.delete(income)
    suspend fun deleteById(id: String) = dao.deleteById(id)
    override fun getTotalAmount(userGuid: String) = dao.getTotalAmount(userGuid)

    override fun getAllIncome(userGuids: List<String>) = dao.getAllIncome(userGuids)
    override fun getRecurringIncome(userGuids: List<String>) = dao.getRecurringIncome(userGuids)
    override fun getTotalAmount(userGuids: List<String>) = dao.getTotalAmount(userGuids)
    override fun getIncomeBySource(userGuids: List<String>, source: String) = dao.getIncomeBySource(userGuids, source)
    override fun getIncomeByRecurringType(userGuids: List<String>, type: String) = dao.getIncomeByRecurringType(userGuids, type)
}
