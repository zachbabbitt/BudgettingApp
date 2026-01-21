package com.example.budgettingtogether

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.budgettingtogether.core.AppDatabase
import com.example.budgettingtogether.currency.ExchangeRate
import com.example.budgettingtogether.currency.ExchangeRateDao
import com.example.budgettingtogether.util.UserPreferences
import com.example.budgettingtogether.util.UserPreferencesDao
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CurrencyConversionTest {

    private lateinit var database: AppDatabase
    private lateinit var exchangeRateDao: ExchangeRateDao
    private lateinit var userPreferencesDao: UserPreferencesDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        exchangeRateDao = database.exchangeRateDao()
        userPreferencesDao = database.userPreferencesDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    private suspend fun setupExchangeRates() {
        val rates = listOf(
            ExchangeRate("USD", 1.0, "US Dollar", "$"),
            ExchangeRate("EUR", 0.92, "Euro", "€"),
            ExchangeRate("GBP", 0.79, "British Pound", "£"),
            ExchangeRate("JPY", 149.50, "Japanese Yen", "¥"),
            ExchangeRate("CAD", 1.36, "Canadian Dollar", "C$")
        )
        exchangeRateDao.insertAll(rates)
    }

    // Keep in sync with CurrencyRepository's convert
    private suspend fun convert(amount: Double, fromCurrency: String, toCurrency: String): Double {
        if (fromCurrency == toCurrency) return amount
        val fromRate = exchangeRateDao.getRate(fromCurrency)?.rateToUsd ?: 1.0
        val toRate = exchangeRateDao.getRate(toCurrency)?.rateToUsd ?: 1.0
        val amountInUsd = amount / fromRate
        return amountInUsd * toRate
    }

    // region Expense to Tracking Currency Tests

    @Test
    fun expenseToTracking_eurExpenseToUsdTracking() = runTest {
        setupExchangeRates()
        userPreferencesDao.savePreferences(
            UserPreferences(
                defaultCurrencyCodeExpenses = "EUR",
                defaultCurrencyCodeTracking = "USD"
            )
        )

        // User enters 100 EUR expense, tracking in USD
        val expenseAmount = 100.0
        val expenseCurrency = "EUR"
        val trackingCurrency = "USD"

        val convertedExpense = convert(expenseAmount, expenseCurrency, trackingCurrency)

        // 100 EUR / 0.92 * 1.0 = 108.70 USD
        assertEquals(108.70, convertedExpense, 0.01)
    }

    @Test
    fun expenseToTracking_usdExpenseToEurTracking() = runTest {
        setupExchangeRates()
        userPreferencesDao.savePreferences(
            UserPreferences(
                defaultCurrencyCodeExpenses = "USD",
                defaultCurrencyCodeTracking = "EUR"
            )
        )

        val expenseAmount = 100.0
        val convertedExpense = convert(expenseAmount, "USD", "EUR")

        // 100 USD / 1.0 * 0.92 = 92 EUR
        assertEquals(92.0, convertedExpense, 0.01)
    }

    @Test
    fun expenseToTracking_jpyExpenseToUsdTracking() = runTest {
        setupExchangeRates()

        // User enters 15000 JPY expense, tracking in USD
        val expenseAmount = 15000.0
        val convertedExpense = convert(expenseAmount, "JPY", "USD")

        // 15000 JPY / 149.50 * 1.0 = 100.33 USD
        assertEquals(100.33, convertedExpense, 0.01)
    }

    @Test
    fun expenseToTracking_sameCurrency_noConversion() = runTest {
        setupExchangeRates()

        val expenseAmount = 100.0
        val convertedExpense = convert(expenseAmount, "USD", "USD")

        assertEquals(100.0, convertedExpense, 0.001)
    }

    // endregion

    // region Income to Tracking Currency Tests

    @Test
    fun incomeToTracking_gbpIncomeToUsdTracking() = runTest {
        setupExchangeRates()
        userPreferencesDao.savePreferences(
            UserPreferences(
                defaultCurrencyCodeIncome = "GBP",
                defaultCurrencyCodeTracking = "USD"
            )
        )

        // User enters 1000 GBP income, tracking in USD
        val incomeAmount = 1000.0
        val incomeCurrency = "GBP"
        val trackingCurrency = "USD"

        val convertedIncome = convert(incomeAmount, incomeCurrency, trackingCurrency)

        // 1000 GBP / 0.79 * 1.0 = 1265.82 USD
        assertEquals(1265.82, convertedIncome, 0.01)
    }

    @Test
    fun incomeToTracking_cadIncomeToEurTracking() = runTest {
        setupExchangeRates()

        // User enters 5000 CAD income, tracking in EUR
        val incomeAmount = 5000.0
        val convertedIncome = convert(incomeAmount, "CAD", "EUR")

        // 5000 CAD / 1.36 * 0.92 = 3382.35 EUR
        assertEquals(3382.35, convertedIncome, 0.01)
    }

    @Test
    fun incomeToTracking_jpyIncomeToGbpTracking() = runTest {
        setupExchangeRates()

        // User enters 500000 JPY income, tracking in GBP
        val incomeAmount = 500000.0
        val convertedIncome = convert(incomeAmount, "JPY", "GBP")

        // 500000 JPY / 149.50 * 0.79 = 2641.47 GBP
        assertEquals(2642.1404682274247, convertedIncome, 0.01)
    }

    // endregion

    // region Expense/Tracking/Income Relationship Tests

    @Test
    fun expenseIncomeComparison_bothConvertedToTrackingCurrency() = runTest {
        setupExchangeRates()
        userPreferencesDao.savePreferences(
            UserPreferences(
                defaultCurrencyCodeExpenses = "EUR",
                defaultCurrencyCodeIncome = "GBP",
                defaultCurrencyCodeTracking = "USD"
            )
        )

        // Expense: 500 EUR
        val expenseInEur = 500.0
        val expenseInTracking = convert(expenseInEur, "EUR", "USD")

        // Income: 1000 GBP
        val incomeInGbp = 1000.0
        val incomeInTracking = convert(incomeInGbp, "GBP", "USD")

        // Expense: 500 EUR / 0.92 * 1.0 = 543.48 USD
        assertEquals(543.48, expenseInTracking, 0.01)

        // Income: 1000 GBP / 0.79 * 1.0 = 1265.82 USD
        assertEquals(1265.82, incomeInTracking, 0.01)

        // Remaining budget in tracking currency
        val remainingBudget = incomeInTracking - expenseInTracking
        assertEquals(722.34, remainingBudget, 0.01)
    }

    @Test
    fun expenseIncomeComparison_multipleExpensesAndIncomes() = runTest {
        setupExchangeRates()
        val trackingCurrency = "USD"

        // Multiple expenses in different currencies
        val expenses = listOf(
            Pair(100.0, "EUR"),   // 100 EUR
            Pair(50.0, "GBP"),    // 50 GBP
            Pair(5000.0, "JPY")   // 5000 JPY
        )

        // Multiple incomes in different currencies
        val incomes = listOf(
            Pair(2000.0, "EUR"),  // 2000 EUR
            Pair(500.0, "GBP")    // 500 GBP
        )

        // Convert all expenses to tracking currency
        var totalExpensesInTracking = 0.0
        for ((amount, currency) in expenses) {
            totalExpensesInTracking += convert(amount, currency, trackingCurrency)
        }

        // Convert all incomes to tracking currency
        var totalIncomesInTracking = 0.0
        for ((amount, currency) in incomes) {
            totalIncomesInTracking += convert(amount, currency, trackingCurrency)
        }

        // Expenses: 100/0.92 + 50/0.79 + 5000/149.50 = 108.70 + 63.29 + 33.44 = 205.43 USD
        assertEquals(205.43, totalExpensesInTracking, 0.01)

        // Incomes: 2000/0.92 + 500/0.79 = 2173.91 + 632.91 = 2806.82 USD
        assertEquals(2806.82, totalIncomesInTracking, 0.01)

        // Net savings
        val netSavings = totalIncomesInTracking - totalExpensesInTracking
        assertEquals(2601.39, netSavings, 0.01)
    }

    @Test
    fun expenseIncomeComparison_allSameCurrency() = runTest {
        setupExchangeRates()
        userPreferencesDao.savePreferences(
            UserPreferences(
                defaultCurrencyCodeExpenses = "EUR",
                defaultCurrencyCodeIncome = "EUR",
                defaultCurrencyCodeTracking = "EUR"
            )
        )

        val expenseAmount = 500.0
        val incomeAmount = 2000.0

        val expenseInTracking = convert(expenseAmount, "EUR", "EUR")
        val incomeInTracking = convert(incomeAmount, "EUR", "EUR")

        // No conversion needed
        assertEquals(500.0, expenseInTracking, 0.001)
        assertEquals(2000.0, incomeInTracking, 0.001)
        assertEquals(1500.0, incomeInTracking - expenseInTracking, 0.001)
    }

    @Test
    fun expenseIncomeComparison_expenseExceedsIncome() = runTest {
        setupExchangeRates()

        // Large expense in strong currency (GBP)
        val expenseInGbp = 2000.0
        val expenseInTracking = convert(expenseInGbp, "GBP", "USD")

        // Small income in weak currency (JPY)
        val incomeInJpy = 100000.0
        val incomeInTracking = convert(incomeInJpy, "JPY", "USD")

        // Expense: 2000 GBP / 0.79 * 1.0 = 2531.65 USD
        assertEquals(2531.65, expenseInTracking, 0.01)

        // Income: 100000 JPY / 149.50 * 1.0 = 668.90 USD
        assertEquals(668.90, incomeInTracking, 0.01)

        // Deficit
        val deficit = incomeInTracking - expenseInTracking
        assertTrue(deficit < 0)
        assertEquals(-1862.75, deficit, 0.01)
    }

    // endregion

    // region Round-Trip Conversion Tests

    @Test
    fun roundTrip_expenseCurrencyToTrackingAndBack() = runTest {
        setupExchangeRates()

        val originalAmount = 100.0
        val expenseCurrency = "EUR"
        val trackingCurrency = "USD"

        // Convert to tracking
        val inTracking = convert(originalAmount, expenseCurrency, trackingCurrency)

        // Convert back to expense currency
        val backToOriginal = convert(inTracking, trackingCurrency, expenseCurrency)

        // Should return approximately the original amount
        assertEquals(originalAmount, backToOriginal, 0.01)
    }

    @Test
    fun roundTrip_throughMultipleCurrencies() = runTest {
        setupExchangeRates()

        val originalAmount = 1000.0

        // EUR -> USD -> GBP -> JPY -> EUR
        val step1 = convert(originalAmount, "EUR", "USD")
        val step2 = convert(step1, "USD", "GBP")
        val step3 = convert(step2, "GBP", "JPY")
        val step4 = convert(step3, "JPY", "EUR")

        // Should return approximately the original amount
        assertEquals(originalAmount, step4, 0.01)
    }

    // endregion

    // region Edge Cases

    @Test
    fun conversion_zeroAmount() = runTest {
        setupExchangeRates()

        val result = convert(0.0, "EUR", "USD")
        assertEquals(0.0, result, 0.001)
    }

    @Test
    fun conversion_verySmallAmount() = runTest {
        setupExchangeRates()

        val result = convert(0.01, "USD", "JPY")
        // 0.01 USD * 149.50 = 1.495 JPY
        assertEquals(1.495, result, 0.001)
    }

    @Test
    fun conversion_veryLargeAmount() = runTest {
        setupExchangeRates()

        val result = convert(1_000_000_000.0, "JPY", "USD")
        // 1 billion JPY / 149.50 = 6,688,963.21 USD
        assertEquals(6_688_963.21, result, 1.0)
    }

    // endregion
}
