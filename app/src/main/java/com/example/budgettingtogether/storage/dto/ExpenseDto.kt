package com.example.budgettingtogether.storage.dto

import com.example.budgettingtogether.expenses.Expense
import com.example.budgettingtogether.util.RecurringType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Date

@Serializable
data class ExpenseDto(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("amount") val amount: Double,
    @SerialName("category") val category: String,
    @SerialName("date") val date: Long,
    @SerialName("recurring_type") val recurringType: String,
    @SerialName("original_amount") val originalAmount: Double? = null,
    @SerialName("original_currency") val originalCurrency: String? = null,
    @SerialName("user_guid") val userGuid: String
) {
    fun toEntity(): Expense = Expense(
        id = id,
        title = title,
        amount = amount,
        category = category,
        date = Date(date),
        recurringType = RecurringType.valueOf(recurringType),
        originalAmount = originalAmount,
        originalCurrency = originalCurrency,
        userGuid = userGuid
    )

    companion object {
        fun fromEntity(e: Expense) = ExpenseDto(
            id = e.id,
            title = e.title,
            amount = e.amount,
            category = e.category,
            date = e.date.time,
            recurringType = e.recurringType.name,
            originalAmount = e.originalAmount,
            originalCurrency = e.originalCurrency,
            userGuid = e.userGuid
        )
    }
}
