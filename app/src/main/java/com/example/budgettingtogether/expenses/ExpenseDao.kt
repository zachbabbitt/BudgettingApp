package com.example.budgettingtogether.expenses

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE recurringType != 'NONE' ORDER BY date DESC")
    fun getRecurringExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE recurringType = :type ORDER BY date DESC")
    fun getExpensesByRecurringType(type: String): Flow<List<Expense>>

    @Insert
    suspend fun insert(expense: Expense)

    @Delete
    suspend fun delete(expense: Expense)

    @Query("SELECT SUM(amount) FROM expenses")
    fun getTotalAmount(): Flow<Double?>

    @Query("SELECT * FROM expenses WHERE recurringType = 'MONTHLY'")
    suspend fun getMonthlyRecurringExpensesOnce(): List<Expense>

    @Query("SELECT COUNT(*) FROM expenses WHERE title = :title AND category = :category AND recurringType = :recurringType AND date >= :monthStart AND date < :monthEnd")
    suspend fun countMatchingExpensesInMonth(title: String, category: String, recurringType: String, monthStart: Long, monthEnd: Long): Int
}
