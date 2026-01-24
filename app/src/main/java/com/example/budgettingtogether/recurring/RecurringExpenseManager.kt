package com.example.budgettingtogether.recurring

import com.example.budgettingtogether.expenses.Expense
import com.example.budgettingtogether.expenses.ExpenseDao
import com.example.budgettingtogether.util.RecurringType
import com.example.budgettingtogether.util.UserPreferencesDao
import java.util.Calendar
import java.util.Date

class RecurringExpenseManager(
    private val expenseDao: ExpenseDao,
    private val userPreferencesDao: UserPreferencesDao
) {
    suspend fun generateMonthlyRecurringExpensesIfNeeded() {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        val preferences = userPreferencesDao.getPreferencesOnce() ?: return

        // Check if we already generated for this month
        if (preferences.lastRecurringGenerationMonth == currentMonth &&
            preferences.lastRecurringGenerationYear == currentYear) {
            return
        }

        // Get monthly recurring templates
        val monthlyTemplates = expenseDao.getMonthlyRecurringExpensesOnce()

        // Calculate month boundaries for duplicate checking
        val monthStart = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, currentMonth)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val monthEnd = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, currentMonth + 1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // Create new expenses from templates (only if not already created this month)
        for (template in monthlyTemplates) {
            val existingCount = expenseDao.countMatchingExpensesInMonth(
                title = template.title,
                category = template.category,
                recurringType = RecurringType.MONTHLY.name,
                monthStart = monthStart,
                monthEnd = monthEnd
            )

            // Only create if no matching expense exists this month yet
            if (existingCount == 0) {
                // Create a new expense entry based on the template
                val newExpense = Expense(
                    title = template.title,
                    amount = template.amount,
                    category = template.category,
                    date = Date(),
                    recurringType = RecurringType.MONTHLY,
                    originalAmount = template.originalAmount,
                    originalCurrency = template.originalCurrency
                )
                expenseDao.insert(newExpense)
            }
        }

        // Update tracking in preferences
        userPreferencesDao.updateLastRecurringGeneration(currentMonth, currentYear)
    }
}
