package com.example.budgettingtogether.util

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserPreferencesDao {
    @Query("SELECT * FROM user_preferences WHERE userGuid = :userGuid LIMIT 1")
    fun getPreferences(userGuid: String): Flow<UserPreferences?>

    @Query("SELECT * FROM user_preferences WHERE userGuid = :userGuid LIMIT 1")
    suspend fun getPreferencesOnce(userGuid: String): UserPreferences?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePreferences(preferences: UserPreferences)

    @Query("UPDATE user_preferences SET lastRecurringGenerationMonth = :month, lastRecurringGenerationYear = :year WHERE userGuid = :userGuid")
    suspend fun updateLastRecurringGeneration(userGuid: String, month: Int, year: Int)
}
