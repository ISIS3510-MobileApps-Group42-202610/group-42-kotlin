package com.example.unimarketfrontend.network

import okhttp3.Interceptor
import okhttp3.Response
/**
 * Implementación de un [Interceptor] de OkHttp para la gestión centralizada
 * de la autenticación mediante tokens Bearer.
 */
class AuthInterceptor(
    private val tokenProvider: () -> String?
) : Interceptor {
/* Se hace la intercepción de la petición HTTP para poner las credenciales de seguridad en el header de la petición*/
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = tokenProvider()
        val newRequest = if (token != null) {
            request.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            request
        }
        return chain.proceed(newRequest)
    }
}