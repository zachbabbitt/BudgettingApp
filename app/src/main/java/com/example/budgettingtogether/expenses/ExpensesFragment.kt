package com.example.budgettingtogether.expenses

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.budgettingtogether.core.AppDatabase
import com.example.budgettingtogether.categories.CategoryDao
import com.example.budgettingtogether.currency.CurrencyRepository
import com.example.budgettingtogether.R
import com.example.budgettingtogether.databinding.FragmentExpensesBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar

class ExpensesFragment : Fragment() {

    private var _binding: FragmentExpensesBinding? = null
    private val binding get() = _binding!!

    private lateinit var expenseAdapter: ExpenseAdapter
    private lateinit var expenseDao: ExpenseDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var currencyRepository: CurrencyRepository

    private var categories: List<String> = emptyList()
    private var defaultCurrencyExpenses: String = "USD"
    private var defaultCurrencyTracking: String = "USD"
    private var allExpenses: List<Expense> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExpensesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val database = AppDatabase.Companion.getDatabase(requireContext())
        expenseDao = database.expenseDao()
        categoryDao = database.categoryDao()
        currencyRepository = CurrencyRepository(requireContext())

        setupRecyclerView()
        setupFab()
        loadChangeableSettings()
        observeCategories()
        observeExpenses()
    }

    private fun loadChangeableSettings() {
        viewLifecycleOwner.lifecycleScope.launch {
            loadDefaultCurrencyExpenses()
            loadDefaultCurrencyTracking()
        }
    }

    private suspend fun loadDefaultCurrencyExpenses() {

        currencyRepository.observeDefaultCurrencyExpenses().collectLatest { currency ->
            defaultCurrencyExpenses = currency
        }
    }

    private suspend fun loadDefaultCurrencyTracking() {

        currencyRepository.observeDefaultCurrencyTracking().collectLatest { currency ->
            defaultCurrencyTracking = currency
        }
    }

    private fun observeCategories() {
        viewLifecycleOwner.lifecycleScope.launch {
            categoryDao.getAllCategoryNames().collectLatest { categoryList ->
                categories = categoryList
            }
        }
    }

    private fun setupRecyclerView() {
        expenseAdapter = ExpenseAdapter { expense ->
            deleteExpense(expense)
        }
        binding.recyclerViewExpenses.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = expenseAdapter
        }
    }

    private fun setupFab() {
        binding.fabAddExpense.setOnClickListener {
            showAddExpenseDialog()
        }
    }

    private fun observeExpenses() {
        viewLifecycleOwner.lifecycleScope.launch {
            expenseDao.getAllExpenses().collectLatest { expenses ->
                allExpenses = expenses
                updateDisplayedExpenses()
            }
        }
    }

    private fun updateDisplayedExpenses() {
        val monthlyExpenses = filterByCurrentMonth(allExpenses)
        expenseAdapter.updateList(monthlyExpenses)
        updateTotalDisplay(monthlyExpenses)
    }

    private fun filterByCurrentMonth(expenses: List<Expense>): List<Expense> {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        return expenses.filter { expense ->
            val expenseCalendar = Calendar.getInstance().apply { time = expense.date }
            expenseCalendar.get(Calendar.MONTH) == currentMonth &&
                expenseCalendar.get(Calendar.YEAR) == currentYear
        }
    }

    override fun onResume() {
        super.onResume()
        if (allExpenses.isNotEmpty()) {
            updateDisplayedExpenses()
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

    private fun deleteExpense(expense: Expense) {
        viewLifecycleOwner.lifecycleScope.launch {
            expenseDao.delete(expense)
        }
    }

    private fun updateTotalDisplay(expenses: List<Expense>) {
        val total = expenses.sumOf { it.amount }
        binding.textViewTotal.text = getString(R.string.total_format, total)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
