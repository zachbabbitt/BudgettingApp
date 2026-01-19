package com.example.budgettingtogether

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import com.example.budgettingtogether.databinding.ActivityMainBinding
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var expenseDao: ExpenseDao
    private lateinit var incomeDao: IncomeDao

    private var pendingCsvContent: String? = null

    private val createDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let { saveToUri(it) }
    }

    private val tabTitles = arrayOf(
        R.string.tab_home,
        R.string.tab_expenses,
        R.string.tab_income,
        R.string.tab_analysis
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val database = AppDatabase.getDatabase(this)
        expenseDao = database.expenseDao()
        incomeDao = database.incomeDao()

        setupToolbar()
        setupNavigationDrawer()
        setupViewPager()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
    }

    private fun setupNavigationDrawer() {
        val toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_budget_limits -> {
                    startActivity(Intent(this, BudgetLimitsActivity::class.java))
                }
                R.id.nav_currency -> {
                    startActivity(Intent(this, CurrencySettingsActivity::class.java))
                }
                R.id.nav_categories -> {
                    startActivity(Intent(this, CategoriesActivity::class.java))
                }
                R.id.nav_export_csv -> {
                    exportToCsv()
                }
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun setupViewPager() {
        val pagerAdapter = MainPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = getString(tabTitles[position])
        }.attach()
    }

    private fun exportToCsv() {
        lifecycleScope.launch {
            val expenses = expenseDao.getAllExpenses().first()
            val income = incomeDao.getAllIncome().first()

            if (expenses.isEmpty() && income.isEmpty()) {
                Toast.makeText(this@MainActivity, R.string.export_empty, Toast.LENGTH_SHORT).show()
                return@launch
            }

            pendingCsvContent = CsvExporter.export(expenses, income)

            AlertDialog.Builder(this@MainActivity)
                .setTitle(R.string.export_csv)
                .setItems(arrayOf(
                    getString(R.string.save_to_device),
                    getString(R.string.share)
                )) { _, which ->
                    when (which) {
                        0 -> saveToDevice()
                        1 -> shareCsv()
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    private fun saveToDevice() {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "budget_export_$timestamp.csv"
        createDocumentLauncher.launch(fileName)
    }

    private fun shareCsv() {
        val content = pendingCsvContent ?: return
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "budget_export_$timestamp.csv"

        val file = File(cacheDir, fileName)
        file.writeText(content)

        val uri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(shareIntent, getString(R.string.export_csv)))
        pendingCsvContent = null
    }

    private fun saveToUri(uri: Uri) {
        val content = pendingCsvContent ?: return
        try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(content.toByteArray())
            }
            Toast.makeText(this, R.string.export_success, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            pendingCsvContent = null
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
