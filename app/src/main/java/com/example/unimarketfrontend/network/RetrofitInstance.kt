package com.example.unimarketfrontend.network
import com.example.unimarketfrontend.network.ApiService
import com.example.unimarketfrontend.network.AuthInterceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
/* Se encarga de la construcción de servicios, manejo de tokens y serialización de JSON.*/
object RetrofitInstance {
    private var jwtToken: String? = null

    fun setToken(token: String) {
        jwtToken = token
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor { jwtToken })
        .build()

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://group-42-backend.vercel.app/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}