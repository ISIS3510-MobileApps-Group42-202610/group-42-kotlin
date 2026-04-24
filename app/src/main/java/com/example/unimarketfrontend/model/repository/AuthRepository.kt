package com.example.unimarketfrontend.model.repository

import com.example.unimarketfrontend.model.network.client.RetrofitInstance
import com.example.unimarketfrontend.model.auth.*
import com.example.unimarketfrontend.model.user.User
import com.example.unimarketfrontend.model.user.LoginRequest

class AuthRepository {
    private val api = RetrofitInstance.api


    suspend fun login(request: LoginRequest) = api.login(request)

    suspend fun register(request: RegisterRequest) = api.register(request)

    fun saveToken(token: String) = RetrofitInstance.setToken(token)

    fun logout() = RetrofitInstance.clearToken()

    suspend fun forgotPassword(email: String) = api.forgotPassword(ForgotPasswordRequest(email))

    suspend fun resetPassword(token: String, newPass: String) =
        api.resetPassword(ResetPasswordRequest(token, newPass))

    suspend fun getMyProfile(): User = api.getMe()

    suspend fun getWishlist() = api.getWishlist()

    suspend fun getPurchases() = api.getPurchases()

    suspend fun getFollowing() = api.getFollowing()

    suspend fun removeFromWishlist(id: Int) = api.removeFromWishlist(id)

    suspend fun deleteAccount(userId: Int) = api.deleteAccount(userId)
}