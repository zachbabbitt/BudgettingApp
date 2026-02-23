package com.example.budgettingtogether.settings

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.budgettingtogether.R
import com.example.budgettingtogether.auth.SessionManager
import com.example.budgettingtogether.core.AppDatabase
import com.example.budgettingtogether.databinding.ActivityStorageSettingsBinding
import com.example.budgettingtogether.storage.StorageMigrationService
import com.example.budgettingtogether.storage.StorageMode
import com.example.budgettingtogether.storage.StoragePreferenceManager
import kotlinx.coroutines.launch

class StorageSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStorageSettingsBinding
    private lateinit var storagePrefManager: StoragePreferenceManager
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStorageSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        storagePrefManager = StoragePreferenceManager(this)
        sessionManager = SessionManager(this)

        setupToolbar()
        loadCurrentState()
        setupToggle()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.storage_settings_title)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun loadCurrentState() {
        val isRemote = storagePrefManager.isRemote()
        binding.switchStorageMode.isChecked = isRemote
        updateModeLabel(isRemote)
    }

    private fun setupToggle() {
        binding.switchStorageMode.setOnCheckedChangeListener { _, isChecked ->
            val targetMode = if (isChecked) StorageMode.REMOTE else StorageMode.LOCAL
            val confirmMessageRes = if (isChecked) R.string.migrate_to_remote_confirm
                                    else R.string.migrate_to_local_confirm
            confirmAndMigrate(targetMode, confirmMessageRes)
        }
    }

    private fun confirmAndMigrate(targetMode: StorageMode, confirmMessageRes: Int) {
        // Detach listener so the revert below doesn't re-trigger it
        binding.switchStorageMode.setOnCheckedChangeListener(null)
        // Revert switch visually until the user confirms
        binding.switchStorageMode.isChecked = !binding.switchStorageMode.isChecked

        AlertDialog.Builder(this)
            .setTitle(R.string.switch_storage_mode)
            .setMessage(confirmMessageRes)
            .setPositiveButton(R.string.confirm_switch) { _, _ -> performMigration(targetMode) }
            .setNegativeButton(R.string.cancel) { _, _ -> setupToggle() }
            .setOnCancelListener { setupToggle() }
            .show()
    }

    private fun performMigration(targetMode: StorageMode) {
        val userGuid = sessionManager.getUserGuid() ?: run {
            Toast.makeText(this, R.string.migration_failed, Toast.LENGTH_SHORT).show()
            setupToggle()
            return
        }

        val migrationService = StorageMigrationService(AppDatabase.getDatabase(this))

        binding.progressBarMigration.visibility = View.VISIBLE
        binding.switchStorageMode.isEnabled = false

        lifecycleScope.launch {
            try {
                when (targetMode) {
                    StorageMode.REMOTE -> migrationService.migrateLocalToRemote(userGuid)
                    StorageMode.LOCAL -> migrationService.migrateRemoteToLocal(userGuid)
                }
                storagePrefManager.setStorageMode(targetMode)
                binding.switchStorageMode.isChecked = (targetMode == StorageMode.REMOTE)
                updateModeLabel(targetMode == StorageMode.REMOTE)
                Toast.makeText(this@StorageSettingsActivity, R.string.migration_success, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(
                    this@StorageSettingsActivity,
                    getString(R.string.migration_failed, e.message),
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                binding.progressBarMigration.visibility = View.GONE
                binding.switchStorageMode.isEnabled = true
                setupToggle()
            }
        }
    }

    private fun updateModeLabel(isRemote: Boolean) {
        binding.textViewCurrentMode.text = getString(
            if (isRemote) R.string.mode_remote else R.string.mode_local
        )
    }
}
