package com.example.budgettingtogether.storage.sync

import android.util.Log
import com.example.budgettingtogether.core.SupabaseClientProvider
import com.example.budgettingtogether.storage.dto.BudgetLimitDto
import com.example.budgettingtogether.storage.dto.CategoryDto
import com.example.budgettingtogether.storage.dto.ExpenseDto
import com.example.budgettingtogether.storage.dto.IncomeDto
import com.example.budgettingtogether.storage.source.LocalBudgetLimitSource
import com.example.budgettingtogether.storage.source.LocalCategorySource
import com.example.budgettingtogether.storage.source.LocalExpenseSource
import com.example.budgettingtogether.storage.source.LocalIncomeSource
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive

class RealtimeListener(
    private val localExpense: LocalExpenseSource,
    private val localIncome: LocalIncomeSource,
    private val localBudgetLimit: LocalBudgetLimitSource,
    private val localCategory: LocalCategorySource,
    private val scope: CoroutineScope
) {
    private val supabase = SupabaseClientProvider.client
    private val channels = mutableListOf<RealtimeChannel>()
    private val jobs = mutableListOf<Job>()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun start(pairedGuids: List<String>) {
        stop()
        if (pairedGuids.isEmpty()) return
        setupExpensesChannel(pairedGuids)
        setupIncomeChannel(pairedGuids)
        setupBudgetLimitsChannel(pairedGuids)
        setupCategoriesChannel(pairedGuids)
        Log.d("RealtimeListener", "Realtime started for ${pairedGuids.size} user(s)")
    }

    suspend fun stop() {
        jobs.forEach { it.cancel() }
        jobs.clear()
        for (ch in channels) {
            try { ch.unsubscribe() } catch (_: Exception) {}
        }
        channels.clear()
        Log.d("RealtimeListener", "Realtime stopped")
    }

    private suspend fun setupExpensesChannel(pairedGuids: List<String>) {
        val channel = supabase.channel("bt-expenses")

        val insertJob = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "expenses"
        }.onEach { action ->
            try {
                val dto = json.decodeFromJsonElement<ExpenseDto>(action.record)
                if (dto.userGuid in pairedGuids) localExpense.insert(dto.toEntity())
            } catch (e: Exception) {
                Log.w("RealtimeListener", "expenses insert: ${e.message}")
            }
        }.launchIn(scope)

        val updateJob = channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
            table = "expenses"
        }.onEach { action ->
            try {
                val dto = json.decodeFromJsonElement<ExpenseDto>(action.record)
                if (dto.userGuid in pairedGuids) localExpense.insert(dto.toEntity())
            } catch (e: Exception) {
                Log.w("RealtimeListener", "expenses update: ${e.message}")
            }
        }.launchIn(scope)

        val deleteJob = channel.postgresChangeFlow<PostgresAction.Delete>(schema = "public") {
            table = "expenses"
        }.onEach { action ->
            try {
                val id = action.oldRecord["id"]?.jsonPrimitive?.content ?: return@onEach
                localExpense.deleteById(id)
            } catch (e: Exception) {
                Log.w("RealtimeListener", "expenses delete: ${e.message}")
            }
        }.launchIn(scope)

        channel.subscribe()
        channels.add(channel)
        jobs.addAll(listOf(insertJob, updateJob, deleteJob))
    }

    private suspend fun setupIncomeChannel(pairedGuids: List<String>) {
        val channel = supabase.channel("bt-income")

        val insertJob = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "income"
        }.onEach { action ->
            try {
                val dto = json.decodeFromJsonElement<IncomeDto>(action.record)
                if (dto.userGuid in pairedGuids) localIncome.insert(dto.toEntity())
            } catch (e: Exception) {
                Log.w("RealtimeListener", "income insert: ${e.message}")
            }
        }.launchIn(scope)

        val updateJob = channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
            table = "income"
        }.onEach { action ->
            try {
                val dto = json.decodeFromJsonElement<IncomeDto>(action.record)
                if (dto.userGuid in pairedGuids) localIncome.insert(dto.toEntity())
            } catch (e: Exception) {
                Log.w("RealtimeListener", "income update: ${e.message}")
            }
        }.launchIn(scope)

        val deleteJob = channel.postgresChangeFlow<PostgresAction.Delete>(schema = "public") {
            table = "income"
        }.onEach { action ->
            try {
                val id = action.oldRecord["id"]?.jsonPrimitive?.content ?: return@onEach
                localIncome.deleteById(id)
            } catch (e: Exception) {
                Log.w("RealtimeListener", "income delete: ${e.message}")
            }
        }.launchIn(scope)

        channel.subscribe()
        channels.add(channel)
        jobs.addAll(listOf(insertJob, updateJob, deleteJob))
    }

    private suspend fun setupBudgetLimitsChannel(pairedGuids: List<String>) {
        val channel = supabase.channel("bt-budget-limits")

        val insertJob = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "budget_limits"
        }.onEach { action ->
            try {
                val dto = json.decodeFromJsonElement<BudgetLimitDto>(action.record)
                if (dto.userGuid in pairedGuids) localBudgetLimit.insertOrUpdate(dto.toEntity())
            } catch (e: Exception) {
                Log.w("RealtimeListener", "budget_limits insert: ${e.message}")
            }
        }.launchIn(scope)

        val updateJob = channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
            table = "budget_limits"
        }.onEach { action ->
            try {
                val dto = json.decodeFromJsonElement<BudgetLimitDto>(action.record)
                if (dto.userGuid in pairedGuids) localBudgetLimit.insertOrUpdate(dto.toEntity())
            } catch (e: Exception) {
                Log.w("RealtimeListener", "budget_limits update: ${e.message}")
            }
        }.launchIn(scope)

        val deleteJob = channel.postgresChangeFlow<PostgresAction.Delete>(schema = "public") {
            table = "budget_limits"
        }.onEach { action ->
            try {
                val userGuid = action.oldRecord["user_guid"]?.jsonPrimitive?.content ?: return@onEach
                val category = action.oldRecord["category"]?.jsonPrimitive?.content ?: return@onEach
                localBudgetLimit.delete(userGuid, category)
            } catch (e: Exception) {
                Log.w("RealtimeListener", "budget_limits delete: ${e.message}")
            }
        }.launchIn(scope)

        channel.subscribe()
        channels.add(channel)
        jobs.addAll(listOf(insertJob, updateJob, deleteJob))
    }

    private suspend fun setupCategoriesChannel(pairedGuids: List<String>) {
        val channel = supabase.channel("bt-categories")

        val insertJob = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "categories"
        }.onEach { action ->
            try {
                val dto = json.decodeFromJsonElement<CategoryDto>(action.record)
                if (dto.userGuid in pairedGuids) localCategory.insert(dto.toEntity())
            } catch (e: Exception) {
                Log.w("RealtimeListener", "categories insert: ${e.message}")
            }
        }.launchIn(scope)

        val updateJob = channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
            table = "categories"
        }.onEach { action ->
            try {
                val dto = json.decodeFromJsonElement<CategoryDto>(action.record)
                if (dto.userGuid in pairedGuids) localCategory.insert(dto.toEntity())
            } catch (e: Exception) {
                Log.w("RealtimeListener", "categories update: ${e.message}")
            }
        }.launchIn(scope)

        val deleteJob = channel.postgresChangeFlow<PostgresAction.Delete>(schema = "public") {
            table = "categories"
        }.onEach { action ->
            try {
                val name = action.oldRecord["name"]?.jsonPrimitive?.content ?: return@onEach
                localCategory.deleteByName(name)
            } catch (e: Exception) {
                Log.w("RealtimeListener", "categories delete: ${e.message}")
            }
        }.launchIn(scope)

        channel.subscribe()
        channels.add(channel)
        jobs.addAll(listOf(insertJob, updateJob, deleteJob))
    }
}
