package com.example.budgettingtogether.storage.source

import com.example.budgettingtogether.income.Income
import kotlinx.coroutines.flow.Flow

interface IIncomeSource {
    fun getAllIncome(userGuid: String): Flow<List<Income>>
    fun getRecurringIncome(userGuid: String): Flow<List<Income>>
    fun getIncomeByRecurringType(userGuid: String, type: String): Flow<List<Income>>
    fun getIncomeBySource(userGuid: String, source: String): Flow<List<Income>>
    suspend fun insert(income: Income)
    suspend fun delete(income: Income)
    fun getTotalAmount(userGuid: String): Flow<Double?>

    fun getAllIncome(userGuids: List<String>): Flow<List<Income>>
    fun getRecurringIncome(userGuids: List<String>): Flow<List<Income>>
    fun getTotalAmount(userGuids: List<String>): Flow<Double?>
    fun getIncomeBySource(userGuids: List<String>, source: String): Flow<List<Income>>
    fun getIncomeByRecurringType(userGuids: List<String>, type: String): Flow<List<Income>>
}
