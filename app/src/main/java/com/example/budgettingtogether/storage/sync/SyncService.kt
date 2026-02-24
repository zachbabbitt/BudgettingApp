package com.example.budgettingtogether.storage.sync

import android.util.Log
import com.example.budgettingtogether.core.AppDatabase
import com.example.budgettingtogether.storage.source.LocalBudgetLimitSource
import com.example.budgettingtogether.storage.source.LocalCategorySource
import com.example.budgettingtogether.storage.source.LocalExpenseSource
import com.example.budgettingtogether.storage.source.LocalIncomeSource
import com.example.budgettingtogether.storage.source.RemoteBudgetLimitSource
import com.example.budgettingtogether.storage.source.RemoteCategorySource
import com.example.budgettingtogether.storage.source.RemoteExpenseSource
import com.example.budgettingtogether.storage.source.RemoteIncomeSource
import kotlinx.coroutines.flow.first

/**
 * Bidirectional sync between Room and Supabase.
 *
 * Call order on app resume (remote mode only):
 *   1. pushToRemote  — uploads any Room data that failed to sync during last session
 *   2. pullFromRemote — downloads own data from other devices + partner data
 *
 * Deletion handling for the current user's own data: items present in Room but
 * absent from Supabase are removed from Room, so deletes made on another device
 * appear here after sync.
 *
 * Partner data uses upsert-only (no deletion) — partners manage their own data.
 */
class SyncService(db: AppDatabase) {

    private val localExpense      = LocalExpenseSource(db.expenseDao())
    private val localIncome       = LocalIncomeSource(db.incomeDao())
    private val localBudgetLimit  = LocalBudgetLimitSource(db.budgetLimitDao())
    private val localCategory     = LocalCategorySource(db.categoryDao())

    private val remoteExpense     = RemoteExpenseSource()
    private val remoteIncome      = RemoteIncomeSource()
    private val remoteBudgetLimit = RemoteBudgetLimitSource()
    private val remoteCategory    = RemoteCategorySource()

    // ── Push: local Room → Supabase ───────────────────────────────────────────

    /**
     * Upserts all local data for [userGuid] to Supabase.
     * Reconciles any writes that failed in the background during the last session.
     */
    suspend fun pushToRemote(userGuid: String) {
        try {
            localExpense.getAllExpenses(userGuid).first()
                .forEach { remoteExpense.insert(it) }

            localIncome.getAllIncome(userGuid).first()
                .forEach { remoteIncome.insert(it) }

            localBudgetLimit.getAllLimitsOnce(userGuid)
                .forEach { remoteBudgetLimit.insertOrUpdate(it) }

            localCategory.getAllCategories(userGuid).first()
                .filter { !it.isDefault }
                .forEach { remoteCategory.insert(it) }

            Log.d("SyncService", "pushToRemote complete for $userGuid")
        } catch (e: Exception) {
            Log.w("SyncService", "pushToRemote failed: ${e.message}")
        }
    }

    // ── Pull: Supabase → local Room ───────────────────────────────────────────

    /**
     * Downloads data from Supabase into Room.
     * - Own data ([userGuid]): full replace — handles deletions made on other devices.
     * - Partner data ([pairedGuids] minus own): upsert only.
     */
    suspend fun pullFromRemote(userGuid: String, pairedGuids: List<String>) {
        try {
            pullExpenses(userGuid, pairedGuids)
            pullIncome(userGuid, pairedGuids)
            pullBudgetLimits(userGuid, pairedGuids)
            pullCategories(userGuid, pairedGuids)
            Log.d("SyncService", "pullFromRemote complete for $userGuid")
        } catch (e: Exception) {
            Log.w("SyncService", "pullFromRemote failed: ${e.message}")
        }
    }

    private suspend fun pullExpenses(userGuid: String, pairedGuids: List<String>) {
        // Own data: full replace so remote deletions propagate locally
        val ownRemote = remoteExpense.getAllExpenses(userGuid).first()
        val ownLocal  = localExpense.getAllExpenses(userGuid).first()
        val remoteIds = ownRemote.map { it.id }.toSet()
        ownLocal.filter { it.id !in remoteIds }.forEach { localExpense.delete(it) }
        ownRemote.forEach { localExpense.insert(it) }

        // Partner data: upsert only
        val partnerGuids = pairedGuids.filter { it != userGuid }
        if (partnerGuids.isNotEmpty()) {
            remoteExpense.getAllExpenses(partnerGuids).first()
                .forEach { localExpense.insert(it) }
        }
    }

    private suspend fun pullIncome(userGuid: String, pairedGuids: List<String>) {
        val ownRemote = remoteIncome.getAllIncome(userGuid).first()
        val ownLocal  = localIncome.getAllIncome(userGuid).first()
        val remoteIds = ownRemote.map { it.id }.toSet()
        ownLocal.filter { it.id !in remoteIds }.forEach { localIncome.delete(it) }
        ownRemote.forEach { localIncome.insert(it) }

        val partnerGuids = pairedGuids.filter { it != userGuid }
        if (partnerGuids.isNotEmpty()) {
            remoteIncome.getAllIncome(partnerGuids).first()
                .forEach { localIncome.insert(it) }
        }
    }

    private suspend fun pullBudgetLimits(userGuid: String, pairedGuids: List<String>) {
        val ownRemote = remoteBudgetLimit.getAllLimitsOnce(userGuid)
        val ownLocal  = localBudgetLimit.getAllLimitsOnce(userGuid)
        val remoteKeys = ownRemote.map { it.category }.toSet()
        ownLocal.filter { it.category !in remoteKeys }
            .forEach { localBudgetLimit.delete(it.userGuid, it.category) }
        ownRemote.forEach { localBudgetLimit.insertOrUpdate(it) }

        val partnerGuids = pairedGuids.filter { it != userGuid }
        if (partnerGuids.isNotEmpty()) {
            remoteBudgetLimit.getAllLimitsOnce(partnerGuids)
                .forEach { localBudgetLimit.insertOrUpdate(it) }
        }
    }

    private suspend fun pullCategories(userGuid: String, pairedGuids: List<String>) {
        val ownRemote = remoteCategory.getAllCategories(userGuid).first()
            .filter { !it.isDefault }
        val ownLocal  = localCategory.getAllCategories(userGuid).first()
            .filter { !it.isDefault }
        val remoteNames = ownRemote.map { it.name }.toSet()
        ownLocal.filter { it.name !in remoteNames }
            .forEach { localCategory.delete(it) }
        ownRemote.forEach { localCategory.insert(it) }

        val partnerGuids = pairedGuids.filter { it != userGuid }
        if (partnerGuids.isNotEmpty()) {
            remoteCategory.getAllCategories(partnerGuids).first()
                .filter { !it.isDefault }
                .forEach { localCategory.insert(it) }
        }
    }
}
