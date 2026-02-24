package com.example.budgettingtogether.storage.source

import android.util.Log
import com.example.budgettingtogether.income.Income
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@OptIn(DelicateCoroutinesApi::class)
class CachedIncomeSource(
    private val local: LocalIncomeSource,
    private val remote: RemoteIncomeSource
) : IIncomeSource {

    // ── Reads — always from Room ──────────────────────────────────────────────

    override fun getAllIncome(userGuid: String): Flow<List<Income>> =
        local.getAllIncome(userGuid)

    override fun getRecurringIncome(userGuid: String): Flow<List<Income>> =
        local.getRecurringIncome(userGuid)

    override fun getIncomeByRecurringType(userGuid: String, type: String): Flow<List<Income>> =
        local.getIncomeByRecurringType(userGuid, type)

    override fun getIncomeBySource(userGuid: String, source: String): Flow<List<Income>> =
        local.getIncomeBySource(userGuid, source)

    override fun getTotalAmount(userGuid: String): Flow<Double?> =
        local.getTotalAmount(userGuid)

    override fun getAllIncome(userGuids: List<String>): Flow<List<Income>> =
        local.getAllIncome(userGuids)

    override fun getRecurringIncome(userGuids: List<String>): Flow<List<Income>> =
        local.getRecurringIncome(userGuids)

    override fun getTotalAmount(userGuids: List<String>): Flow<Double?> =
        local.getTotalAmount(userGuids)

    override fun getIncomeBySource(userGuids: List<String>, source: String): Flow<List<Income>> =
        local.getIncomeBySource(userGuids, source)

    override fun getIncomeByRecurringType(userGuids: List<String>, type: String): Flow<List<Income>> =
        local.getIncomeByRecurringType(userGuids, type)

    // ── Writes — Room first, Supabase in background ───────────────────────────

    override suspend fun insert(income: Income) {
        local.insert(income)
        GlobalScope.launch(Dispatchers.IO) {
            try { remote.insert(income) }
            catch (e: Exception) { Log.w("CachedIncome", "Remote insert failed: ${e.message}") }
        }
    }

    override suspend fun delete(income: Income) {
        local.delete(income)
        GlobalScope.launch(Dispatchers.IO) {
            try { remote.delete(income) }
            catch (e: Exception) { Log.w("CachedIncome", "Remote delete failed: ${e.message}") }
        }
    }
}
