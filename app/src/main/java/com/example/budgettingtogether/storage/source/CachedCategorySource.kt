package com.example.budgettingtogether.storage.source

import android.util.Log
import com.example.budgettingtogether.categories.Category
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@OptIn(DelicateCoroutinesApi::class)
class CachedCategorySource(
    private val local: LocalCategorySource,
    private val remote: RemoteCategorySource
) : ICategorySource {

    // ── Reads — always from Room ──────────────────────────────────────────────

    override fun getAllCategories(userGuid: String): Flow<List<Category>> =
        local.getAllCategories(userGuid)

    override fun getAllCategoryNames(userGuid: String): Flow<List<String>> =
        local.getAllCategoryNames(userGuid)

    override fun getAllCategories(userGuids: List<String>): Flow<List<Category>> =
        local.getAllCategories(userGuids)

    override fun getAllCategoryNames(userGuids: List<String>): Flow<List<String>> =
        local.getAllCategoryNames(userGuids)

    // ── Writes — Room first, Supabase in background ───────────────────────────

    override suspend fun insert(category: Category) {
        local.insert(category)
        GlobalScope.launch(Dispatchers.IO) {
            try { remote.insert(category) }
            catch (e: Exception) { Log.w("CachedCategory", "Remote insert failed: ${e.message}") }
        }
    }

    override suspend fun insertAll(categories: List<Category>) {
        local.insertAll(categories)
        GlobalScope.launch(Dispatchers.IO) {
            try { remote.insertAll(categories) }
            catch (e: Exception) { Log.w("CachedCategory", "Remote insertAll failed: ${e.message}") }
        }
    }

    override suspend fun delete(category: Category) {
        local.delete(category)
        GlobalScope.launch(Dispatchers.IO) {
            try { remote.delete(category) }
            catch (e: Exception) { Log.w("CachedCategory", "Remote delete failed: ${e.message}") }
        }
    }

    override suspend fun deleteByName(name: String) {
        local.deleteByName(name)
        GlobalScope.launch(Dispatchers.IO) {
            try { remote.deleteByName(name) }
            catch (e: Exception) { Log.w("CachedCategory", "Remote deleteByName failed: ${e.message}") }
        }
    }
}
