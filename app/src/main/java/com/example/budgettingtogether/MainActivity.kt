package com.example.budgettingtogether

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import com.example.budgettingtogether.databinding.ActivityMainBinding
import com.example.budgettingtogether.databinding.ItemBudgetProgressBinding
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var expenseDao: ExpenseDao
    private lateinit var budgetLimitDao: BudgetLimitDao
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val database = AppDatabase.getDatabase(this)
        expenseDao = database.expenseDao()
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
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
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
                budgetLimitDao.getAllLimits()
            ) { expenses, limits ->
                Pair(expenses, limits)
            }.collectLatest { (expenses, limits) ->
                updateProgressBars(expenses, limits)
            }
        }
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
