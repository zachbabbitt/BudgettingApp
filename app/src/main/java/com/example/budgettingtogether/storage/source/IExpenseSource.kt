package com.example.budgettingtogether.storage.source

import com.example.budgettingtogether.expenses.Expense
import kotlinx.coroutines.flow.Flow

interface IExpenseSource {
    fun getAllExpenses(userGuid: String): Flow<List<Expense>>
    fun getRecurringExpenses(userGuid: String): Flow<List<Expense>>
    fun getExpensesByRecurringType(userGuid: String, type: String): Flow<List<Expense>>
    suspend fun insert(expense: Expense)
    suspend fun delete(expense: Expense)
    fun getTotalAmount(userGuid: String): Flow<Double?>
    suspend fun getMonthlyRecurringExpensesOnce(userGuid: String): List<Expense>
    suspend fun countMatchingExpensesInMonth(
        userGuid: String, title: String, category: String,
        recurringType: String, monthStart: Long, monthEnd: Long
    ): Int

    fun getAllExpenses(userGuids: List<String>): Flow<List<Expense>>
    fun getRecurringExpenses(userGuids: List<String>): Flow<List<Expense>>
    fun getTotalAmount(userGuids: List<String>): Flow<Double?>
    fun getExpensesByRecurringType(userGuids: List<String>, type: String): Flow<List<Expense>>
    suspend fun getMonthlyRecurringExpensesOnce(userGuids: List<String>): List<Expense>
    suspend fun countMatchingExpensesInMonth(
        userGuids: List<String>, title: String, category: String,
        recurringType: String, monthStart: Long, monthEnd: Long
    ): Int
}
