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
import com.example.budgettingtogether.auth.PairingRepository
import com.example.budgettingtogether.auth.SessionManager
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class BudgetLimitsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBudgetLimitsBinding
    private lateinit var budgetLimitDao: BudgetLimitDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var currencyRepository: CurrencyRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var pairingRepository: PairingRepository
    private val userGuid: String get() = sessionManager.getUserGuid() ?: ""
    private var pairedGuids: List<String> = emptyList()
    private var adapter: BudgetLimitAdapter? = null
    private var currentCurrencyCode: String = "USD"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBudgetLimitsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val database = AppDatabase.getDatabase(this)
        budgetLimitDao = database.budgetLimitDao()
        categoryDao = database.categoryDao()
        sessionManager = SessionManager(this)
        pairingRepository = PairingRepository(database.userDao(), database.userPairingDao())
        currencyRepository = CurrencyRepository(this, userGuid)

        setupToolbar()
        setupRecyclerView()
        loadPairedGuidsAndObserve()
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

    private fun loadPairedGuidsAndObserve() {
        lifecycleScope.launch {
            val userId = sessionManager.getUserId() ?: return@launch
            pairedGuids = pairingRepository.getPairedUserGuids(userId, userGuid)
            observeData()
        }
    }

    private fun observeData() {
        lifecycleScope.launch {
            combine(
                categoryDao.getAllCategoryNames(pairedGuids),
                budgetLimitDao.getAllLimits(pairedGuids),
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

                val totalLimits = limits.sumOf { it.limitAmount }
                binding.textViewTotalLimits.text = String.format("%s%.2f", currencySymbol, totalLimits)
            }
        }
    }

    private fun saveLimitDebounced(category: String, limit: Double?) {
        lifecycleScope.launch {
            if (limit != null && limit > 0) {
                budgetLimitDao.insertOrUpdate(BudgetLimit(category, limit, currentCurrencyCode, userGuid = userGuid))
            } else {
                budgetLimitDao.delete(userGuid, category)
            }
        }
    }
}
