package com.example.budgettingtogether.storage

import com.example.budgettingtogether.core.AppDatabase
import com.example.budgettingtogether.storage.source.IBudgetLimitSource
import com.example.budgettingtogether.storage.source.ICategorySource
import com.example.budgettingtogether.storage.source.IExpenseSource
import com.example.budgettingtogether.storage.source.IIncomeSource
import com.example.budgettingtogether.storage.source.IUserPreferencesSource
import com.example.budgettingtogether.storage.source.CachedBudgetLimitSource
import com.example.budgettingtogether.storage.source.CachedCategorySource
import com.example.budgettingtogether.storage.source.CachedExpenseSource
import com.example.budgettingtogether.storage.source.CachedIncomeSource
import com.example.budgettingtogether.storage.source.LocalBudgetLimitSource
import com.example.budgettingtogether.storage.source.LocalCategorySource
import com.example.budgettingtogether.storage.source.LocalExpenseSource
import com.example.budgettingtogether.storage.source.LocalIncomeSource
import com.example.budgettingtogether.storage.source.LocalUserPreferencesSource
import com.example.budgettingtogether.storage.source.RemoteBudgetLimitSource
import com.example.budgettingtogether.storage.source.RemoteCategorySource
import com.example.budgettingtogether.storage.source.RemoteExpenseSource
import com.example.budgettingtogether.storage.source.RemoteIncomeSource

class AppDataSource(
    private val db: AppDatabase,
    private val prefManager: StoragePreferenceManager
) {
    val expenseSource: IExpenseSource
        get() = if (prefManager.isRemote())
                    CachedExpenseSource(LocalExpenseSource(db.expenseDao()), RemoteExpenseSource())
                else LocalExpenseSource(db.expenseDao())

    val incomeSource: IIncomeSource
        get() = if (prefManager.isRemote())
                    CachedIncomeSource(LocalIncomeSource(db.incomeDao()), RemoteIncomeSource())
                else LocalIncomeSource(db.incomeDao())

    val budgetLimitSource: IBudgetLimitSource
        get() = if (prefManager.isRemote())
                    CachedBudgetLimitSource(LocalBudgetLimitSource(db.budgetLimitDao()), RemoteBudgetLimitSource())
                else LocalBudgetLimitSource(db.budgetLimitDao())

    val categorySource: ICategorySource
        get() = if (prefManager.isRemote())
                    CachedCategorySource(LocalCategorySource(db.categoryDao()), RemoteCategorySource())
                else LocalCategorySource(db.categoryDao())

    // User preferences stay local — they track per-device state (recurring generation timestamps)
    val userPreferencesSource: IUserPreferencesSource
        get() = LocalUserPreferencesSource(db.userPreferencesDao())
}
