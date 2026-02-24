package com.example.budgettingtogether.storage.source

import com.example.budgettingtogether.expenses.Expense
import com.example.budgettingtogether.expenses.ExpenseDao
import kotlinx.coroutines.flow.Flow

class LocalExpenseSource(private val dao: ExpenseDao) : IExpenseSource {
    override fun getAllExpenses(userGuid: String) = dao.getAllExpenses(userGuid)
    override fun getRecurringExpenses(userGuid: String) = dao.getRecurringExpenses(userGuid)
    override fun getExpensesByRecurringType(userGuid: String, type: String) = dao.getExpensesByRecurringType(userGuid, type)
    override suspend fun insert(expense: Expense) = dao.insert(expense)
    override suspend fun delete(expense: Expense) = dao.delete(expense)
    suspend fun deleteById(id: String) = dao.deleteById(id)
    override fun getTotalAmount(userGuid: String) = dao.getTotalAmount(userGuid)
    override suspend fun getMonthlyRecurringExpensesOnce(userGuid: String) = dao.getMonthlyRecurringExpensesOnce(userGuid)
    override suspend fun countMatchingExpensesInMonth(userGuid: String, title: String, category: String, recurringType: String, monthStart: Long, monthEnd: Long) =
        dao.countMatchingExpensesInMonth(userGuid, title, category, recurringType, monthStart, monthEnd)

    override fun getAllExpenses(userGuids: List<String>) = dao.getAllExpenses(userGuids)
    override fun getRecurringExpenses(userGuids: List<String>) = dao.getRecurringExpenses(userGuids)
    override fun getTotalAmount(userGuids: List<String>) = dao.getTotalAmount(userGuids)
    override fun getExpensesByRecurringType(userGuids: List<String>, type: String) = dao.getExpensesByRecurringType(userGuids, type)
    override suspend fun getMonthlyRecurringExpensesOnce(userGuids: List<String>) = dao.getMonthlyRecurringExpensesOnce(userGuids)
    override suspend fun countMatchingExpensesInMonth(userGuids: List<String>, title: String, category: String, recurringType: String, monthStart: Long, monthEnd: Long) =
        dao.countMatchingExpensesInMonth(userGuids, title, category, recurringType, monthStart, monthEnd)
}
