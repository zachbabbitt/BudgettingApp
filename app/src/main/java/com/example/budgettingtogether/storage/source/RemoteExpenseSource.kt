package com.example.budgettingtogether.storage.source

import com.example.budgettingtogether.core.SupabaseClientProvider
import com.example.budgettingtogether.expenses.Expense
import com.example.budgettingtogether.storage.dto.ExpenseDto
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class RemoteExpenseSource : IExpenseSource {

    private val db get() = SupabaseClientProvider.client

    override fun getAllExpenses(userGuid: String): Flow<List<Expense>> = flow {
        emit(db.from("expenses").select {
            filter { eq("user_guid", userGuid) }
        }.decodeList<ExpenseDto>().map { it.toEntity() }.sortedByDescending { it.date })
    }

    override fun getRecurringExpenses(userGuid: String): Flow<List<Expense>> = flow {
        emit(db.from("expenses").select {
            filter { eq("user_guid", userGuid); neq("recurring_type", "NONE") }
        }.decodeList<ExpenseDto>().map { it.toEntity() }.sortedByDescending { it.date })
    }

    override fun getExpensesByRecurringType(userGuid: String, type: String): Flow<List<Expense>> = flow {
        emit(db.from("expenses").select {
            filter { eq("user_guid", userGuid); eq("recurring_type", type) }
        }.decodeList<ExpenseDto>().map { it.toEntity() }.sortedByDescending { it.date })
    }

    override suspend fun insert(expense: Expense) {
        db.from("expenses").upsert(ExpenseDto.fromEntity(expense))
    }

    override suspend fun delete(expense: Expense) {
        db.from("expenses").delete { filter { eq("id", expense.id) } }
    }

    override fun getTotalAmount(userGuid: String): Flow<Double?> = flow {
        val rows = db.from("expenses").select {
            filter { eq("user_guid", userGuid) }
        }.decodeList<ExpenseDto>()
        emit(if (rows.isEmpty()) null else rows.sumOf { it.amount })
    }

    override suspend fun getMonthlyRecurringExpensesOnce(userGuid: String): List<Expense> =
        db.from("expenses").select {
            filter { eq("user_guid", userGuid); eq("recurring_type", "MONTHLY") }
        }.decodeList<ExpenseDto>().map { it.toEntity() }

    override suspend fun countMatchingExpensesInMonth(
        userGuid: String, title: String, category: String,
        recurringType: String, monthStart: Long, monthEnd: Long
    ): Int = db.from("expenses").select {
        filter {
            eq("user_guid", userGuid)
            eq("title", title)
            eq("category", category)
            eq("recurring_type", recurringType)
            gte("date", monthStart)
            lt("date", monthEnd)
        }
    }.decodeList<ExpenseDto>().size

    override fun getAllExpenses(userGuids: List<String>): Flow<List<Expense>> = flow {
        emit(db.from("expenses").select {
            filter { isIn("user_guid", userGuids) }
        }.decodeList<ExpenseDto>().map { it.toEntity() }.sortedByDescending { it.date })
    }

    override fun getRecurringExpenses(userGuids: List<String>): Flow<List<Expense>> = flow {
        emit(db.from("expenses").select {
            filter { isIn("user_guid", userGuids); neq("recurring_type", "NONE") }
        }.decodeList<ExpenseDto>().map { it.toEntity() }.sortedByDescending { it.date })
    }

    override fun getTotalAmount(userGuids: List<String>): Flow<Double?> = flow {
        val rows = db.from("expenses").select {
            filter { isIn("user_guid", userGuids) }
        }.decodeList<ExpenseDto>()
        emit(if (rows.isEmpty()) null else rows.sumOf { it.amount })
    }

    override fun getExpensesByRecurringType(userGuids: List<String>, type: String): Flow<List<Expense>> = flow {
        emit(db.from("expenses").select {
            filter { isIn("user_guid", userGuids); eq("recurring_type", type) }
        }.decodeList<ExpenseDto>().map { it.toEntity() }.sortedByDescending { it.date })
    }

    override suspend fun getMonthlyRecurringExpensesOnce(userGuids: List<String>): List<Expense> =
        db.from("expenses").select {
            filter { isIn("user_guid", userGuids); eq("recurring_type", "MONTHLY") }
        }.decodeList<ExpenseDto>().map { it.toEntity() }

    override suspend fun countMatchingExpensesInMonth(
        userGuids: List<String>, title: String, category: String,
        recurringType: String, monthStart: Long, monthEnd: Long
    ): Int = db.from("expenses").select {
        filter {
            isIn("user_guid", userGuids)
            eq("title", title)
            eq("category", category)
            eq("recurring_type", recurringType)
            gte("date", monthStart)
            lt("date", monthEnd)
        }
    }.decodeList<ExpenseDto>().size
}
