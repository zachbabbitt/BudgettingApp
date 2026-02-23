package com.example.budgettingtogether.expenses

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE userGuid = :userGuid ORDER BY date DESC")
    fun getAllExpenses(userGuid: String): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE userGuid = :userGuid AND recurringType != 'NONE' ORDER BY date DESC")
    fun getRecurringExpenses(userGuid: String): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE userGuid = :userGuid AND recurringType = :type ORDER BY date DESC")
    fun getExpensesByRecurringType(userGuid: String, type: String): Flow<List<Expense>>

    @Insert
    suspend fun insert(expense: Expense)

    @Delete
    suspend fun delete(expense: Expense)

    @Query("SELECT SUM(amount) FROM expenses WHERE userGuid = :userGuid")
    fun getTotalAmount(userGuid: String): Flow<Double?>

    @Query("SELECT * FROM expenses WHERE userGuid = :userGuid AND recurringType = 'MONTHLY'")
    suspend fun getMonthlyRecurringExpensesOnce(userGuid: String): List<Expense>

    @Query("SELECT COUNT(*) FROM expenses WHERE userGuid = :userGuid AND title = :title AND category = :category AND recurringType = :recurringType AND date >= :monthStart AND date < :monthEnd")
    suspend fun countMatchingExpensesInMonth(userGuid: String, title: String, category: String, recurringType: String, monthStart: Long, monthEnd: Long): Int
}
