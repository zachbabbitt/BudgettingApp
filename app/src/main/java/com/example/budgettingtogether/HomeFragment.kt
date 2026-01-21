package com.example.budgettingtogether

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.budgettingtogether.categories.CategoryDao
import com.example.budgettingtogether.core.AppDatabase
import com.example.budgettingtogether.currency.CurrencyData
import com.example.budgettingtogether.currency.CurrencyRepository
import com.example.budgettingtogether.databinding.FragmentHomeBinding
import com.example.budgettingtogether.databinding.ItemBudgetProgressBinding
import com.example.budgettingtogether.expenses.AddExpenseDialogHelper
import com.example.budgettingtogether.expenses.Expense
import com.example.budgettingtogether.expenses.ExpenseDao
import com.example.budgettingtogether.income.Income
import com.example.budgettingtogether.income.IncomeDao
import com.example.budgettingtogether.limits.BudgetLimit
import com.example.budgettingtogether.limits.BudgetLimitDao
import com.example.budgettingtogether.limits.BudgetLimitsActivity
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var expenseDao: ExpenseDao
    private lateinit var incomeDao: IncomeDao
    private lateinit var budgetLimitDao: BudgetLimitDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var currencyRepository: CurrencyRepository
    private var currencySymbol: String = "$"

    private var categories: List<String> = emptyList()
    private var defaultCurrencyExpenses: String = "USD"
    private var defaultCurrencyTracking: String = "USD"

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
    }

    private fun showAddExpenseDialog() {
        AddExpenseDialogHelper(
            context = requireContext(),
            lifecycleOwner = viewLifecycleOwner,
            categories = categories,
            currencyRepository = currencyRepository,
            defaultCurrencyExpenses = defaultCurrencyExpenses,
            defaultCurrencyTracking = defaultCurrencyTracking
        ) { expense ->
            expenseDao.insert(expense)
        }.show()
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            combine(
                expenseDao.getAllExpenses(),
                budgetLimitDao.getAllLimits(),
                incomeDao.getAllIncome(),
                currencyRepository.observeDefaultCurrencyTracking()
            ) { expenses, limits, income, trackingCurrency ->
                defaultCurrencyTracking = trackingCurrency
                currencySymbol = CurrencyData.getSymbol(trackingCurrency)

                // Convert expenses to new tracking currency
                val convertedExpenses = expenses.map { expense ->
                    val originalAmount = expense.originalAmount ?: expense.amount
                    val originalCurrency = expense.originalCurrency ?: "USD"
                    val convertedAmount = currencyRepository.convert(originalAmount, originalCurrency, trackingCurrency)
                    expense.copy(amount = convertedAmount)
                }

                Triple(convertedExpenses, limits, income)
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
                formatCurrency(totalLimits),
                formatCurrency(totalMonthlyIncome)
            )
        }

        // Warning 2: Spending exceeds income
        val showWarning2 = totalMonthlyExpenses > totalMonthlyIncome && totalMonthlyIncome > 0
        binding.textViewWarning2.visibility = if (showWarning2) View.VISIBLE else View.GONE
        if (showWarning2) {
            binding.textViewWarning2.text = getString(
                R.string.warning_spending_exceeds_income,
                formatCurrency(totalMonthlyExpenses),
                formatCurrency(totalMonthlyIncome)
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
            formatCurrency(spent),
            formatCurrency(limit)
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

    private fun formatCurrency(amount: Double): String {
        return "$currencySymbol${String.format("%.2f", amount)}"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
