package com.example.budgettingtogether.storage.source

import com.example.budgettingtogether.categories.Category
import com.example.budgettingtogether.core.SupabaseClientProvider
import com.example.budgettingtogether.storage.dto.CategoryDto
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class RemoteCategorySource : ICategorySource {

    private val db get() = SupabaseClientProvider.client

    override fun getAllCategories(userGuid: String): Flow<List<Category>> = flow {
        emit(db.from("categories").select {
            filter { or { eq("user_guid", userGuid); eq("is_default", true) } }
        }.decodeList<CategoryDto>().map { it.toEntity() }
            .sortedWith(compareByDescending<Category> { it.isDefault }.thenBy { it.name }))
    }

    override fun getAllCategoryNames(userGuid: String): Flow<List<String>> = flow {
        emit(db.from("categories").select {
            filter { or { eq("user_guid", userGuid); eq("is_default", true) } }
        }.decodeList<CategoryDto>().map { it.name }
            .sortedWith(Comparator { a, b -> a.compareTo(b) }))
    }

    override suspend fun insert(category: Category) {
        db.from("categories").upsert(CategoryDto.fromEntity(category))
    }

    override suspend fun insertAll(categories: List<Category>) {
        if (categories.isNotEmpty()) {
            db.from("categories").upsert(categories.map { CategoryDto.fromEntity(it) })
        }
    }

    override suspend fun delete(category: Category) {
        db.from("categories").delete {
            filter { eq("name", category.name); eq("user_guid", category.userGuid) }
        }
    }

    override suspend fun deleteByName(name: String) {
        db.from("categories").delete {
            filter { eq("name", name); eq("is_default", false) }
        }
    }

    override fun getAllCategories(userGuids: List<String>): Flow<List<Category>> = flow {
        emit(db.from("categories").select {
            filter { or { isIn("user_guid", userGuids); eq("is_default", true) } }
        }.decodeList<CategoryDto>().map { it.toEntity() }
            .sortedWith(compareByDescending<Category> { it.isDefault }.thenBy { it.name }))
    }

    override fun getAllCategoryNames(userGuids: List<String>): Flow<List<String>> = flow {
        emit(db.from("categories").select {
            filter { or { isIn("user_guid", userGuids); eq("is_default", true) } }
        }.decodeList<CategoryDto>().map { it.name }
            .sortedWith(Comparator { a, b -> a.compareTo(b) }))
    }
}
