package com.example.budgettingtogether.storage.dto

import com.example.budgettingtogether.util.UserPreferences
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserPreferencesDto(
    @SerialName("user_guid") val userGuid: String,
    @SerialName("default_currency_code_expenses") val defaultCurrencyCodeExpenses: String = "USD",
    @SerialName("default_currency_code_tracking") val defaultCurrencyCodeTracking: String = "USD",
    @SerialName("default_currency_code_income") val defaultCurrencyCodeIncome: String = "USD",
    @SerialName("last_rates_update") val lastRatesUpdate: Long = 0L,
    @SerialName("last_recurring_generation_month") val lastRecurringGenerationMonth: Int = -1,
    @SerialName("last_recurring_generation_year") val lastRecurringGenerationYear: Int = -1
) {
    fun toEntity(): UserPreferences = UserPreferences(
        id = 1,
        defaultCurrencyCodeExpenses = defaultCurrencyCodeExpenses,
        defaultCurrencyCodeTracking = defaultCurrencyCodeTracking,
        defaultCurrencyCodeIncome = defaultCurrencyCodeIncome,
        lastRatesUpdate = lastRatesUpdate,
        lastRecurringGenerationMonth = lastRecurringGenerationMonth,
        lastRecurringGenerationYear = lastRecurringGenerationYear,
        userGuid = userGuid
    )

    companion object {
        fun fromEntity(p: UserPreferences) = UserPreferencesDto(
            userGuid = p.userGuid,
            defaultCurrencyCodeExpenses = p.defaultCurrencyCodeExpenses,
            defaultCurrencyCodeTracking = p.defaultCurrencyCodeTracking,
            defaultCurrencyCodeIncome = p.defaultCurrencyCodeIncome,
            lastRatesUpdate = p.lastRatesUpdate,
            lastRecurringGenerationMonth = p.lastRecurringGenerationMonth,
            lastRecurringGenerationYear = p.lastRecurringGenerationYear
        )
    }
}
