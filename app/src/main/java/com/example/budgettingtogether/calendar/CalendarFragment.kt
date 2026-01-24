package com.example.budgettingtogether.calendar

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.budgettingtogether.R
import com.example.budgettingtogether.core.AppDatabase
import com.example.budgettingtogether.currency.CurrencyData
import com.example.budgettingtogether.currency.CurrencyRepository
import com.example.budgettingtogether.databinding.FragmentCalendarBinding
import com.example.budgettingtogether.databinding.ItemCalendarDayBinding
import com.example.budgettingtogether.databinding.ItemCalendarExpenseBinding
import com.example.budgettingtogether.expenses.Expense
import com.example.budgettingtogether.expenses.ExpenseDao
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    private lateinit var expenseDao: ExpenseDao
    private lateinit var currencyRepository: CurrencyRepository
    private var currencySymbol: String = "$"

    private val displayedMonth = Calendar.getInstance()
    private var selectedDate: Date = Date()
    private var allExpenses: List<Expense> = emptyList()

    private data class DayCell(
        val binding: ItemCalendarDayBinding,
        val date: Date?,
        val dayOfMonth: Int
    )

    private val dayCells = mutableListOf<DayCell>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val database = AppDatabase.getDatabase(requireContext())
        expenseDao = database.expenseDao()
        currencyRepository = CurrencyRepository(requireContext())

        setupNavigation()
        buildCalendarGrid()
        updateCalendar()
        observeData()
    }

    private fun setupNavigation() {
        binding.buttonPrevMonth.setOnClickListener {
            displayedMonth.add(Calendar.MONTH, -1)
            updateCalendar()
            updateExpenseIndicators()
            updateSelectedDayExpenses()
        }

        binding.buttonNextMonth.setOnClickListener {
            displayedMonth.add(Calendar.MONTH, 1)
            updateCalendar()
            updateExpenseIndicators()
            updateSelectedDayExpenses()
        }
    }

    private fun buildCalendarGrid() {
        val gridContainer = binding.linearLayoutCalendarGrid
        gridContainer.removeAllViews()
        dayCells.clear()

        // Create 6 rows of 7 days each
        for (row in 0 until 6) {
            val rowLayout = LinearLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                orientation = LinearLayout.HORIZONTAL
            }

            for (col in 0 until 7) {
                val dayBinding = ItemCalendarDayBinding.inflate(
                    LayoutInflater.from(requireContext()),
                    rowLayout,
                    false
                )

                dayBinding.root.setOnClickListener {
                    val cellIndex = row * 7 + col
                    val cell = dayCells.getOrNull(cellIndex)
                    cell?.date?.let { date ->
                        selectDate(date)
                    }
                }

                rowLayout.addView(dayBinding.root)
                dayCells.add(DayCell(dayBinding, null, 0))
            }

            gridContainer.addView(rowLayout)
        }
    }

    private fun updateCalendar() {
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        binding.textViewMonthYear.text = monthFormat.format(displayedMonth.time)

        val calendar = displayedMonth.clone() as Calendar
        calendar.set(Calendar.DAY_OF_MONTH, 1)

        val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        val today = Calendar.getInstance()

        // Update each cell
        for (i in dayCells.indices) {
            val dayBinding = dayCells[i].binding
            val dayOfMonth = i - firstDayOfWeek + 1

            if (dayOfMonth in 1..daysInMonth) {
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                val cellDate = calendar.time

                dayBinding.textViewDayNumber.text = dayOfMonth.toString()
                dayBinding.textViewDayNumber.visibility = View.VISIBLE
                dayBinding.root.isClickable = true

                // Update the cell data
                dayCells[i] = DayCell(dayBinding, cellDate, dayOfMonth)

                // Check if this is today
                val isToday = isSameDay(cellDate, today.time)
                dayBinding.viewTodayIndicator.visibility = if (isToday) View.VISIBLE else View.GONE

                // Check if this is the selected date
                val isSelected = isSameDay(cellDate, selectedDate)
                dayBinding.viewSelectedBackground.visibility = if (isSelected) View.VISIBLE else View.GONE

                // Update text color based on selection and dark mode
                val isDarkMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                if (isSelected) {
                    dayBinding.textViewDayNumber.setTextColor(
                        requireContext().getColor(android.R.color.white)
                    )
                } else {
                    dayBinding.textViewDayNumber.setTextColor(
                        requireContext().getColor(if (isDarkMode) android.R.color.white else android.R.color.black)
                    )
                }
            } else {
                dayBinding.textViewDayNumber.text = ""
                dayBinding.textViewDayNumber.visibility = View.INVISIBLE
                dayBinding.viewTodayIndicator.visibility = View.GONE
                dayBinding.viewSelectedBackground.visibility = View.GONE
                dayBinding.viewHasExpenses.visibility = View.GONE
                dayBinding.root.isClickable = false

                dayCells[i] = DayCell(dayBinding, null, 0)
            }
        }

        // Update selected date display
        updateSelectedDateHeader()
    }

    private fun selectDate(date: Date) {
        selectedDate = date
        updateCalendar()
        updateSelectedDayExpenses()
    }

    private fun updateSelectedDateHeader() {
        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        binding.textViewSelectedDate.text = dateFormat.format(selectedDate)
    }

    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            combine(
                expenseDao.getAllExpenses(),
                currencyRepository.observeDefaultCurrencyTracking()
            ) { expenses, trackingCurrency ->
                currencySymbol = CurrencyData.getSymbol(trackingCurrency)

                // Convert expenses to tracking currency
                expenses.map { expense ->
                    val originalAmount = expense.originalAmount ?: expense.amount
                    val originalCurrency = expense.originalCurrency ?: "USD"
                    val convertedAmount = currencyRepository.convert(originalAmount, originalCurrency, trackingCurrency)
                    expense.copy(amount = convertedAmount)
                }
            }.collectLatest { expenses ->
                allExpenses = expenses
                updateExpenseIndicators()
                updateSelectedDayExpenses()
            }
        }
    }

    private fun updateExpenseIndicators() {
        for (cell in dayCells) {
            val date = cell.date ?: continue
            val hasExpenses = allExpenses.any { isSameDay(it.date, date) }
            cell.binding.viewHasExpenses.visibility = if (hasExpenses) View.VISIBLE else View.GONE
        }
    }

    private fun updateSelectedDayExpenses() {
        val dayExpenses = allExpenses.filter { isSameDay(it.date, selectedDate) }

        binding.linearLayoutExpenses.removeAllViews()

        if (dayExpenses.isEmpty()) {
            binding.textViewNoExpenses.visibility = View.VISIBLE
            binding.textViewDayTotal.visibility = View.GONE
        } else {
            binding.textViewNoExpenses.visibility = View.GONE
            binding.textViewDayTotal.visibility = View.VISIBLE

            val total = dayExpenses.sumOf { it.amount }
            binding.textViewDayTotal.text = getString(R.string.day_total, formatCurrency(total))

            for (expense in dayExpenses) {
                val itemBinding = ItemCalendarExpenseBinding.inflate(
                    LayoutInflater.from(requireContext()),
                    binding.linearLayoutExpenses,
                    false
                )

                itemBinding.textViewExpenseTitle.text = expense.title
                itemBinding.textViewExpenseCategory.text = expense.category
                itemBinding.textViewExpenseAmount.text = formatCurrency(expense.amount)

                binding.linearLayoutExpenses.addView(itemBinding.root)
            }
        }
    }

    private fun formatCurrency(amount: Double): String {
        return "$currencySymbol${String.format("%.2f", amount)}"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
