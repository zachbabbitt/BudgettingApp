package com.example.budgettingtogether

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.budgettingtogether.databinding.ItemExpenseBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class ExpenseAdapter(
    private val onDeleteClick: (Expense) -> Unit
) : RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    private var expenses: List<Expense> = emptyList()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

    fun updateList(newExpenses: List<Expense>) {
        val diffCallback = ExpenseDiffCallback(expenses, newExpenses)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        expenses = newExpenses
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val binding = ItemExpenseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ExpenseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        holder.bind(expenses[position])
    }

    override fun getItemCount(): Int = expenses.size

    inner class ExpenseViewHolder(
        private val binding: ItemExpenseBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(expense: Expense) {
            binding.apply {
                textViewTitle.text = expense.title
                textViewAmount.text = currencyFormat.format(expense.amount)
                textViewCategory.text = expense.category
                textViewDate.text = dateFormat.format(expense.date)

                when (expense.recurringType) {
                    RecurringType.NONE -> {
                        textViewRecurring.visibility = View.GONE
                    }
                    RecurringType.WEEKLY -> {
                        textViewRecurring.visibility = View.VISIBLE
                        textViewRecurring.text = root.context.getString(R.string.recurring_weekly)
                    }
                    RecurringType.MONTHLY -> {
                        textViewRecurring.visibility = View.VISIBLE
                        textViewRecurring.text = root.context.getString(R.string.recurring_monthly)
                    }
                }

                buttonDelete.setOnClickListener {
                    onDeleteClick(expense)
                }
            }
        }
    }

    private class ExpenseDiffCallback(
        private val oldList: List<Expense>,
        private val newList: List<Expense>
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size
        override fun areItemsTheSame(oldPos: Int, newPos: Int) = oldList[oldPos].id == newList[newPos].id
        override fun areContentsTheSame(oldPos: Int, newPos: Int) = oldList[oldPos] == newList[newPos]
    }
}
