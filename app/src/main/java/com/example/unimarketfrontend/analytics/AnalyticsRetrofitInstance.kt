
package com.example.unimarketfrontend.analytics

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object AnalyticsRetrofitInstance {
    private val client by lazy {
        OkHttpClient.Builder()
            .addInterceptor(AnalyticsAuthInterceptor())
            .build()
    }

    val api: AnalyticsApiService by lazy {
        Retrofit.Builder()
            .baseUrl(AnalyticsConfig.ANALYTICS_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AnalyticsApiService::class.java)
    }
}


