package com.example.unimarketfrontend.network
import com.example.unimarketfrontend.network.model.LoginResponse
import com.example.unimarketfrontend.network.model.LoginRequest
import com.example.unimarketfrontend.network.model.Listing
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import com.example.unimarketfrontend.network.model.User
import com.example.unimarketfrontend.network.model.HomeResponseDto
import retrofit2.Response
import com.example.unimarketfrontend.network.model.*


interface ApiService {
    @POST("api/v1/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): LoginResponse

    @GET("api/v1/listings")
    suspend fun getListings(): Response<List<Listing>>
    @GET("api/v1/listings/my")
    suspend fun getMyListings(): Response<MyListingsResponse>
    @POST("api/v1/listings")
    suspend fun createListing(
        @Body request: CreateListingRequest
    ): Response<Listing>
    @GET("api/v1/users/me")
    suspend fun getMe(): User
    @GET("api/v1/listings/home/ranking")
    suspend fun getHomeRanking(): HomeResponseDto
}
