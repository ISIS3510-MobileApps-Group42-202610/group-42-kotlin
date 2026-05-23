package com.example.unimarketfrontend.model.network.api

import com.example.unimarketfrontend.model.auth.ForgotPasswordRequest
import com.example.unimarketfrontend.model.auth.RegisterRequest
import com.example.unimarketfrontend.model.auth.ResetPasswordRequest
import com.example.unimarketfrontend.model.course.CourseDto
import com.example.unimarketfrontend.model.listing.AddImageRequest
import com.example.unimarketfrontend.model.listing.CreateListingRequest
import com.example.unimarketfrontend.model.listing.HomeResponseDto
import com.example.unimarketfrontend.model.listing.Listing
import com.example.unimarketfrontend.model.listing.ListingImage
import com.example.unimarketfrontend.model.listing.MyListingsResponse
import com.example.unimarketfrontend.model.listing.Review
import com.example.unimarketfrontend.model.message.Message
import com.example.unimarketfrontend.model.message.SendMessageRequest
import com.example.unimarketfrontend.model.message.SendMessageAsSellerRequest
import com.example.unimarketfrontend.model.uploads.CloudinarySignatureRequest
import com.example.unimarketfrontend.model.uploads.CloudinarySignatureResponse
import com.example.unimarketfrontend.model.user.LoginRequest
import com.example.unimarketfrontend.model.user.LoginResponse
import com.example.unimarketfrontend.model.user.User
import com.example.unimarketfrontend.model.user.UserWithPurchases
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

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

    @POST("api/v1/users/me/wishlist/{listingId}")
    suspend fun addToWishlist(@Path("listingId") listingId: Int)

    @DELETE("api/v1/users/me/wishlist/{listingId}")
    suspend fun removeFromWishlist(@Path("listingId") listingId: Int)

    @GET("api/v1/users/me/purchases")
    suspend fun getPurchases(): UserWithPurchases

    @GET("api/v1/users/me/following")
    suspend fun getFollowing(): List<User>

    // Listings
    @GET("api/v1/listings")
    suspend fun getListings(): Response<List<Listing>>

    @GET("api/v1/courses")
    suspend fun getCourses(): Response<List<CourseDto>>

    @GET("api/v1/listings/{id}")
    suspend fun getListingById(@Path("id") listingId: Int): Response<Listing>

    @GET("api/v1/listings/my")
    suspend fun getMyListings(): Response<MyListingsResponse>

    @POST("api/v1/listings")
    suspend fun createListing(@Body request: CreateListingRequest): Response<Listing>

    @DELETE("api/v1/listings/{id}")
    suspend fun deleteListing(@Path("id") listingId: Int): Response<Unit>

    @GET("api/v1/listings/home/ranking")
    suspend fun getHomeRanking(): HomeResponseDto

    @POST("api/v1/listings/{id}/images")
    suspend fun addListingImage(
        @Path("id") listingId: Int,
        @Body request: AddImageRequest
    ): Response<ListingImage>

    // Messages
    @GET("api/v1/messages/as-buyer")
    suspend fun getMessagesAsBuyer(): List<Message>

    @GET("api/v1/messages/as-seller")
    suspend fun getMessagesAsSeller(): List<Message>

    @GET("api/v1/messages/thread/seller/{sellerId}")
    suspend fun getThread(@Path("sellerId") sellerId: Int): List<Message>

    @GET("api/v1/messages/thread/buyer/{buyerId}")
    suspend fun getThreadAsSeller(@Path("buyerId") buyerId: Int): List<Message>

    @POST("api/v1/messages/buyer")
    suspend fun sendMessageAsBuyer(@Body request: SendMessageRequest): Message

    @POST("api/v1/messages/seller")
    suspend fun sendMessageAsSeller(@Body request: SendMessageAsSellerRequest): Message

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
}
