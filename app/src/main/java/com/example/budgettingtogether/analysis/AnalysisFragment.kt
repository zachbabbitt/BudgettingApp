package com.example.budgettingtogether.analysis

import android.R
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.budgettingtogether.core.AppDatabase
import com.example.budgettingtogether.expenses.Expense
import com.example.budgettingtogether.expenses.ExpenseAdapter
import com.example.budgettingtogether.util.RecurringType
import com.example.budgettingtogether.auth.PairingRepository
import com.example.budgettingtogether.auth.SessionManager
import com.example.budgettingtogether.storage.AppDataSource
import com.example.budgettingtogether.storage.StoragePreferenceManager
import com.example.budgettingtogether.storage.source.ICategorySource
import com.example.budgettingtogether.storage.source.IExpenseSource
import com.example.budgettingtogether.databinding.FragmentAnalysisBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class AnalysisFragment : Fragment() {

    private var _binding: FragmentAnalysisBinding? = null
    private val binding get() = _binding!!

    private lateinit var appDataSource: AppDataSource
    private val expenseSource: IExpenseSource get() = appDataSource.expenseSource
    private val categorySource: ICategorySource get() = appDataSource.categorySource
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnalysisBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val database = AppDatabase.Companion.getDatabase(requireContext())
        sessionManager = SessionManager(requireContext())
        pairingRepository = PairingRepository(database.userDao(), database.userPairingDao())
        appDataSource = AppDataSource(database, StoragePreferenceManager(requireContext()))

        setupRecyclerViews()
        loadPairedGuidsAndObserve()
    }

    private fun setupRecyclerViews() {
        recurringAdapter = ExpenseAdapter { /* No delete in analysis view */ }
        binding.recyclerViewRecurring.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recurringAdapter
        }

        categoryExpenseAdapter = ExpenseAdapter { /* No delete in analysis view */ }
        binding.recyclerViewExpenses.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = categoryExpenseAdapter
        }
    }

    private fun setupCategoryFilter(categoryList: List<String>) {
        categories = listOf("All Categories") + categoryList
        val adapter = ArrayAdapter(requireContext(), R.layout.simple_dropdown_item_1line, categories)
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
        viewLifecycleOwner.lifecycleScope.launch {
            val userId = sessionManager.getUserId() ?: return@launch
            pairedGuids = pairingRepository.getPairedUserGuids(userId, userGuid)
            observeData()
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            categorySource.getAllCategoryNames(pairedGuids).collectLatest { categoryList ->
                setupCategoryFilter(categoryList)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            expenseSource.getRecurringExpenses(pairedGuids).collectLatest { recurringExpenses ->
                updateRecurringSection(recurringExpenses)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
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
            binding.textViewRecurringTotal.text = getString(com.example.budgettingtogether.R.string.monthly_total, currencyFormat.format(0.0))
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
            binding.textViewRecurringTotal.text = getString(com.example.budgettingtogether.R.string.monthly_total, currencyFormat.format(monthlyTotal))
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
            binding.textViewFilteredTotal.text = getString(com.example.budgettingtogether.R.string.total_label, currencyFormat.format(0.0))
        } else {
            binding.recyclerViewExpenses.visibility = View.VISIBLE
            binding.textViewNoExpenses.visibility = View.GONE
            categoryExpenseAdapter.updateList(filteredExpenses)

            val total = filteredExpenses.sumOf { it.amount }
            binding.textViewFilteredTotal.text = getString(com.example.budgettingtogether.R.string.total_label, currencyFormat.format(total))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
