package com.example.budgettingtogether

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.budgettingtogether.databinding.ActivityAnalysisBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class AnalysisActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAnalysisBinding
    private lateinit var expenseDao: ExpenseDao
    private lateinit var recurringAdapter: ExpenseAdapter
    private lateinit var categoryAdapter: ExpenseAdapter
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

    private val categories = listOf(
        "All Categories",
        "Food & Dining",
        "Transportation",
        "Shopping",
        "Entertainment",
        "Bills & Utilities",
        "Health",
        "Other"
    )

    private var allExpenses: List<Expense> = emptyList()
    private var selectedCategory: String = "All Categories"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnalysisBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val database = AppDatabase.getDatabase(this)
        expenseDao = database.expenseDao()

        setupToolbar()
        setupRecyclerViews()
        setupCategoryFilter()
        observeData()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.analysis_title)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupRecyclerViews() {
        recurringAdapter = ExpenseAdapter { /* No delete in analysis view */ }
        binding.recyclerViewRecurring.apply {
            layoutManager = LinearLayoutManager(this@AnalysisActivity)
            adapter = recurringAdapter
        }

        categoryAdapter = ExpenseAdapter { /* No delete in analysis view */ }
        binding.recyclerViewExpenses.apply {
            layoutManager = LinearLayoutManager(this@AnalysisActivity)
            adapter = categoryAdapter
        }
    }

    private fun setupCategoryFilter() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        binding.spinnerCategoryFilter.setAdapter(adapter)
        binding.spinnerCategoryFilter.setText(categories[0], false)

        binding.spinnerCategoryFilter.setOnItemClickListener { _, _, position, _ ->
            selectedCategory = categories[position]
            updateCategoryExpenses()
        }
    }

    private fun observeData() {
        lifecycleScope.launch {
            expenseDao.getRecurringExpenses().collectLatest { recurringExpenses ->
                updateRecurringSection(recurringExpenses)
            }
        }

        lifecycleScope.launch {
            expenseDao.getAllExpenses().collectLatest { expenses ->
                allExpenses = expenses
                updateCategoryExpenses()
            }
        }
    }

    private fun updateRecurringSection(expenses: List<Expense>) {
        if (expenses.isEmpty()) {
            binding.recyclerViewRecurring.visibility = View.GONE
            binding.textViewNoRecurring.visibility = View.VISIBLE
            binding.textViewRecurringTotal.text = getString(R.string.monthly_total, currencyFormat.format(0.0))
        } else {
            binding.recyclerViewRecurring.visibility = View.VISIBLE
            binding.textViewNoRecurring.visibility = View.GONE
            recurringAdapter.updateList(expenses)

            val monthlyTotal = expenses.sumOf { expense ->
                when (expense.recurringType) {
                    RecurringType.WEEKLY -> expense.amount * 4
                    RecurringType.MONTHLY -> expense.amount
                    else -> 0.0
                }
            }
            binding.textViewRecurringTotal.text = getString(R.string.monthly_total, currencyFormat.format(monthlyTotal))
        }
    }

    private fun updateCategoryExpenses() {
        val filteredExpenses = if (selectedCategory == "All Categories") {
            allExpenses
        } else {
            allExpenses.filter { it.category == selectedCategory }
        }

        if (filteredExpenses.isEmpty()) {
            binding.recyclerViewExpenses.visibility = View.GONE
            binding.textViewNoExpenses.visibility = View.VISIBLE
            binding.textViewFilteredTotal.text = getString(R.string.total_label, currencyFormat.format(0.0))
        } else {
            binding.recyclerViewExpenses.visibility = View.VISIBLE
            binding.textViewNoExpenses.visibility = View.GONE
            categoryAdapter.updateList(filteredExpenses)

            val total = filteredExpenses.sumOf { it.amount }
            binding.textViewFilteredTotal.text = getString(R.string.total_label, currencyFormat.format(total))
        }
    }
}
