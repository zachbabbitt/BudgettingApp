package com.example.budgettingtogether

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import com.example.budgettingtogether.databinding.ActivityMainBinding
import com.example.budgettingtogether.databinding.ItemBudgetProgressBinding
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var expenseDao: ExpenseDao
    private lateinit var incomeDao: IncomeDao
    private lateinit var budgetLimitDao: BudgetLimitDao
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

    private var pendingCsvContent: String? = null

    private val createDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let { saveToUri(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val database = AppDatabase.getDatabase(this)
        expenseDao = database.expenseDao()
        incomeDao = database.incomeDao()
        budgetLimitDao = database.budgetLimitDao()

        setupToolbar()
        setupNavigationDrawer()
        setupButtons()
        observeData()
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

    private fun setupButtons() {
        binding.buttonExpenses.setOnClickListener {
            startActivity(Intent(this, ExpensesActivity::class.java))
        }

        binding.buttonIncome.setOnClickListener {
            startActivity(Intent(this, IncomeActivity::class.java))
        }

        binding.buttonSetLimits.setOnClickListener {
            startActivity(Intent(this, BudgetLimitsActivity::class.java))
        }

        binding.buttonAnalysis.setOnClickListener {
            startActivity(Intent(this, AnalysisActivity::class.java))
        }
    }

    private fun observeData() {
        lifecycleScope.launch {
            combine(
                expenseDao.getAllExpenses(),
                budgetLimitDao.getAllLimits(),
                incomeDao.getAllIncome()
            ) { expenses, limits, income ->
                Triple(expenses, limits, income)
            }.collectLatest { (expenses, limits, income) ->
                updateProgressBars(expenses, limits)
                updateWarnings(expenses, limits, income)
            }
        }
    }

    private fun updateWarnings(expenses: List<Expense>, limits: List<BudgetLimit>, income: List<Income>) {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        // Filter to current month
        val monthlyExpenses = expenses.filter { expense ->
            val expenseCalendar = Calendar.getInstance().apply { time = expense.date }
            expenseCalendar.get(Calendar.MONTH) == currentMonth &&
                expenseCalendar.get(Calendar.YEAR) == currentYear
        }

        val monthlyIncome = income.filter { inc ->
            val incomeCalendar = Calendar.getInstance().apply { time = inc.date }
            incomeCalendar.get(Calendar.MONTH) == currentMonth &&
                incomeCalendar.get(Calendar.YEAR) == currentYear
        }

        val totalLimits = limits.sumOf { it.limitAmount }
        val totalMonthlyExpenses = monthlyExpenses.sumOf { it.amount }
        val totalMonthlyIncome = monthlyIncome.sumOf { it.amount }

        // Warning 1: Budget limits exceed income
        val showWarning1 = totalLimits > totalMonthlyIncome && totalMonthlyIncome > 0
        binding.textViewWarning1.visibility = if (showWarning1) View.VISIBLE else View.GONE
        if (showWarning1) {
            binding.textViewWarning1.text = getString(
                R.string.warning_limits_exceed_income,
                currencyFormat.format(totalLimits),
                currencyFormat.format(totalMonthlyIncome)
            )
        }

        // Warning 2: Spending exceeds income
        val showWarning2 = totalMonthlyExpenses > totalMonthlyIncome && totalMonthlyIncome > 0
        binding.textViewWarning2.visibility = if (showWarning2) View.VISIBLE else View.GONE
        if (showWarning2) {
            binding.textViewWarning2.text = getString(
                R.string.warning_spending_exceeds_income,
                currencyFormat.format(totalMonthlyExpenses),
                currencyFormat.format(totalMonthlyIncome)
            )
        }

        // Show/hide the warning card
        binding.cardWarning.visibility = if (showWarning1 || showWarning2) View.VISIBLE else View.GONE
    }

    private fun updateProgressBars(expenses: List<Expense>, limits: List<BudgetLimit>) {
        binding.linearLayoutProgress.removeAllViews()

        if (limits.isEmpty()) {
            binding.scrollViewProgress.visibility = View.GONE
            binding.textViewNoLimits.visibility = View.VISIBLE
            return
        }

        binding.scrollViewProgress.visibility = View.VISIBLE
        binding.textViewNoLimits.visibility = View.GONE

        // Calculate spent per category
        val spentByCategory = expenses
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        // Create progress bars for each limit
        limits.sortedBy { it.category }.forEach { limit ->
            val spent = spentByCategory[limit.category] ?: 0.0
            addProgressBar(limit.category, spent, limit.limitAmount)
        }
    }

    private fun addProgressBar(category: String, spent: Double, limit: Double) {
        val itemBinding = ItemBudgetProgressBinding.inflate(
            LayoutInflater.from(this),
            binding.linearLayoutProgress,
            false
        )

        val percentage = ((spent / limit) * 100).coerceAtMost(100.0).toInt()
        val actualPercentage = ((spent / limit) * 100).toInt()

        itemBinding.textViewCategory.text = category
        itemBinding.textViewAmount.text = getString(
            R.string.spent_of_limit,
            currencyFormat.format(spent),
            currencyFormat.format(limit)
        )
        itemBinding.progressBar.progress = percentage

        // Set color based on percentage
        val progressDrawable = when {
            actualPercentage >= 100 -> R.drawable.progress_bar_red
            actualPercentage >= 75 -> R.drawable.progress_bar_yellow
            else -> R.drawable.progress_bar_green
        }
        itemBinding.progressBar.progressDrawable = getDrawable(progressDrawable)

        // Show percentage text
        itemBinding.textViewPercentage.text = if (actualPercentage > 100) {
            getString(R.string.over_budget)
        } else {
            "$actualPercentage%"
        }

        // Color the percentage text if over budget
        if (actualPercentage >= 100) {
            itemBinding.textViewPercentage.setTextColor(getColor(android.R.color.holo_red_dark))
            itemBinding.textViewPercentage.alpha = 1f
        }

        binding.linearLayoutProgress.addView(itemBinding.root)
    }
}
