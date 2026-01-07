package com.example.budgettingtogether

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.budgettingtogether.databinding.ItemIncomeBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class IncomeAdapter(
    private val incomeList: List<Income>,
    private val onDeleteClick: (Income) -> Unit
) : RecyclerView.Adapter<IncomeAdapter.IncomeViewHolder>() {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IncomeViewHolder {
        val binding = ItemIncomeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return IncomeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: IncomeViewHolder, position: Int) {
        holder.bind(incomeList[position])
    }

    override fun getItemCount(): Int = incomeList.size

    inner class IncomeViewHolder(
        private val binding: ItemIncomeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(income: Income) {
            binding.apply {
                textViewTitle.text = income.title
                textViewAmount.text = currencyFormat.format(income.amount)
                textViewSource.text = income.source
                textViewDate.text = dateFormat.format(income.date)

                buttonDelete.setOnClickListener {
                    onDeleteClick(income)
                }
            }
        }
    }
}
