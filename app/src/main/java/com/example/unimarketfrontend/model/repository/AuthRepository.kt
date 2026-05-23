package com.example.unimarketfrontend.model.repository

import com.example.unimarketfrontend.model.auth.ForgotPasswordRequest
import com.example.unimarketfrontend.model.auth.RegisterRequest
import com.example.unimarketfrontend.model.auth.ResetPasswordRequest
import com.example.unimarketfrontend.model.listing.Listing
import com.example.unimarketfrontend.model.network.client.RetrofitInstance
import com.example.unimarketfrontend.model.user.LoginRequest
import com.example.unimarketfrontend.model.user.LoginResponse
import com.example.unimarketfrontend.model.user.User
import com.example.unimarketfrontend.model.user.UserWithPurchases
import retrofit2.Response

class AuthRepository {
    private val api = RetrofitInstance.api

    suspend fun login(request: LoginRequest): LoginResponse = api.login(request)
    suspend fun register(request: RegisterRequest) = api.register(request)
    fun saveToken(token: String) = RetrofitInstance.setToken(token)
    fun logout() = RetrofitInstance.clearToken()
    suspend fun forgotPassword(email: String) = api.forgotPassword(ForgotPasswordRequest(email))
    suspend fun resetPassword(token: String, newPass: String) = api.resetPassword(ResetPasswordRequest(token, newPass))
    suspend fun getMyProfile(): User = api.getMe()
    suspend fun getWishlist(): List<Listing> = api.getWishlist()
    suspend fun getPurchases(): UserWithPurchases = api.getPurchases()
    suspend fun getFollowing(): List<User> = api.getFollowing()
    suspend fun removeFromWishlist(listingId: Int) = api.removeFromWishlist(listingId)
    suspend fun deleteAccount(userId: Int): Response<Unit> = api.deleteAccount(userId)
    suspend fun addToWishlist(listingId: Int) = api.addToWishlist(listingId)
}

