package com.example.budgettingtogether.auth

import android.content.Context
import com.example.budgettingtogether.core.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.runBlocking

class SessionManager(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveSession(userId: String, userGuid: String) {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_USER_ID, userId)
            .putString(KEY_USER_GUID, userGuid)
            .apply()
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun getUserId(): String? {
        return prefs.getString(KEY_USER_ID, null)
    }

    fun getUserGuid(): String? {
        return prefs.getString(KEY_USER_GUID, null)
    }

    fun clearSession() {
        runBlocking {
            try { SupabaseClientProvider.client.auth.signOut() } catch (_: Exception) {}
        }
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .remove(KEY_USER_ID)
            .remove(KEY_USER_GUID)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "auth_prefs"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_GUID = "user_guid"
    }
}
