package com.example.budgettingtogether.storage.source

import com.example.budgettingtogether.util.UserPreferences
import com.example.budgettingtogether.util.UserPreferencesDao
import kotlinx.coroutines.flow.Flow

class LocalUserPreferencesSource(private val dao: UserPreferencesDao) : IUserPreferencesSource {
    override fun getPreferences(userGuid: String) = dao.getPreferences(userGuid)
    override suspend fun getPreferencesOnce(userGuid: String) = dao.getPreferencesOnce(userGuid)
    override suspend fun savePreferences(preferences: UserPreferences) = dao.savePreferences(preferences)
    override suspend fun updateLastRecurringGeneration(userGuid: String, month: Int, year: Int) =
        dao.updateLastRecurringGeneration(userGuid, month, year)
}
