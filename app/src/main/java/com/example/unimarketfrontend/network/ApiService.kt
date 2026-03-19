package com.example.unimarketfrontend.network

import com.example.unimarketfrontend.network.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

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

    @POST("api/v1/uploads/cloudinary-signature")
    suspend fun getCloudinarySignature(
        @Body request: CloudinarySignatureRequest
    ): Response<CloudinarySignatureResponse>

    @POST("api/v1/listings/{id}/images")
    suspend fun addListingImage(
        @Path("id") listingId: Int,
        @Body request: AddImageRequest
    ): Response<ListingImage>

    @GET("api/v1/users/{id}")
    suspend fun getUserById(
        @Path("id") userId: Int
    ): Response<User>

    @GET("api/v1/reviews/listing/{listingId}")
    suspend fun getReviewsByListing(
        @Path("listingId") listingId: Int
    ): Response<List<Review>>

    @GET("api/v1/reviews/listing/{listingId}/average")
    suspend fun getAverageByListing(
        @Path("listingId") listingId: Int
    ): Response<Map<String, Any?>>
}
