package com.example.budgettingtogether

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.budgettingtogether.databinding.FragmentExpensesBinding
import com.example.budgettingtogether.databinding.DialogAddExpenseBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ExpensesFragment : Fragment() {

    private var _binding: FragmentExpensesBinding? = null
    private val binding get() = _binding!!

    private lateinit var expenseAdapter: ExpenseAdapter
    private lateinit var expenseDao: ExpenseDao
    private lateinit var categoryDao: CategoryDao

    private var categories: List<String> = emptyList()

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
        _binding = FragmentExpensesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val database = AppDatabase.getDatabase(requireContext())
        expenseDao = database.expenseDao()
        categoryDao = database.categoryDao()

        setupRecyclerView()
        setupFab()
        observeExpenses()
        observeCategories()
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
                expenseAdapter.updateList(expenses)
                updateTotalDisplay(expenses)
            }
        }
    }

    private fun showAddExpenseDialog() {
        val dialogBinding = DialogAddExpenseBinding.inflate(LayoutInflater.from(requireContext()))

        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        dialogBinding.spinnerCategory.setAdapter(categoryAdapter)

        val recurringAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, recurringOptions)
        dialogBinding.spinnerRecurring.setAdapter(recurringAdapter)
        dialogBinding.spinnerRecurring.setText(recurringOptions[0], false)

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.add_expense)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.add) { _, _ ->
                val title = dialogBinding.editTextTitle.text.toString().trim()
                val amountStr = dialogBinding.editTextAmount.text.toString().trim()
                val category = dialogBinding.spinnerCategory.text.toString().trim()

                if (title.isEmpty() || amountStr.isEmpty() || category.isEmpty()) {
                    Toast.makeText(requireContext(), R.string.fill_all_fields, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val amount = amountStr.toDoubleOrNull()
                if (amount == null || amount <= 0) {
                    Toast.makeText(requireContext(), R.string.invalid_amount, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val recurringSelection = dialogBinding.spinnerRecurring.text.toString()
                val recurringType = when (recurringSelection) {
                    getString(R.string.recurring_weekly) -> RecurringType.WEEKLY
                    getString(R.string.recurring_monthly) -> RecurringType.MONTHLY
                    else -> RecurringType.NONE
                }

                addExpense(Expense(title = title, amount = amount, category = category, recurringType = recurringType))
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun addExpense(expense: Expense) {
        viewLifecycleOwner.lifecycleScope.launch {
            expenseDao.insert(expense)
        }
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
