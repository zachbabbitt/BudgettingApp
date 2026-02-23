package com.example.budgettingtogether.storage.source

import com.example.budgettingtogether.categories.Category
import kotlinx.coroutines.flow.Flow

interface ICategorySource {
    fun getAllCategories(userGuid: String): Flow<List<Category>>
    fun getAllCategoryNames(userGuid: String): Flow<List<String>>
    suspend fun insert(category: Category)
    suspend fun insertAll(categories: List<Category>)
    suspend fun delete(category: Category)
    suspend fun deleteByName(name: String)

    fun getAllCategories(userGuids: List<String>): Flow<List<Category>>
    fun getAllCategoryNames(userGuids: List<String>): Flow<List<String>>
}
