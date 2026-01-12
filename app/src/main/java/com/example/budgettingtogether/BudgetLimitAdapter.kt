package com.example.budgettingtogether

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.budgettingtogether.databinding.ItemBudgetLimitBinding

class BudgetLimitAdapter(
    private val categories: List<String>,
    private val onLimitChanged: (String, Double?) -> Unit
) : RecyclerView.Adapter<BudgetLimitAdapter.LimitViewHolder>() {

    private val limits = mutableMapOf<String, Double>()

    fun setLimits(budgetLimits: List<BudgetLimit>) {
        limits.clear()
        budgetLimits.forEach { limits[it.category] = it.limitAmount }
        notifyDataSetChanged()
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
