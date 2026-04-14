package com.example.unimarketfrontend.analytics

import okhttp3.Interceptor
import okhttp3.Response


// Intercepta las solicitudes HTTP y agrega el encabezado de autenticación
//centraliza seguridad dentro de la app para que solo se pida una vez el token jwt
//
class AnalyticsAuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val builder = request.newBuilder()

        when (AnalyticsConfig.authMode) {
            AnalyticsAuthMode.Token -> {
                val token = AnalyticsConfig.authToken
                if (!token.isNullOrBlank()) {
                    builder.addHeader("Authorization", "Token $token")
                }
            }

            AnalyticsAuthMode.Bearer -> {
                val token = AnalyticsConfig.authToken
                if (!token.isNullOrBlank()) {
                    builder.addHeader("Authorization", "Bearer $token")
                }
            }

            AnalyticsAuthMode.ApiKey -> {
                val key = AnalyticsConfig.apiKey
                if (!key.isNullOrBlank()) {
                    builder.addHeader("X-API-Key", key)
                }
            }

            AnalyticsAuthMode.None -> Unit
        }

        return chain.proceed(builder.build())
    }
}

