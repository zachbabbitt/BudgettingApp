package com.example.budgettingtogether.storage.source

import android.util.Log
import com.example.budgettingtogether.limits.BudgetLimit
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@OptIn(DelicateCoroutinesApi::class)
class CachedBudgetLimitSource(
    private val local: LocalBudgetLimitSource,
    private val remote: RemoteBudgetLimitSource
) : IBudgetLimitSource {

    // ── Reads — always from Room ──────────────────────────────────────────────

    override fun getAllLimits(userGuid: String): Flow<List<BudgetLimit>> =
        local.getAllLimits(userGuid)

    override suspend fun getAllLimitsOnce(userGuid: String): List<BudgetLimit> =
        local.getAllLimitsOnce(userGuid)

    override suspend fun getLimitForCategory(userGuid: String, category: String): BudgetLimit? =
        local.getLimitForCategory(userGuid, category)

    override fun getAllLimits(userGuids: List<String>): Flow<List<BudgetLimit>> =
        local.getAllLimits(userGuids)

    override suspend fun getAllLimitsOnce(userGuids: List<String>): List<BudgetLimit> =
        local.getAllLimitsOnce(userGuids)

    override suspend fun getLimitForCategory(userGuids: List<String>, category: String): BudgetLimit? =
        local.getLimitForCategory(userGuids, category)

    // ── Writes — Room first, Supabase in background ───────────────────────────

    override suspend fun insertOrUpdate(budgetLimit: BudgetLimit) {
        local.insertOrUpdate(budgetLimit)
        GlobalScope.launch(Dispatchers.IO) {
            try { remote.insertOrUpdate(budgetLimit) }
            catch (e: Exception) { Log.w("CachedBudgetLimit", "Remote insertOrUpdate failed: ${e.message}") }
        }
    }

    override suspend fun updateAll(limits: List<BudgetLimit>) {
        local.updateAll(limits)
        GlobalScope.launch(Dispatchers.IO) {
            try { remote.updateAll(limits) }
            catch (e: Exception) { Log.w("CachedBudgetLimit", "Remote updateAll failed: ${e.message}") }
        }
    }

    override suspend fun delete(userGuid: String, category: String) {
        local.delete(userGuid, category)
        GlobalScope.launch(Dispatchers.IO) {
            try { remote.delete(userGuid, category) }
            catch (e: Exception) { Log.w("CachedBudgetLimit", "Remote delete failed: ${e.message}") }
        }
    }
}
