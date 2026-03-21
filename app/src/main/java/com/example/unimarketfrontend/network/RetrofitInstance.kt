package com.example.unimarketfrontend.network

import android.content.Context
import android.content.SharedPreferences
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
    }

    private var jwtToken: String? = null
        get() = field ?: prefs?.getString("token", null)

    fun setToken(token: String) {
        jwtToken = token
        prefs?.edit()?.putString("token", token)?.apply()
    }

    fun clearToken() {
        jwtToken = null
        prefs?.edit()?.remove("token")?.apply()
    }

    fun isLoggedIn(): Boolean = prefs?.getString("token", null) != null

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