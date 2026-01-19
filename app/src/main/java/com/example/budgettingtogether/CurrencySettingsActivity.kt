package com.example.budgettingtogether

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.budgettingtogether.databinding.ActivityCurrencySettingsBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CurrencySettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCurrencySettingsBinding
    private lateinit var currencyRepository: CurrencyRepository

    private val currencyCodes = CurrencyData.currencies.keys.sorted()
    private val currencyDisplayNames = currencyCodes.map { CurrencyData.getDisplayName(it) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCurrencySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currencyRepository = CurrencyRepository(this)

        setupToolbar()
        setupDefaultCurrencySpinner()
        setupRefreshButton()
        loadData()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.currency_settings)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupDefaultCurrencySpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, currencyDisplayNames)
        val adapterExpenseCurrency = ArrayAdapter(this,android.R.layout.simple_dropdown_item_1line, currencyDisplayNames )
        binding.spinnerDefaultCurrency.setAdapter(adapter)
        binding.spinnerDefaultExpenseCurrency.setAdapter(adapterExpenseCurrency);

        binding.spinnerDefaultCurrency.setOnItemClickListener { _, _, position, _ ->
            val selectedCode = currencyCodes[position]
            lifecycleScope.launch {
                currencyRepository.setDefaultCurrencyTracking(selectedCode)
                Toast.makeText(
                    this@CurrencySettingsActivity,
                    getString(R.string.default_currency_set, selectedCode),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }


        binding.spinnerDefaultExpenseCurrency.setOnItemClickListener { _, _, position, _ ->
            val selectedCode = currencyCodes[position]
            lifecycleScope.launch {
                currencyRepository.setDefaultCurrencyExpenses(selectedCode)
                Toast.makeText(
                    this@CurrencySettingsActivity,
                    getString(R.string.default_currency_set, selectedCode),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setupRefreshButton() {
        binding.buttonRefresh.setOnClickListener {
            refreshRates()
        }
    }

    private fun loadData() {
        lifecycleScope.launch {
            // Load default currency
            val defaultCurrencyTracking = currencyRepository.getDefaultCurrencyTracking()
            val defaultCurrencyExpenses = currencyRepository.getDefaultCurrencyExpenses()

            val indexTracking = currencyCodes.indexOf(defaultCurrencyTracking)
            val indexExpenses = currencyCodes.indexOf(defaultCurrencyExpenses)

            if (indexExpenses >= 0) {
                binding.spinnerDefaultCurrency.setText(currencyDisplayNames[indexTracking], false)
            }

            if (indexExpenses >= 0) {
                binding.spinnerDefaultExpenseCurrency.setText(currencyDisplayNames[indexExpenses], false)
            }

            // Load last update time and rates status
            updateStatusDisplay()

            // Auto-fetch rates on first launch if none available
            val rates = currencyRepository.getAllRates()
            if (rates.isEmpty()) {
                refreshRates()
            }
        }
    }

    private fun refreshRates() {
        binding.progressBar.visibility = View.VISIBLE
        binding.buttonRefresh.isEnabled = false

        lifecycleScope.launch {
            val result = currencyRepository.fetchAndSaveRates()

            binding.progressBar.visibility = View.GONE
            binding.buttonRefresh.isEnabled = true

            result.fold(
                onSuccess = {
                    updateStatusDisplay()
                    Toast.makeText(
                        this@CurrencySettingsActivity,
                        R.string.rates_updated,
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onFailure = { e ->
                    Toast.makeText(
                        this@CurrencySettingsActivity,
                        getString(R.string.rates_update_failed, e.message),
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }

    private fun updateStatusDisplay() {
        lifecycleScope.launch {
            val rates = currencyRepository.getAllRates()
            binding.textViewRatesStatus.text = getString(R.string.currencies_available, rates.size)

            val lastUpdate = currencyRepository.getLastUpdateTime()
            binding.textViewLastUpdated.text = if (lastUpdate == 0L) {
                getString(R.string.last_updated_never)
            } else {
                val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                getString(R.string.last_updated, dateFormat.format(Date(lastUpdate)))
            }
        }
    }
}
