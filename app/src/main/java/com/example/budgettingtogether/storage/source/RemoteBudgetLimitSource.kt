package com.example.budgettingtogether.storage.source

import com.example.budgettingtogether.core.SupabaseClientProvider
import com.example.budgettingtogether.limits.BudgetLimit
import com.example.budgettingtogether.storage.dto.BudgetLimitDto
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class RemoteBudgetLimitSource : IBudgetLimitSource {

    private val db get() = SupabaseClientProvider.client

    override fun getAllLimits(userGuid: String): Flow<List<BudgetLimit>> = flow {
        emit(db.from("budget_limits").select {
            filter { eq("user_guid", userGuid) }
        }.decodeList<BudgetLimitDto>().map { it.toEntity() }.sortedBy { it.category })
    }

    override suspend fun getAllLimitsOnce(userGuid: String): List<BudgetLimit> =
        db.from("budget_limits").select {
            filter { eq("user_guid", userGuid) }
        }.decodeList<BudgetLimitDto>().map { it.toEntity() }.sortedBy { it.category }

    override suspend fun getLimitForCategory(userGuid: String, category: String): BudgetLimit? =
        db.from("budget_limits").select {
            filter { eq("user_guid", userGuid); eq("category", category) }
        }.decodeList<BudgetLimitDto>().firstOrNull()?.toEntity()

    override suspend fun insertOrUpdate(budgetLimit: BudgetLimit) {
        db.from("budget_limits").upsert(BudgetLimitDto.fromEntity(budgetLimit))
    }

    override suspend fun updateAll(limits: List<BudgetLimit>) {
        if (limits.isNotEmpty()) {
            db.from("budget_limits").upsert(limits.map { BudgetLimitDto.fromEntity(it) })
        }
    }

    override suspend fun delete(userGuid: String, category: String) {
        db.from("budget_limits").delete {
            filter { eq("user_guid", userGuid); eq("category", category) }
        }
    }

    override fun getAllLimits(userGuids: List<String>): Flow<List<BudgetLimit>> = flow {
        emit(db.from("budget_limits").select {
            filter { isIn("user_guid", userGuids) }
        }.decodeList<BudgetLimitDto>().map { it.toEntity() }.sortedBy { it.category })
    }

    override suspend fun getAllLimitsOnce(userGuids: List<String>): List<BudgetLimit> =
        db.from("budget_limits").select {
            filter { isIn("user_guid", userGuids) }
        }.decodeList<BudgetLimitDto>().map { it.toEntity() }.sortedBy { it.category }

    override suspend fun getLimitForCategory(userGuids: List<String>, category: String): BudgetLimit? =
        db.from("budget_limits").select {
            filter { isIn("user_guid", userGuids); eq("category", category) }
        }.decodeList<BudgetLimitDto>().firstOrNull()?.toEntity()
}
