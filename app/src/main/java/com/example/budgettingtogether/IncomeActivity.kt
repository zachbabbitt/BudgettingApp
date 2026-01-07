package com.example.budgettingtogether

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.budgettingtogether.databinding.ActivityIncomeBinding
import com.example.budgettingtogether.databinding.DialogAddIncomeBinding

class IncomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIncomeBinding
    private lateinit var incomeAdapter: IncomeAdapter
    private val incomeList = mutableListOf<Income>()

    private val sources = listOf(
        "Salary",
        "Freelance",
        "Investments",
        "Business",
        "Rental",
        "Gifts",
        "Other"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIncomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupFab()
        updateTotalDisplay()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.income_title)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupRecyclerView() {
        incomeAdapter = IncomeAdapter(incomeList) { income ->
            deleteIncome(income)
        }
        binding.recyclerViewIncome.apply {
            layoutManager = LinearLayoutManager(this@IncomeActivity)
            adapter = incomeAdapter
        }
    }

    private fun setupFab() {
        binding.fabAddIncome.setOnClickListener {
            showAddIncomeDialog()
        }
    }

    private fun showAddIncomeDialog() {
        val dialogBinding = DialogAddIncomeBinding.inflate(LayoutInflater.from(this))

        val sourceAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, sources)
        dialogBinding.spinnerSource.setAdapter(sourceAdapter)

        AlertDialog.Builder(this)
            .setTitle(R.string.add_income)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.add) { _, _ ->
                val title = dialogBinding.editTextTitle.text.toString().trim()
                val amountStr = dialogBinding.editTextAmount.text.toString().trim()
                val source = dialogBinding.spinnerSource.text.toString().trim()

                if (title.isEmpty() || amountStr.isEmpty() || source.isEmpty()) {
                    Toast.makeText(this, R.string.fill_all_fields, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val amount = amountStr.toDoubleOrNull()
                if (amount == null || amount <= 0) {
                    Toast.makeText(this, R.string.invalid_amount, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                addIncome(Income(title = title, amount = amount, source = source))
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun addIncome(income: Income) {
        incomeList.add(0, income)
        incomeAdapter.notifyItemInserted(0)
        binding.recyclerViewIncome.scrollToPosition(0)
        updateTotalDisplay()
    }

    private fun deleteIncome(income: Income) {
        val position = incomeList.indexOf(income)
        if (position != -1) {
            incomeList.removeAt(position)
            incomeAdapter.notifyItemRemoved(position)
            updateTotalDisplay()
        }
    }

    private fun updateTotalDisplay() {
        val total = incomeList.sumOf { it.amount }
        binding.textViewTotal.text = getString(R.string.total_format, total)
    }
}
