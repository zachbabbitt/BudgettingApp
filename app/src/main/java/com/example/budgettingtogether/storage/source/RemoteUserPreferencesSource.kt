package com.example.budgettingtogether.storage.source

import com.example.budgettingtogether.core.SupabaseClientProvider
import com.example.budgettingtogether.storage.dto.UserPreferencesDto
import com.example.budgettingtogether.util.UserPreferences
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class RemoteUserPreferencesSource : IUserPreferencesSource {

    private val db get() = SupabaseClientProvider.client

    override fun getPreferences(userGuid: String): Flow<UserPreferences?> = flow {
        emit(fetchPreferences(userGuid))
    }

    override suspend fun getPreferencesOnce(userGuid: String): UserPreferences? =
        fetchPreferences(userGuid)

    override suspend fun savePreferences(preferences: UserPreferences) {
        db.from("user_preferences").upsert(UserPreferencesDto.fromEntity(preferences))
    }

    override suspend fun updateLastRecurringGeneration(userGuid: String, month: Int, year: Int) {
        val current = fetchPreferences(userGuid) ?: UserPreferences(userGuid = userGuid)
        val updated = current.copy(
            lastRecurringGenerationMonth = month,
            lastRecurringGenerationYear = year
        )
        db.from("user_preferences").upsert(UserPreferencesDto.fromEntity(updated))
    }

    private suspend fun fetchPreferences(userGuid: String): UserPreferences? =
        db.from("user_preferences").select {
            filter { eq("user_guid", userGuid) }
        }.decodeList<UserPreferencesDto>().firstOrNull()?.toEntity()
}
