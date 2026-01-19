package com.example.budgettingtogether

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.budgettingtogether.databinding.FragmentHomeBinding
import com.example.budgettingtogether.databinding.DialogAddExpenseBinding
import com.example.budgettingtogether.databinding.ItemBudgetProgressBinding
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var expenseDao: ExpenseDao
    private lateinit var incomeDao: IncomeDao
    private lateinit var budgetLimitDao: BudgetLimitDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var currencyRepository: CurrencyRepository
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

    private var categories: List<String> = emptyList()
    private var defaultCurrencyExpenses: String = "USD"
    private var defaultCurrencyTracking: String = "USD"
    private val currencyCodes = CurrencyData.currencies.keys.sorted()

    private val recurringOptions by lazy {
        listOf(
            getString(R.string.recurring_none),
            getString(R.string.recurring_weekly),
            getString(R.string.recurring_monthly)
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val database = AppDatabase.getDatabase(requireContext())
        expenseDao = database.expenseDao()
        incomeDao = database.incomeDao()
        budgetLimitDao = database.budgetLimitDao()
        categoryDao = database.categoryDao()
        currencyRepository = CurrencyRepository(requireContext())

        setupButtons()
        setupFab()
        observeData()
        observeCategories()
        loadCurrencySettings()
    }

    private fun setupButtons() {
        binding.buttonSetLimits.setOnClickListener {
            startActivity(Intent(requireContext(), BudgetLimitsActivity::class.java))
        }
    }

    private fun setupFab() {
        binding.fabAddExpense.setOnClickListener {
            showAddExpenseDialog()
        }
    }

    private fun observeCategories() {
        viewLifecycleOwner.lifecycleScope.launch {
            categoryDao.getAllCategoryNames().collectLatest { categoryList ->
                categories = categoryList
            }
        }
    }

    private fun loadCurrencySettings() {
        viewLifecycleOwner.lifecycleScope.launch {
            currencyRepository.observeDefaultCurrencyExpenses().collectLatest { currency ->
                defaultCurrencyExpenses = currency
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            currencyRepository.observeDefaultCurrencyTracking().collectLatest { currency ->
                defaultCurrencyTracking = currency
            }
        }
    }

    private fun showAddExpenseDialog() {
        val dialogBinding = DialogAddExpenseBinding.inflate(LayoutInflater.from(requireContext()))

        // Setup category spinner
        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        dialogBinding.spinnerCategory.setAdapter(categoryAdapter)

        // Setup recurring spinner
        val recurringAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, recurringOptions)
        dialogBinding.spinnerRecurring.setAdapter(recurringAdapter)
        dialogBinding.spinnerRecurring.setText(recurringOptions[0], false)

        // Setup currency spinner with recent currencies
        val recentCurrencies = currencyRepository.getRecentCurrencies()
        val currencyAdapter = CurrencyDropdownAdapter(requireContext(), recentCurrencies, currencyCodes)
        dialogBinding.spinnerCurrency.setAdapter(currencyAdapter)
        dialogBinding.spinnerCurrency.setText(defaultCurrencyExpenses, false)

        var selectedCurrency = defaultCurrencyExpenses

        // Update conversion preview when amount changes
        val textWatcher = object : TextWatcher {
            private var runnable: Runnable? = null

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                runnable?.let { dialogBinding.editTextAmount.removeCallbacks(it) }
                runnable = Runnable {
                    updateConversionPreview(dialogBinding, selectedCurrency)
                }
                dialogBinding.editTextAmount.postDelayed(runnable, 500)
            }
        }
        dialogBinding.editTextAmount.addTextChangedListener(textWatcher)

        // Update conversion preview when currency changes
        dialogBinding.spinnerCurrency.setOnItemClickListener { _, _, position, _ ->
            currencyAdapter.getCurrencyCodeAtPosition(position)?.let { code ->
                selectedCurrency = code
                updateConversionPreview(dialogBinding, selectedCurrency)
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.add_expense)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.add) { _, _ ->
                val title = dialogBinding.editTextTitle.text.toString().trim()
                val amountStr = dialogBinding.editTextAmount.text.toString().trim()
                val category = dialogBinding.spinnerCategory.text.toString().trim()
                val entryCurrency = dialogBinding.spinnerCurrency.text.toString().trim()

                if (title.isEmpty() || amountStr.isEmpty() || category.isEmpty()) {
                    Toast.makeText(requireContext(), R.string.fill_all_fields, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val enteredAmount = amountStr.toDoubleOrNull()
                if (enteredAmount == null || enteredAmount <= 0) {
                    Toast.makeText(requireContext(), R.string.invalid_amount, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val recurringSelection = dialogBinding.spinnerRecurring.text.toString()
                val recurringType = when (recurringSelection) {
                    getString(R.string.recurring_weekly) -> RecurringType.WEEKLY
                    getString(R.string.recurring_monthly) -> RecurringType.MONTHLY
                    else -> RecurringType.NONE
                }

                // Convert and save
                viewLifecycleOwner.lifecycleScope.launch {
                    val convertedAmount = currencyRepository.convert(enteredAmount, entryCurrency, defaultCurrencyTracking)

                    val expense = if (entryCurrency != defaultCurrencyTracking) {
                        Expense(
                            title = title,
                            amount = convertedAmount,
                            category = category,
                            recurringType = recurringType,
                            originalAmount = enteredAmount,
                            originalCurrency = entryCurrency
                        )
                    } else {
                        Expense(
                            title = title,
                            amount = enteredAmount,
                            category = category,
                            recurringType = recurringType
                        )
                    }
                    currencyRepository.addRecentCurrency(entryCurrency)
                    expenseDao.insert(expense)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun updateConversionPreview(dialogBinding: DialogAddExpenseBinding, selectedCurrency: String) {
        val amountStr = dialogBinding.editTextAmount.text.toString().trim()
        val amount = amountStr.toDoubleOrNull()

        if (amount == null || amount <= 0 || selectedCurrency == defaultCurrencyTracking) {
            dialogBinding.textViewConversionPreview.visibility = View.GONE
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val preview = currencyRepository.getConversionPreview(amount, selectedCurrency, defaultCurrencyTracking)
            dialogBinding.textViewConversionPreview.text = preview
            dialogBinding.textViewConversionPreview.visibility = View.VISIBLE
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
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
            LayoutInflater.from(requireContext()),
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
        itemBinding.progressBar.progressDrawable = requireContext().getDrawable(progressDrawable)

        // Show percentage text
        itemBinding.textViewPercentage.text = if (actualPercentage > 100) {
            getString(R.string.over_budget)
        } else {
            "$actualPercentage%"
        }

        // Color the percentage text if over budget
        if (actualPercentage >= 100) {
            itemBinding.textViewPercentage.setTextColor(requireContext().getColor(android.R.color.holo_red_dark))
            itemBinding.textViewPercentage.alpha = 1f
        }

        binding.linearLayoutProgress.addView(itemBinding.root)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
