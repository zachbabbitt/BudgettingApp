package com.example.budgettingtogether

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.budgettingtogether.databinding.ActivityBudgetLimitsBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BudgetLimitsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBudgetLimitsBinding
    private lateinit var budgetLimitDao: BudgetLimitDao
    private lateinit var adapter: BudgetLimitAdapter

    private val categories = listOf(
        "Food & Dining",
        "Transportation",
        "Shopping",
        "Entertainment",
        "Bills & Utilities",
        "Health",
        "Other"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBudgetLimitsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val database = AppDatabase.getDatabase(this)
        budgetLimitDao = database.budgetLimitDao()

        setupToolbar()
        setupRecyclerView()
        loadLimits()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.budget_limits_title)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupRecyclerView() {
        adapter = BudgetLimitAdapter(categories) { category, limit ->
            saveLimitDebounced(category, limit)
        }
        binding.recyclerViewLimits.apply {
            layoutManager = LinearLayoutManager(this@BudgetLimitsActivity)
            adapter = this@BudgetLimitsActivity.adapter
        }
    }

    private fun loadLimits() {
        lifecycleScope.launch {
            val limits = budgetLimitDao.getAllLimits().first()
            adapter.setLimits(limits)
        }
    }

    private fun saveLimitDebounced(category: String, limit: Double?) {
        lifecycleScope.launch {
            if (limit != null && limit > 0) {
                budgetLimitDao.insertOrUpdate(BudgetLimit(category, limit))
            } else {
                budgetLimitDao.delete(category)
            }
        }
    }
}
