package com.example.unimarketfrontend.network

import com.example.unimarketfrontend.network.model.HomeRankingResponse
import com.example.unimarketfrontend.network.model.Listing
import com.example.unimarketfrontend.network.model.LoginRequest
import com.example.unimarketfrontend.network.model.LoginResponse
import com.example.unimarketfrontend.network.model.Message
import com.example.unimarketfrontend.network.model.SendMessageRequest
import com.example.unimarketfrontend.network.model.User
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {

    // Auth
    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    // Users
    @GET("api/v1/users/me")
    suspend fun getMe(): User

    // Listings
    @GET("api/v1/listings")
    suspend fun getListings(): List<Listing>

    @GET("api/v1/listings/home/ranking")
    suspend fun getHomeRanking(): HomeRankingResponse

    // Messages - as buyer
    @GET("api/v1/messages/as-buyer")
    suspend fun getMessagesAsBuyer(): List<Message>

    // Thread with a specific seller
    @GET("api/v1/messages/thread/seller/{sellerId}")
    suspend fun getThread(@Path("sellerId") sellerId: Int): List<Message>

    // Send message as buyer
    @POST("api/v1/messages/buyer")
    suspend fun sendMessageAsBuyer(@Body request: SendMessageRequest): Message

    // Mark a message as read
    @PATCH("api/v1/messages/{id}/read")
    suspend fun markAsRead(@Path("id") messageId: Int)
}