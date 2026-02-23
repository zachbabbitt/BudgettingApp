package com.example.budgettingtogether.storage.dto

import com.example.budgettingtogether.income.Income
import com.example.budgettingtogether.util.RecurringType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Date

@Serializable
data class IncomeDto(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("amount") val amount: Double,
    @SerialName("source") val source: String,
    @SerialName("date") val date: Long,
    @SerialName("recurring_type") val recurringType: String,
    @SerialName("notes") val notes: String = "",
    @SerialName("original_amount") val originalAmount: Double? = null,
    @SerialName("original_currency") val originalCurrency: String? = null,
    @SerialName("user_guid") val userGuid: String
) {
    fun toEntity(): Income = Income(
        id = id,
        title = title,
        amount = amount,
        source = source,
        date = Date(date),
        recurringType = RecurringType.valueOf(recurringType),
        notes = notes,
        originalAmount = originalAmount,
        originalCurrency = originalCurrency,
        userGuid = userGuid
    )

    companion object {
        fun fromEntity(i: Income) = IncomeDto(
            id = i.id,
            title = i.title,
            amount = i.amount,
            source = i.source,
            date = i.date.time,
            recurringType = i.recurringType.name,
            notes = i.notes,
            originalAmount = i.originalAmount,
            originalCurrency = i.originalCurrency,
            userGuid = i.userGuid
        )
    }
}
