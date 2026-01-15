package com.example.budgettingtogether

import java.text.SimpleDateFormat
import java.util.Locale

object CsvExporter {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun export(expenses: List<Expense>, income: List<Income>): String {
        val builder = StringBuilder()

        // Expenses section
        builder.appendLine("EXPENSES")
        builder.appendLine("Date,Title,Amount,Category,Recurring")
        expenses.sortedBy { it.date }.forEach { expense ->
            builder.appendLine(
                "${dateFormat.format(expense.date)}," +
                "${escapeCsv(expense.title)}," +
                "${expense.amount}," +
                "${escapeCsv(expense.category)}," +
                "${expense.recurringType.name}"
            )
        }

        builder.appendLine()

        // Income section
        builder.appendLine("INCOME")
        builder.appendLine("Date,Title,Amount,Source,Recurring")
        income.sortedBy { it.date }.forEach { inc ->
            builder.appendLine(
                "${dateFormat.format(inc.date)}," +
                "${escapeCsv(inc.title)}," +
                "${inc.amount}," +
                "${escapeCsv(inc.source)}," +
                "${inc.recurringType.name}"
            )
        }

        return builder.toString()
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
