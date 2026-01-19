package com.example.budgettingtogether

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class CurrencyRepository(context: Context) {

    companion object {
        private const val PREFS_NAME = "currency_prefs"
        private const val KEY_RECENT_CURRENCIES = "recent_currencies"
        private const val MAX_RECENT_CURRENCIES = 3
    }

    private val database = AppDatabase.getDatabase(context)
    private val exchangeRateDao = database.exchangeRateDao()
    private val userPreferencesDao = database.userPreferencesDao()
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val apiService: ExchangeRateApiService by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(ExchangeRateApiService.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ExchangeRateApiService::class.java)
    }

    suspend fun fetchAndSaveRates(): Result<Unit> {
        return try {
            val response = apiService.getLatestRates()

            if (response.result != "success") {
                return Result.failure(Exception("API returned error"))
            }

            val rates = response.rates.mapNotNull { (code, rate) ->
                CurrencyData.currencies[code]?.let { info ->
                    ExchangeRate(
                        currencyCode = code,
                        rateToUsd = rate,
                        currencyName = info.name,
                        symbol = info.symbol
                    )
                }
            }

            exchangeRateDao.deleteAll()
            exchangeRateDao.insertAll(rates)

            // Update last refresh time
            val prefs = userPreferencesDao.getPreferencesOnce() ?: UserPreferences()
            userPreferencesDao.savePreferences(prefs.copy(lastRatesUpdate = System.currentTimeMillis()))

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDefaultCurrencyTracking(): String {
        return userPreferencesDao.getPreferencesOnce()?.defaultCurrencyCodeTracking ?: "USD"
    }

    suspend fun getDefaultCurrencyExpenses(): String {
        return userPreferencesDao.getPreferencesOnce()?.defaultCurrencyCodeExpenses ?: "USD"
    }

    suspend fun getDefaultCurrencyIncome() : String {
        return userPreferencesDao.getPreferencesOnce()?.defaultCurrencyCodeIncome ?: "USD"
    }

    suspend fun setDefaultCurrencyTracking(currencyCode: String) {
        val prefs = userPreferencesDao.getPreferencesOnce() ?: UserPreferences()
        userPreferencesDao.savePreferences(prefs.copy(defaultCurrencyCodeTracking = currencyCode))
    }

    suspend fun setDefaultCurrencyExpenses(currencyCode: String) {
        val prefs = userPreferencesDao.getPreferencesOnce() ?: UserPreferences()
        userPreferencesDao.savePreferences(prefs.copy(defaultCurrencyCodeExpenses = currencyCode))
    }

    suspend fun setDefaultCurrencyIncome(currencyCode: String) {
        val prefs = userPreferencesDao.getPreferencesOnce() ?: UserPreferences()
        userPreferencesDao.savePreferences(prefs.copy(defaultCurrencyCodeIncome = currencyCode))
    }

    suspend fun getLastUpdateTime(): Long {
        return userPreferencesDao.getPreferencesOnce()?.lastRatesUpdate ?: 0L
    }

    suspend fun getAllRates(): List<ExchangeRate> {
        return exchangeRateDao.getAllRatesOnce()
    }

    suspend fun hasRates(): Boolean {
        return exchangeRateDao.getCount() > 0
    }

    suspend fun convert(amount: Double, fromCurrency: String, toCurrency: String): Double {
        if (fromCurrency == toCurrency) return amount

        val fromRate = exchangeRateDao.getRate(fromCurrency)?.rateToUsd ?: 1.0
        val toRate = exchangeRateDao.getRate(toCurrency)?.rateToUsd ?: 1.0

        // Convert: amount in fromCurrency -> USD -> toCurrency
        // If fromCurrency rate is 18.5 (ZAR per USD), then 100 ZAR = 100/18.5 USD
        // If toCurrency rate is 149.2 (JPY per USD), then X USD = X * 149.2 JPY
        val amountInUsd = amount / fromRate
        return amountInUsd * toRate
    }

    suspend fun getConversionPreview(amount: Double, fromCurrency: String, toCurrency: String): String {
        val converted = convert(amount, fromCurrency, toCurrency)
        val symbol = CurrencyData.getSymbol(toCurrency)
        return String.format("≈ %s%.2f %s", symbol, converted, toCurrency)
    }

    fun observeDefaultCurrencyExpenses(): Flow<String> =
        userPreferencesDao.getPreferences()
            .map { it?.defaultCurrencyCodeExpenses ?: "USD" }

    fun observeDefaultCurrencyTracking(): Flow<String> =
        userPreferencesDao.getPreferences()
            .map { it?.defaultCurrencyCodeTracking ?: "USD" }


    fun observeDefaultCurrencyIncome(): Flow<String> =
        userPreferencesDao.getPreferences()
            .map { it?.defaultCurrencyCodeIncome ?: "USD" }

    fun getRecentCurrencies(): List<String> {
        val stored = sharedPreferences.getString(KEY_RECENT_CURRENCIES, "") ?: ""
        if (stored.isEmpty()) return emptyList()
        return stored.split(",").filter { it.isNotEmpty() }
    }

    fun addRecentCurrency(currencyCode: String) {
        val current = getRecentCurrencies().toMutableList()
        current.remove(currencyCode)
        current.add(0, currencyCode)
        val updated = current.take(MAX_RECENT_CURRENCIES)
        sharedPreferences.edit()
            .putString(KEY_RECENT_CURRENCIES, updated.joinToString(","))
            .apply()
    }
}
