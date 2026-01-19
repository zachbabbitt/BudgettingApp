package com.example.budgettingtogether

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class CurrencyRepository(context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val exchangeRateDao = database.exchangeRateDao()
    private val userPreferencesDao = database.userPreferencesDao()

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

    suspend fun getDefaultCurrency(): String {
        return userPreferencesDao.getPreferencesOnce()?.defaultCurrencyCode ?: "USD"
    }

    suspend fun setDefaultCurrency(currencyCode: String) {
        val prefs = userPreferencesDao.getPreferencesOnce() ?: UserPreferences()
        userPreferencesDao.savePreferences(prefs.copy(defaultCurrencyCode = currencyCode))
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
}
