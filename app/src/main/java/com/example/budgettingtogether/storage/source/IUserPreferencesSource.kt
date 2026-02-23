package com.example.budgettingtogether.storage.source

import com.example.budgettingtogether.util.UserPreferences
import kotlinx.coroutines.flow.Flow

interface IUserPreferencesSource {
    fun getPreferences(userGuid: String): Flow<UserPreferences?>
    suspend fun getPreferencesOnce(userGuid: String): UserPreferences?
    suspend fun savePreferences(preferences: UserPreferences)
    suspend fun updateLastRecurringGeneration(userGuid: String, month: Int, year: Int)
}
