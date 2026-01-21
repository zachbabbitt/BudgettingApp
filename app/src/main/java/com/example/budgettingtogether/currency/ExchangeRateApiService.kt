package com.example.budgettingtogether.currency

import retrofit2.http.GET

data class ExchangeRateResponse(
    val result: String,
    val base_code: String,
    val time_last_update_utc: String,
    val rates: Map<String, Double>
)

interface ExchangeRateApiService {
    @GET("v6/latest/USD")
    suspend fun getLatestRates(): ExchangeRateResponse

    companion object {
        const val BASE_URL = "https://open.er-api.com/"
    }
}
