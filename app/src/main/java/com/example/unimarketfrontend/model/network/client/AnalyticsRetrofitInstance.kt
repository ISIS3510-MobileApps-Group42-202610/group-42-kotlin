package com.example.unimarketfrontend.model.network.client

import com.example.unimarketfrontend.model.network.api.AnalyticsApiService
import com.example.unimarketfrontend.model.network.interceptor.AnalyticsAuthInterceptor
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okio.Buffer
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object AnalyticsRetrofitInstance {
    private const val ANALYTICS_BASE_URL = "https://group-42-analytic-engine-back.vercel.app/"

    private val missingEventTypeFixer = Interceptor { chain ->
        val request = chain.request()
        val path = request.url.encodedPath

        if (path.contains("/api/performance") && request.method == "POST") {
            val body = request.body
            if (body != null) {
                val buffer = Buffer()
                body.writeTo(buffer)
                val bodyStr = buffer.readUtf8()

                if (bodyStr.startsWith("{") && !bodyStr.contains("\"event_type\"")) {
                    val newBodyStr = bodyStr.replaceFirst("{", "{\"event_type\":\"performance_metric\",")
                    val newBody = newBodyStr.toRequestBody(body.contentType())
                    return@Interceptor chain.proceed(request.newBuilder().post(newBody).build())
                }
            }
        }
        chain.proceed(request)
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(AnalyticsAuthInterceptor())
        .addInterceptor(missingEventTypeFixer)
        .build()

    val api: AnalyticsApiService by lazy {
        Retrofit.Builder()
            .baseUrl(ANALYTICS_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AnalyticsApiService::class.java)
    }
}