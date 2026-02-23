package com.example.budgettingtogether.storage

import com.example.budgettingtogether.core.AppDatabase
import com.example.budgettingtogether.storage.source.LocalBudgetLimitSource
import com.example.budgettingtogether.storage.source.LocalCategorySource
import com.example.budgettingtogether.storage.source.LocalExpenseSource
import com.example.budgettingtogether.storage.source.LocalIncomeSource
import com.example.budgettingtogether.storage.source.LocalUserPreferencesSource
import com.example.budgettingtogether.storage.source.RemoteBudgetLimitSource
import com.example.budgettingtogether.storage.source.RemoteCategorySource
import com.example.budgettingtogether.storage.source.RemoteExpenseSource
import com.example.budgettingtogether.storage.source.RemoteIncomeSource
import com.example.budgettingtogether.storage.source.RemoteUserPreferencesSource
import kotlinx.coroutines.flow.first

class StorageMigrationService(db: AppDatabase) {

    private val localExpense = LocalExpenseSource(db.expenseDao())
    private val localIncome = LocalIncomeSource(db.incomeDao())
    private val localLimit = LocalBudgetLimitSource(db.budgetLimitDao())
    private val localCategory = LocalCategorySource(db.categoryDao())
    private val localPrefs = LocalUserPreferencesSource(db.userPreferencesDao())

    private val remoteExpense = RemoteExpenseSource()
    private val remoteIncome = RemoteIncomeSource()
    private val remoteLimit = RemoteBudgetLimitSource()
    private val remoteCategory = RemoteCategorySource()
    private val remotePrefs = RemoteUserPreferencesSource()

    /**
     * Copies all local Room data to Supabase. Called before switching to REMOTE mode.
     * Uses upsert semantics — safe to call multiple times.
     */
    suspend fun migrateLocalToRemote(userGuid: String) {
        localExpense.getAllExpenses(userGuid).first().forEach { remoteExpense.insert(it) }
        localIncome.getAllIncome(userGuid).first().forEach { remoteIncome.insert(it) }
        localLimit.getAllLimitsOnce(userGuid).forEach { remoteLimit.insertOrUpdate(it) }
        localCategory.getAllCategories(userGuid).first()
            .filter { !it.isDefault }
            .forEach { remoteCategory.insert(it) }
        localPrefs.getPreferencesOnce(userGuid)?.let { remotePrefs.savePreferences(it) }
    }

    /**
     * Copies all Supabase data down into Room. Called before switching to LOCAL mode.
     * Uses upsert semantics — safe to call multiple times.
     */
    suspend fun migrateRemoteToLocal(userGuid: String) {
        remoteExpense.getAllExpenses(userGuid).first().forEach { localExpense.insert(it) }
        remoteIncome.getAllIncome(userGuid).first().forEach { localIncome.insert(it) }
        remoteLimit.getAllLimitsOnce(userGuid).forEach { localLimit.insertOrUpdate(it) }
        remoteCategory.getAllCategories(userGuid).first()
            .filter { !it.isDefault }
            .forEach { localCategory.insert(it) }
        remotePrefs.getPreferencesOnce(userGuid)?.let { localPrefs.savePreferences(it) }
    }
}
