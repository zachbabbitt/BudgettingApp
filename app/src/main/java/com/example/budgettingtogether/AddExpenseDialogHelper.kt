package com.example.budgettingtogether

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.budgettingtogether.databinding.DialogAddExpenseBinding
import kotlinx.coroutines.launch

class AddExpenseDialogHelper(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val categories: List<String>,
    private val currencyRepository: CurrencyRepository,
    private val defaultCurrencyExpenses: String,
    private val defaultCurrencyTracking: String,
    private val onExpenseSaved: suspend (Expense) -> Unit
) {
    private val currencyCodes = CurrencyData.currencies.keys.sorted()

    private val recurringOptions = listOf(
        context.getString(R.string.recurring_none),
        context.getString(R.string.recurring_weekly),
        context.getString(R.string.recurring_monthly)
    )

    fun show() {
        val dialogBinding = DialogAddExpenseBinding.inflate(LayoutInflater.from(context))

        setupCategorySpinner(dialogBinding)
        setupRecurringSpinner(dialogBinding)
        val currencyAdapter = setupCurrencySpinner(dialogBinding)

        var selectedCurrency = defaultCurrencyExpenses

        setupAmountTextWatcher(dialogBinding) { selectedCurrency }
        setupCurrencySelectionListener(dialogBinding, currencyAdapter) { currency ->
            selectedCurrency = currency
        }

        AlertDialog.Builder(context)
            .setTitle(R.string.add_expense)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.add) { _, _ ->
                handlePositiveClick(dialogBinding, selectedCurrency)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun setupCategorySpinner(dialogBinding: DialogAddExpenseBinding) {
        val categoryAdapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, categories)
        dialogBinding.spinnerCategory.setAdapter(categoryAdapter)
    }

    private fun setupRecurringSpinner(dialogBinding: DialogAddExpenseBinding) {
        val recurringAdapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, recurringOptions)
        dialogBinding.spinnerRecurring.setAdapter(recurringAdapter)
        dialogBinding.spinnerRecurring.setText(recurringOptions[0], false)
    }

    private fun setupCurrencySpinner(dialogBinding: DialogAddExpenseBinding): CurrencyDropdownAdapter {
        val recentCurrencies = currencyRepository.getRecentCurrencies()
        val currencyAdapter = CurrencyDropdownAdapter(context, recentCurrencies, currencyCodes)
        dialogBinding.spinnerCurrency.setAdapter(currencyAdapter)
        dialogBinding.spinnerCurrency.setText(defaultCurrencyExpenses, false)
        return currencyAdapter
    }

    private fun setupAmountTextWatcher(
        dialogBinding: DialogAddExpenseBinding,
        getSelectedCurrency: () -> String
    ) {
        val textWatcher = object : TextWatcher {
            private var runnable: Runnable? = null

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                runnable?.let { dialogBinding.editTextAmount.removeCallbacks(it) }
                runnable = Runnable {
                    updateConversionPreview(dialogBinding, getSelectedCurrency())
                }
                dialogBinding.editTextAmount.postDelayed(runnable, 500)
            }
        }
        dialogBinding.editTextAmount.addTextChangedListener(textWatcher)
    }

    private fun setupCurrencySelectionListener(
        dialogBinding: DialogAddExpenseBinding,
        currencyAdapter: CurrencyDropdownAdapter,
        onCurrencySelected: (String) -> Unit
    ) {
        dialogBinding.spinnerCurrency.setOnItemClickListener { _, _, position, _ ->
            currencyAdapter.getCurrencyCodeAtPosition(position)?.let { code ->
                onCurrencySelected(code)
                updateConversionPreview(dialogBinding, code)
            }
        }
    }

    private fun updateConversionPreview(dialogBinding: DialogAddExpenseBinding, selectedCurrency: String) {
        val amountStr = dialogBinding.editTextAmount.text.toString().trim()
        val amount = amountStr.toDoubleOrNull()

        if (amount == null || amount <= 0 || selectedCurrency == defaultCurrencyTracking) {
            dialogBinding.textViewConversionPreview.visibility = View.GONE
            return
        }

        lifecycleOwner.lifecycleScope.launch {
            val preview = currencyRepository.getConversionPreview(amount, selectedCurrency, defaultCurrencyTracking)
            dialogBinding.textViewConversionPreview.text = preview
            dialogBinding.textViewConversionPreview.visibility = View.VISIBLE
        }
    }

    private fun handlePositiveClick(dialogBinding: DialogAddExpenseBinding, selectedCurrency: String) {
        val title = dialogBinding.editTextTitle.text.toString().trim()
        val amountStr = dialogBinding.editTextAmount.text.toString().trim()
        val category = dialogBinding.spinnerCategory.text.toString().trim()
        val entryCurrency = dialogBinding.spinnerCurrency.text.toString().trim()

        if (title.isEmpty() || amountStr.isEmpty() || category.isEmpty()) {
            Toast.makeText(context, R.string.fill_all_fields, Toast.LENGTH_SHORT).show()
            return
        }

        val enteredAmount = amountStr.toDoubleOrNull()
        if (enteredAmount == null || enteredAmount <= 0) {
            Toast.makeText(context, R.string.invalid_amount, Toast.LENGTH_SHORT).show()
            return
        }

        val recurringSelection = dialogBinding.spinnerRecurring.text.toString()
        val recurringType = when (recurringSelection) {
            context.getString(R.string.recurring_weekly) -> RecurringType.WEEKLY
            context.getString(R.string.recurring_monthly) -> RecurringType.MONTHLY
            else -> RecurringType.NONE
        }

        lifecycleOwner.lifecycleScope.launch {
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
            onExpenseSaved(expense)
        }
    }
}
