package com.example.budgettingtogether.storage.source

import android.util.Log
import com.example.budgettingtogether.expenses.Expense
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Write-through cache: reads always come from Room (instant reactive Flows),
 * writes go to Room immediately and sync to Supabase in the background.
 */
@OptIn(DelicateCoroutinesApi::class)
class CachedExpenseSource(
    private val local: LocalExpenseSource,
    private val remote: RemoteExpenseSource
) : IExpenseSource {

    // ── Reads — always from Room ──────────────────────────────────────────────

    override fun getAllExpenses(userGuid: String): Flow<List<Expense>> =
        local.getAllExpenses(userGuid)

    override fun getRecurringExpenses(userGuid: String): Flow<List<Expense>> =
        local.getRecurringExpenses(userGuid)

    override fun getExpensesByRecurringType(userGuid: String, type: String): Flow<List<Expense>> =
        local.getExpensesByRecurringType(userGuid, type)

    override fun getTotalAmount(userGuid: String): Flow<Double?> =
        local.getTotalAmount(userGuid)

    override suspend fun getMonthlyRecurringExpensesOnce(userGuid: String): List<Expense> =
        local.getMonthlyRecurringExpensesOnce(userGuid)

    override suspend fun countMatchingExpensesInMonth(
        userGuid: String, title: String, category: String,
        recurringType: String, monthStart: Long, monthEnd: Long
    ): Int = local.countMatchingExpensesInMonth(userGuid, title, category, recurringType, monthStart, monthEnd)

    override fun getAllExpenses(userGuids: List<String>): Flow<List<Expense>> =
        local.getAllExpenses(userGuids)

    override fun getRecurringExpenses(userGuids: List<String>): Flow<List<Expense>> =
        local.getRecurringExpenses(userGuids)

    override fun getTotalAmount(userGuids: List<String>): Flow<Double?> =
        local.getTotalAmount(userGuids)

    override fun getExpensesByRecurringType(userGuids: List<String>, type: String): Flow<List<Expense>> =
        local.getExpensesByRecurringType(userGuids, type)

    override suspend fun getMonthlyRecurringExpensesOnce(userGuids: List<String>): List<Expense> =
        local.getMonthlyRecurringExpensesOnce(userGuids)

    override suspend fun countMatchingExpensesInMonth(
        userGuids: List<String>, title: String, category: String,
        recurringType: String, monthStart: Long, monthEnd: Long
    ): Int = local.countMatchingExpensesInMonth(userGuids, title, category, recurringType, monthStart, monthEnd)

    // ── Writes — Room first, Supabase in background ───────────────────────────

    override suspend fun insert(expense: Expense) {
        local.insert(expense)
        GlobalScope.launch(Dispatchers.IO) {
            try { remote.insert(expense) }
            catch (e: Exception) { Log.w("CachedExpense", "Remote insert failed: ${e.message}") }
        }
    }

    override suspend fun delete(expense: Expense) {
        local.delete(expense)
        GlobalScope.launch(Dispatchers.IO) {
            try { remote.delete(expense) }
            catch (e: Exception) { Log.w("CachedExpense", "Remote delete failed: ${e.message}") }
        }
    }
}
