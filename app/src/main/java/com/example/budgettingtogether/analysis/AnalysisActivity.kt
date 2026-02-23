package com.example.budgettingtogether.analysis

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.budgettingtogether.core.AppDatabase
import com.example.budgettingtogether.expenses.Expense
import com.example.budgettingtogether.expenses.ExpenseAdapter
import com.example.budgettingtogether.R
import com.example.budgettingtogether.auth.PairingRepository
import com.example.budgettingtogether.auth.SessionManager
import com.example.budgettingtogether.storage.AppDataSource
import com.example.budgettingtogether.storage.StoragePreferenceManager
import com.example.budgettingtogether.storage.source.ICategorySource
import com.example.budgettingtogether.storage.source.IExpenseSource
import com.example.budgettingtogether.util.RecurringType
import com.example.budgettingtogether.databinding.ActivityAnalysisBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class AnalysisActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAnalysisBinding
    private lateinit var expenseSource: IExpenseSource
    private lateinit var categorySource: ICategorySource
    private lateinit var sessionManager: SessionManager
    private lateinit var pairingRepository: PairingRepository
    private val userGuid: String get() = sessionManager.getUserGuid() ?: ""
    private var pairedGuids: List<String> = emptyList()
    private lateinit var recurringAdapter: ExpenseAdapter
    private lateinit var categoryExpenseAdapter: ExpenseAdapter
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

    private var categories: List<String> = listOf("All Categories")
    private var allExpenses: List<Expense> = emptyList()
    private var selectedCategory: String = "All Categories"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnalysisBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val database = AppDatabase.Companion.getDatabase(this)
        sessionManager = SessionManager(this)
        pairingRepository = PairingRepository(database.userDao(), database.userPairingDao())
        val appDataSource = AppDataSource(database, StoragePreferenceManager(this))
        expenseSource = appDataSource.expenseSource
        categorySource = appDataSource.categorySource

        setupToolbar()
        setupRecyclerViews()
        loadPairedGuidsAndObserve()
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

        categoryExpenseAdapter = ExpenseAdapter { /* No delete in analysis view */ }
        binding.recyclerViewExpenses.apply {
            layoutManager = LinearLayoutManager(this@AnalysisActivity)
            adapter = categoryExpenseAdapter
        }
    }

    private fun setupCategoryFilter(categoryList: List<String>) {
        categories = listOf("All Categories") + categoryList
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        binding.spinnerCategoryFilter.setAdapter(adapter)

        if (binding.spinnerCategoryFilter.text.isNullOrEmpty()) {
            binding.spinnerCategoryFilter.setText(categories[0], false)
        }

        binding.spinnerCategoryFilter.setOnItemClickListener { _, _, position, _ ->
            selectedCategory = categories[position]
            updateCategoryExpenses()
        }
    }

    private fun loadPairedGuidsAndObserve() {
        lifecycleScope.launch {
            val userId = sessionManager.getUserId() ?: return@launch
            pairedGuids = pairingRepository.getPairedUserGuids(userId, userGuid)
            observeData()
        }
    }

    private fun observeData() {
        lifecycleScope.launch {
            categorySource.getAllCategoryNames(pairedGuids).collectLatest { categoryList ->
                setupCategoryFilter(categoryList)
            }
        }

        lifecycleScope.launch {
            expenseSource.getRecurringExpenses(pairedGuids).collectLatest { recurringExpenses ->
                updateRecurringSection(recurringExpenses)
            }
        }

        lifecycleScope.launch {
            expenseSource.getAllExpenses(pairedGuids).collectLatest { expenses ->
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
            categoryExpenseAdapter.updateList(filteredExpenses)

            val total = filteredExpenses.sumOf { it.amount }
            binding.textViewFilteredTotal.text = getString(R.string.total_label, currencyFormat.format(total))
        }
    }
}
