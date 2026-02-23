package com.example.budgettingtogether.storage

import android.content.Context

enum class StorageMode { LOCAL, REMOTE }

class StoragePreferenceManager(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getStorageMode(): StorageMode {
        val name = prefs.getString(KEY_MODE, StorageMode.LOCAL.name) ?: StorageMode.LOCAL.name
        return StorageMode.valueOf(name)
    }

    fun setStorageMode(mode: StorageMode) {
        prefs.edit().putString(KEY_MODE, mode.name).apply()
    }

    fun isRemote(): Boolean = getStorageMode() == StorageMode.REMOTE

    companion object {
        private const val PREFS_NAME = "storage_prefs"
        private const val KEY_MODE = "storage_mode"
    }
}
