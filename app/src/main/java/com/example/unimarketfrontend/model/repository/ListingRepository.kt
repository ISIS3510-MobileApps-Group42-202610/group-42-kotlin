package com.example.unimarketfrontend.model.repository

import com.example.unimarketfrontend.model.local.dao.ListingDao
import com.example.unimarketfrontend.model.mappers.toEntities
import com.example.unimarketfrontend.model.mappers.toEntity
import com.example.unimarketfrontend.model.mappers.toListing
import com.example.unimarketfrontend.model.network.api.ApiService
import com.example.unimarketfrontend.model.network.client.RetrofitInstance
import com.example.unimarketfrontend.model.listing.*
import com.example.unimarketfrontend.model.message.SendMessageRequest
import com.example.unimarketfrontend.model.uploads.CloudinarySignatureRequest

// Clase para manejar el estado dual (Local y Red) de un producto específico
data class ListingCacheThenNetworkResult(
    val cached: Listing?,
    val remote: Listing?,
    val networkError: Throwable?
)

class ListingRepository(
    private val api: ApiService = RetrofitInstance.api,
    private val listingDao: ListingDao
) {

    suspend fun getCachedById(listingId: Int): Listing? {
        return listingDao.getById(listingId)?.toListing()
    }

    suspend fun refreshById(listingId: Int): Listing? {
        val response = api.getListings()
        if (!response.isSuccessful || response.body() == null) {
            throw IllegalStateException("No se pudo cargar el listing")
        }

        val remoteListing = response.body()?.firstOrNull { it.id == listingId } ?: return null

        listingDao.upsert(remoteListing.toEntity())
        return remoteListing
    }

    suspend fun getByIdCacheThenNetwork(listingId: Int): ListingCacheThenNetworkResult {
        val cached = getCachedById(listingId)
        return try {
            val remote = refreshById(listingId)
            ListingCacheThenNetworkResult(
                cached = cached,
                remote = remote,
                networkError = null
            )
        } catch (e: Exception) {
            ListingCacheThenNetworkResult(
                cached = cached,
                remote = null,
                networkError = e
            )
        }
    }


    suspend fun refreshHomeData(): HomeResponseDto {
        val remote = api.getHomeRanking()
        val allRemoteListings = (remote.trending + remote.recent).distinctBy { it.id }

        if (allRemoteListings.isNotEmpty()) {
            listingDao.upsertAll(allRemoteListings.toEntities())
        }
        return remote
    }

    suspend fun getCachedActiveListings(): List<Listing> {
        return listingDao.getActive().map { it.toListing() }
    }

    suspend fun searchListings() = api.getListings()

    suspend fun getSellerInfo(sellerId: Int) = api.getUserById(sellerId)

    suspend fun getReviews(listingId: Int) = api.getReviewsByListing(listingId)

    suspend fun sendMessage(sellerId: Int, content: String) = api.sendMessageAsBuyer(SendMessageRequest(seller_id = sellerId, content = content))

    suspend fun getMyListings() = api.getMyListings()

    suspend fun createListing(request: CreateListingRequest) = api.createListing(request)

    suspend fun addListingImage(listingId: Int, request: AddImageRequest) =
        api.addListingImage(listingId, request)

    suspend fun getCloudinarySignature(request: CloudinarySignatureRequest) =
        api.getCloudinarySignature(request)
}