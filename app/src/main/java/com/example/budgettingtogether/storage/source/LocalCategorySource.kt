package com.example.budgettingtogether.storage.source

import com.example.budgettingtogether.categories.Category
import com.example.budgettingtogether.categories.CategoryDao
import kotlinx.coroutines.flow.Flow

class LocalCategorySource(private val dao: CategoryDao) : ICategorySource {
    override fun getAllCategories(userGuid: String) = dao.getAllCategories(userGuid)
    override fun getAllCategoryNames(userGuid: String) = dao.getAllCategoryNames(userGuid)
    override suspend fun insert(category: Category) = dao.insert(category)
    override suspend fun insertAll(categories: List<Category>) = dao.insertAll(categories)
    override suspend fun delete(category: Category) = dao.delete(category)
    override suspend fun deleteByName(name: String) = dao.deleteByName(name)

    override fun getAllCategories(userGuids: List<String>) = dao.getAllCategories(userGuids)
    override fun getAllCategoryNames(userGuids: List<String>) = dao.getAllCategoryNames(userGuids)
}
