package com.example.budgettingtogether

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.budgettingtogether.databinding.ItemIncomeBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class IncomeAdapter(
    private val onDeleteClick: (Income) -> Unit
) : RecyclerView.Adapter<IncomeAdapter.IncomeViewHolder>() {

    private var incomeList: List<Income> = emptyList()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

    fun updateList(newList: List<Income>) {
        val diffCallback = IncomeDiffCallback(incomeList, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        incomeList = newList
        diffResult.dispatchUpdatesTo(this)
    }

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
                textViewRecurring.visibility = if (income.isRecurring) View.VISIBLE else View.GONE

                buttonDelete.setOnClickListener {
                    onDeleteClick(income)
                }
            }
        }
    }

    private class IncomeDiffCallback(
        private val oldList: List<Income>,
        private val newList: List<Income>
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size
        override fun areItemsTheSame(oldPos: Int, newPos: Int) = oldList[oldPos].id == newList[newPos].id
        override fun areContentsTheSame(oldPos: Int, newPos: Int) = oldList[oldPos] == newList[newPos]
    }
}
