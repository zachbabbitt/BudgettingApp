package com.example.budgettingtogether.limits

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.budgettingtogether.databinding.ItemBudgetLimitBinding

class BudgetLimitAdapter(
    private var categories: List<String>,
    private var currencySymbol: String,
    private val onLimitChanged: (String, Double?) -> Unit
) : RecyclerView.Adapter<BudgetLimitAdapter.LimitViewHolder>() {

    init {
        setHasStableIds(true)
    }

    private val limits = mutableMapOf<String, Double>()
    private var isInitialLoad = true

    fun updateCurrency(newSymbol: String) {
        if (currencySymbol != newSymbol) {
            currencySymbol = newSymbol
            notifyDataSetChanged()
        }
    }

    override fun getItemId(position: Int): Long {
        return categories[position].hashCode().toLong()
    }

    fun setLimits(budgetLimits: List<BudgetLimit>) {
        val newLimits = budgetLimits.associate { it.category to it.limitAmount }

        // Only notify if this is initial load or if limits actually changed
        val hasChanges = isInitialLoad || limits != newLimits

        limits.clear()
        limits.putAll(newLimits)

        if (hasChanges && isInitialLoad) {
            isInitialLoad = false
            notifyDataSetChanged()
        }
    }

    fun updateCategories(newCategories: List<String>) {
        if (categories != newCategories) {
            categories = newCategories
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LimitViewHolder {
        val binding = ItemBudgetLimitBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LimitViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LimitViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    override fun getItemCount(): Int = categories.size

    inner class LimitViewHolder(
        private val binding: ItemBudgetLimitBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var textWatcher: TextWatcher? = null

        fun bind(category: String) {
            binding.textViewCategory.text = category
            binding.textInputLimit.prefixText = currencySymbol

            // Remove previous watcher to avoid triggering on setText
            textWatcher?.let { binding.editTextLimit.removeTextChangedListener(it) }

            // Set current limit value
            val currentLimit = limits[category]
            binding.editTextLimit.setText(
                if (currentLimit != null && currentLimit > 0) {
                    String.format("%.2f", currentLimit)
                } else {
                    ""
                }
            )

            // Add new watcher
            textWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val value = s?.toString()?.toDoubleOrNull()
                    onLimitChanged(category, value)
                }
            }
            binding.editTextLimit.addTextChangedListener(textWatcher)
        }
    }
}
