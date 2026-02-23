package com.example.budgettingtogether.storage.source

import com.example.budgettingtogether.core.SupabaseClientProvider
import com.example.budgettingtogether.income.Income
import com.example.budgettingtogether.storage.dto.IncomeDto
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class RemoteIncomeSource : IIncomeSource {

    private val db get() = SupabaseClientProvider.client

    override fun getAllIncome(userGuid: String): Flow<List<Income>> = flow {
        emit(db.from("income").select {
            filter { eq("user_guid", userGuid) }
        }.decodeList<IncomeDto>().map { it.toEntity() }.sortedByDescending { it.date })
    }

    override fun getRecurringIncome(userGuid: String): Flow<List<Income>> = flow {
        emit(db.from("income").select {
            filter { eq("user_guid", userGuid); neq("recurring_type", "NONE") }
        }.decodeList<IncomeDto>().map { it.toEntity() }.sortedByDescending { it.date })
    }

    override fun getIncomeByRecurringType(userGuid: String, type: String): Flow<List<Income>> = flow {
        emit(db.from("income").select {
            filter { eq("user_guid", userGuid); eq("recurring_type", type) }
        }.decodeList<IncomeDto>().map { it.toEntity() }.sortedByDescending { it.date })
    }

    override fun getIncomeBySource(userGuid: String, source: String): Flow<List<Income>> = flow {
        emit(db.from("income").select {
            filter { eq("user_guid", userGuid); eq("source", source) }
        }.decodeList<IncomeDto>().map { it.toEntity() }.sortedByDescending { it.date })
    }

    override suspend fun insert(income: Income) {
        db.from("income").upsert(IncomeDto.fromEntity(income))
    }

    override suspend fun delete(income: Income) {
        db.from("income").delete { filter { eq("id", income.id) } }
    }

    override fun getTotalAmount(userGuid: String): Flow<Double?> = flow {
        val rows = db.from("income").select {
            filter { eq("user_guid", userGuid) }
        }.decodeList<IncomeDto>()
        emit(if (rows.isEmpty()) null else rows.sumOf { it.amount })
    }

    override fun getAllIncome(userGuids: List<String>): Flow<List<Income>> = flow {
        emit(db.from("income").select {
            filter { isIn("user_guid", userGuids) }
        }.decodeList<IncomeDto>().map { it.toEntity() }.sortedByDescending { it.date })
    }

    override fun getRecurringIncome(userGuids: List<String>): Flow<List<Income>> = flow {
        emit(db.from("income").select {
            filter { isIn("user_guid", userGuids); neq("recurring_type", "NONE") }
        }.decodeList<IncomeDto>().map { it.toEntity() }.sortedByDescending { it.date })
    }

    override fun getTotalAmount(userGuids: List<String>): Flow<Double?> = flow {
        val rows = db.from("income").select {
            filter { isIn("user_guid", userGuids) }
        }.decodeList<IncomeDto>()
        emit(if (rows.isEmpty()) null else rows.sumOf { it.amount })
    }

    override fun getIncomeBySource(userGuids: List<String>, source: String): Flow<List<Income>> = flow {
        emit(db.from("income").select {
            filter { isIn("user_guid", userGuids); eq("source", source) }
        }.decodeList<IncomeDto>().map { it.toEntity() }.sortedByDescending { it.date })
    }

    override fun getIncomeByRecurringType(userGuids: List<String>, type: String): Flow<List<Income>> = flow {
        emit(db.from("income").select {
            filter { isIn("user_guid", userGuids); eq("recurring_type", type) }
        }.decodeList<IncomeDto>().map { it.toEntity() }.sortedByDescending { it.date })
    }
}
