package com.example.unimarketfrontend.network
import com.example.unimarketfrontend.network.model.ForgotPasswordRequest
import com.example.unimarketfrontend.network.model.LoginResponse
import com.example.unimarketfrontend.network.model.LoginRequest
import com.example.unimarketfrontend.network.model.Listing
import com.example.unimarketfrontend.network.model.Purchase
import com.example.unimarketfrontend.network.model.RegisterRequest
import com.example.unimarketfrontend.network.model.ResetPasswordRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import com.example.unimarketfrontend.network.model.User
import com.example.unimarketfrontend.network.model.UserWithPurchases
import retrofit2.http.DELETE
import retrofit2.http.Path


interface ApiService {
    @POST("api/v1/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): LoginResponse

    @POST("api/v1/auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    )

    @POST("api/v1/auth/forgot-password")
    suspend fun forgotPassword(
        @Body request: ForgotPasswordRequest
    )

    @POST("api/v1/auth/reset-password")
    suspend fun resetPassword(
        @Body request: ResetPasswordRequest
    )

    @GET("api/v1/listings")
    suspend fun getListings(): List<Listing>

    @GET("api/v1/users/me")
    suspend fun getMe(): User

    @GET("api/v1/users/me/wishlist")
    suspend fun getWishlist(): List<Listing>

    @GET("api/v1/users/me/purchases")
    suspend fun getPurchases(): UserWithPurchases

    @GET("api/v1/users/me/following")
    suspend fun getFollowing(): List<User>

    @DELETE("api/v1/users/me/wishlist/{listingId}")
    suspend fun removeFromWishlist(@Path("listingId") listingId: Int)

}
