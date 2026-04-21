package com.example.unimarketfrontend.network

import com.example.unimarketfrontend.network.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // Auth
    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest)

    @POST("api/v1/auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest)

    @POST("api/v1/auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest)

    // Users
    @GET("api/v1/users/me")
    suspend fun getMe(): User

    @GET("api/v1/users/{id}")
    suspend fun getUserById(@Path("id") userId: Int): Response<User>

    @GET("api/v1/users/me/wishlist")
    suspend fun getWishlist(): List<Listing>

    @GET("api/v1/users/me/purchases")
    suspend fun getPurchases(): UserWithPurchases

    @GET("api/v1/users/me/following")
    suspend fun getFollowing(): List<User>

    @DELETE("api/v1/users/me/wishlist/{listingId}")
    suspend fun removeFromWishlist(@Path("listingId") listingId: Int)

    // Listings
    @GET("api/v1/listings")
    suspend fun getListings(): Response<List<Listing>>

    @GET("api/v1/listings/my")
    suspend fun getMyListings(): Response<MyListingsResponse>

    @POST("api/v1/listings")
    suspend fun createListing(@Body request: CreateListingRequest): Response<Listing>

    @GET("api/v1/listings/home/ranking")
    suspend fun getHomeRanking(): HomeResponseDto

    @POST("api/v1/listings/{id}/images")
    suspend fun addListingImage(
        @Path("id") listingId: Int,
        @Body request: AddImageRequest
    ): Response<ListingImage>

    @GET("api/v1/messages/as-buyer")
    suspend fun getMessagesAsBuyer(): List<Message>

    @GET("api/v1/messages/thread/seller/{sellerId}")
    suspend fun getThread(@Path("sellerId") sellerId: Int): List<Message>

    @POST("api/v1/messages/buyer")
    suspend fun sendMessageAsBuyer(@Body request: SendMessageRequest): Message

    @PATCH("api/v1/messages/{id}/read")
    suspend fun markAsRead(@Path("id") messageId: Int)

    @POST("api/v1/uploads/cloudinary-signature")
    suspend fun getCloudinarySignature(@Body request: CloudinarySignatureRequest): Response<CloudinarySignatureResponse>

    @GET("api/v1/reviews/listing/{listingId}")
    suspend fun getReviewsByListing(@Path("listingId") listingId: Int): Response<List<Review>>

    @GET("api/v1/reviews/listing/{listingId}/average")
    suspend fun getAverageByListing(@Path("listingId") listingId: Int): Response<Map<String, Any?>>

    @DELETE("api/v1/users/{id}")
    suspend fun deleteAccount(@Path("id") userId: Int): Response<Unit>

    @DELETE("api/v1/reviews/{id}")
    suspend fun deleteReview(@Path("id") reviewId: Int): Response<Unit>

    @GET("api/v1/courses")
    suspend fun getCourses(): Response<List<Course>>
}