package com.example.budgettingtogether

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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.budgettingtogether.databinding.FragmentExpensesBinding
import com.example.budgettingtogether.databinding.DialogAddExpenseBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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
        _binding = FragmentExpensesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val database = AppDatabase.getDatabase(requireContext())
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
                expenseAdapter.updateList(expenses)
                updateTotalDisplay(expenses)
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

        // Setup currency spinner
        val currencyAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, currencyCodes)
        dialogBinding.spinnerCurrency.setAdapter(currencyAdapter)
        dialogBinding.spinnerCurrency.setText(defaultCurrencyExpenses, false)


        var selectedCurrency = defaultCurrencyExpenses

        // Update conversion preview when amount changes (after typing stops)
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
            selectedCurrency = currencyCodes[position]
            updateConversionPreview(dialogBinding, selectedCurrency)
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
                    addExpense(expense)
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
