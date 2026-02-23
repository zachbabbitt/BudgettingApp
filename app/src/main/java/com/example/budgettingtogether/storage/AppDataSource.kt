package com.example.budgettingtogether.storage

import com.example.budgettingtogether.core.AppDatabase
import com.example.budgettingtogether.storage.source.IBudgetLimitSource
import com.example.budgettingtogether.storage.source.ICategorySource
import com.example.budgettingtogether.storage.source.IExpenseSource
import com.example.budgettingtogether.storage.source.IIncomeSource
import com.example.budgettingtogether.storage.source.IUserPreferencesSource
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

class AppDataSource(
    private val db: AppDatabase,
    private val prefManager: StoragePreferenceManager
) {
    val expenseSource: IExpenseSource
        get() = if (prefManager.isRemote()) RemoteExpenseSource()
                else LocalExpenseSource(db.expenseDao())

    val incomeSource: IIncomeSource
        get() = if (prefManager.isRemote()) RemoteIncomeSource()
                else LocalIncomeSource(db.incomeDao())

    val budgetLimitSource: IBudgetLimitSource
        get() = if (prefManager.isRemote()) RemoteBudgetLimitSource()
                else LocalBudgetLimitSource(db.budgetLimitDao())

    val categorySource: ICategorySource
        get() = if (prefManager.isRemote()) RemoteCategorySource()
                else LocalCategorySource(db.categoryDao())

    val userPreferencesSource: IUserPreferencesSource
        get() = if (prefManager.isRemote()) RemoteUserPreferencesSource()
                else LocalUserPreferencesSource(db.userPreferencesDao())
}
