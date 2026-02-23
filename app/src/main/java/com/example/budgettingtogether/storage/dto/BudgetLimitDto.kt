package com.example.budgettingtogether.storage.dto

import com.example.budgettingtogether.limits.BudgetLimit
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BudgetLimitDto(
    @SerialName("category") val category: String,
    @SerialName("limit_amount") val limitAmount: Double,
    @SerialName("currency_code") val currencyCode: String = "USD",
    @SerialName("user_guid") val userGuid: String
) {
    fun toEntity(): BudgetLimit = BudgetLimit(
        category = category,
        limitAmount = limitAmount,
        currencyCode = currencyCode,
        userGuid = userGuid
    )

    companion object {
        fun fromEntity(b: BudgetLimit) = BudgetLimitDto(
            category = b.category,
            limitAmount = b.limitAmount,
            currencyCode = b.currencyCode,
            userGuid = b.userGuid
        )
    }
}
