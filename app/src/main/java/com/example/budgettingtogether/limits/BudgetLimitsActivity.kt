package com.example.budgettingtogether.limits

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.budgettingtogether.categories.CategoryDao
import com.example.budgettingtogether.R
import com.example.budgettingtogether.core.AppDatabase
import com.example.budgettingtogether.currency.CurrencyData
import com.example.budgettingtogether.currency.CurrencyRepository
import com.example.budgettingtogether.databinding.ActivityBudgetLimitsBinding
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class BudgetLimitsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBudgetLimitsBinding
    private lateinit var budgetLimitDao: BudgetLimitDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var currencyRepository: CurrencyRepository
    private var adapter: BudgetLimitAdapter? = null
    private var currentCurrencyCode: String = "USD"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBudgetLimitsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val database = AppDatabase.getDatabase(this)
        budgetLimitDao = database.budgetLimitDao()
        categoryDao = database.categoryDao()
        currencyRepository = CurrencyRepository(this)

        setupToolbar()
        setupRecyclerView()
        observeData()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.budget_limits_title)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupRecyclerView() {
        binding.recyclerViewLimits.layoutManager = LinearLayoutManager(this)
    }

    private fun observeData() {
        lifecycleScope.launch {
            combine(
                categoryDao.getAllCategoryNames(),
                budgetLimitDao.getAllLimits(),
                currencyRepository.observeDefaultCurrencyTracking()
            ) { categories, limits, trackingCurrency ->
                Triple(categories, limits, trackingCurrency)
            }.collectLatest { (categories, limits, trackingCurrency) ->
                currentCurrencyCode = trackingCurrency
                val currencySymbol = CurrencyData.getSymbol(trackingCurrency)

                if (adapter == null) {
                    adapter = BudgetLimitAdapter(categories, currencySymbol) { category, limit ->
                        saveLimitDebounced(category, limit)
                    }
                    binding.recyclerViewLimits.adapter = adapter
                } else {
                    adapter?.updateCategories(categories)
                    adapter?.updateCurrency(currencySymbol)
                }
                adapter?.setLimits(limits)
            }
        }
    }

    private fun saveLimitDebounced(category: String, limit: Double?) {
        lifecycleScope.launch {
            if (limit != null && limit > 0) {
                budgetLimitDao.insertOrUpdate(BudgetLimit(category, limit, currentCurrencyCode))
            } else {
                budgetLimitDao.delete(category)
            }
        }
    }
}
