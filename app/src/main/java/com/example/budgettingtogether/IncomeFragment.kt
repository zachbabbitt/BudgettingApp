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
import com.example.budgettingtogether.databinding.FragmentIncomeBinding
import com.example.budgettingtogether.databinding.DialogAddIncomeBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class IncomeFragment : Fragment() {

    private var _binding: FragmentIncomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var incomeAdapter: IncomeAdapter
    private lateinit var incomeDao: IncomeDao
    private lateinit var currencyRepository: CurrencyRepository

    private var defaultCurrency: String = "USD"
    private val currencyCodes = CurrencyData.currencies.keys.sorted()

    private val sources = listOf(
        "Salary",
        "Freelance",
        "Investments",
        "Business",
        "Rental",
        "Gifts",
        "Other"
    )

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
        _binding = FragmentIncomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val database = AppDatabase.getDatabase(requireContext())
        incomeDao = database.incomeDao()
        currencyRepository = CurrencyRepository(requireContext())

        setupRecyclerView()
        setupFab()
        observeIncome()
        loadChangeableSettings()
    }

    private fun loadChangeableSettings() {
        viewLifecycleOwner.lifecycleScope.launch {
            loadDefaultCurrency()
        }
    }

    private suspend fun loadDefaultCurrency() {

        currencyRepository.observeDefaultCurrencyIncome().collectLatest { currency ->
            defaultCurrency = currency
        }
    }

    private fun setupRecyclerView() {
        incomeAdapter = IncomeAdapter { income ->
            deleteIncome(income)
        }
        binding.recyclerViewIncome.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = incomeAdapter
        }
    }

    private fun setupFab() {
        binding.fabAddIncome.setOnClickListener {
            showAddIncomeDialog()
        }
    }

    private fun observeIncome() {
        viewLifecycleOwner.lifecycleScope.launch {
            incomeDao.getAllIncome().collectLatest { incomeList ->
                incomeAdapter.updateList(incomeList)
                updateTotalDisplay(incomeList)
            }
        }
    }

    private fun showAddIncomeDialog() {
        val dialogBinding = DialogAddIncomeBinding.inflate(LayoutInflater.from(requireContext()))

        // Setup source spinner
        val sourceAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, sources)
        dialogBinding.spinnerSource.setAdapter(sourceAdapter)

        // Setup recurring spinner
        val recurringAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, recurringOptions)
        dialogBinding.spinnerRecurring.setAdapter(recurringAdapter)
        dialogBinding.spinnerRecurring.setText(recurringOptions[0], false)

        // Setup currency spinner with recent currencies
        val recentCurrencies = currencyRepository.getRecentCurrencies()
        val currencyAdapter = CurrencyDropdownAdapter(requireContext(), recentCurrencies, currencyCodes)
        dialogBinding.spinnerCurrency.setAdapter(currencyAdapter)
        dialogBinding.spinnerCurrency.setText(defaultCurrency, false)

        var selectedCurrency = defaultCurrency

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
            currencyAdapter.getCurrencyCodeAtPosition(position)?.let { code ->
                selectedCurrency = code
                updateConversionPreview(dialogBinding, selectedCurrency)
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.add_income)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.add) { _, _ ->
                val title = dialogBinding.editTextTitle.text.toString().trim()
                val amountStr = dialogBinding.editTextAmount.text.toString().trim()
                val source = dialogBinding.spinnerSource.text.toString().trim()
                val entryCurrency = dialogBinding.spinnerCurrency.text.toString().trim()

                if (title.isEmpty() || amountStr.isEmpty() || source.isEmpty()) {
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
                    val convertedAmount = currencyRepository.convert(enteredAmount, entryCurrency, defaultCurrency)

                    val income = if (entryCurrency != defaultCurrency) {
                        Income(
                            title = title,
                            amount = convertedAmount,
                            source = source,
                            recurringType = recurringType,
                            originalAmount = enteredAmount,
                            originalCurrency = entryCurrency
                        )
                    } else {
                        Income(
                            title = title,
                            amount = enteredAmount,
                            source = source,
                            recurringType = recurringType
                        )
                    }
                    currencyRepository.addRecentCurrency(entryCurrency)
                    addIncome(income)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun updateConversionPreview(dialogBinding: DialogAddIncomeBinding, selectedCurrency: String) {
        val amountStr = dialogBinding.editTextAmount.text.toString().trim()
        val amount = amountStr.toDoubleOrNull()

        if (amount == null || amount <= 0 || selectedCurrency == defaultCurrency) {
            dialogBinding.textViewConversionPreview.visibility = View.GONE
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val preview = currencyRepository.getConversionPreview(amount, selectedCurrency, defaultCurrency)
            dialogBinding.textViewConversionPreview.text = preview
            dialogBinding.textViewConversionPreview.visibility = View.VISIBLE
        }
    }

    private fun addIncome(income: Income) {
        viewLifecycleOwner.lifecycleScope.launch {
            incomeDao.insert(income)
        }
    }

    private fun deleteIncome(income: Income) {
        viewLifecycleOwner.lifecycleScope.launch {
            incomeDao.delete(income)
        }
    }

    private fun updateTotalDisplay(incomeList: List<Income>) {
        val total = incomeList.sumOf { it.amount }
        binding.textViewTotal.text = getString(R.string.total_format, total)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
