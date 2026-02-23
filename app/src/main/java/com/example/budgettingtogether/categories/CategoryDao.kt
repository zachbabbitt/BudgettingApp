package com.example.budgettingtogether.categories

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE userGuid = :userGuid OR isDefault = 1 ORDER BY isDefault DESC, name ASC")
    fun getAllCategories(userGuid: String): Flow<List<Category>>

    @Query("SELECT name FROM categories WHERE userGuid = :userGuid OR isDefault = 1 ORDER BY isDefault DESC, name ASC")
    fun getAllCategoryNames(userGuid: String): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(category: Category)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(categories: List<Category>)

    @Delete
    suspend fun delete(category: Category)

    @Query("DELETE FROM categories WHERE name = :name AND isDefault = 0")
    suspend fun deleteByName(name: String)

    @Query("SELECT * FROM categories WHERE userGuid IN (:userGuids) OR isDefault = 1 ORDER BY isDefault DESC, name ASC")
    fun getAllCategories(userGuids: List<String>): Flow<List<Category>>

    @Query("SELECT name FROM categories WHERE userGuid IN (:userGuids) OR isDefault = 1 ORDER BY isDefault DESC, name ASC")
    fun getAllCategoryNames(userGuids: List<String>): Flow<List<String>>
}
