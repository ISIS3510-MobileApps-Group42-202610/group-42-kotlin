package com.example.unimarketfrontend.network
import com.example.unimarketfrontend.network.model.LoginResponse
import com.example.unimarketfrontend.network.model.LoginRequest
import com.example.unimarketfrontend.network.model.Listing
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import com.example.unimarketfrontend.network.model.User


interface ApiService {
    @POST("api/v1/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): LoginResponse

    @GET("api/v1/listings")
    suspend fun getListings(): List<Listing>

    @GET("api/v1/users/me")
    suspend fun getMe(): User
}
