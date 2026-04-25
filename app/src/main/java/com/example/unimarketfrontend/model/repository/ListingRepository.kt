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
import com.example.unimarketfrontend.model.user.User
import retrofit2.Response

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

    private suspend fun preserveLocalInactiveState(remoteListing: Listing): Listing {
        val cached = listingDao.getById(remoteListing.id)
        return if (cached != null && !cached.active) {
            remoteListing.copy(active = false)
        } else {
            remoteListing
        }
    }

    suspend fun cacheRemoteListings(listings: List<Listing>) {
        val mergedListings = listings.map { preserveLocalInactiveState(it) }
        if (mergedListings.isNotEmpty()) {
            listingDao.upsertAll(mergedListings.toEntities())
        }
    }

    suspend fun getCachedById(listingId: Int): Listing? {
        return listingDao.getById(listingId)?.toListing()
    }

    suspend fun refreshById(listingId: Int): Listing? {
        // Primero intentamos con el endpoint específico (más eficiente)
        return runCatching {
            val response = api.getListingById(listingId)
            if (response.isSuccessful && response.body() != null) {
                val remoteListing = response.body()!!
                val mergedListing = preserveLocalInactiveState(remoteListing)
                listingDao.upsert(mergedListing.toEntity())
                return@runCatching mergedListing
            }
            null
        }.getOrNull() ?: run {
            // Fallback: traer todos los listings e intentar filtrar (menos eficiente pero funciona)
            val response = api.getListings()
            if (!response.isSuccessful || response.body() == null) {
                throw IllegalStateException("Could not load listing")
            }

            val remoteListing = response.body()?.firstOrNull { it.id == listingId } ?: return null
            val mergedListing = preserveLocalInactiveState(remoteListing)

            listingDao.upsert(mergedListing.toEntity())
            return mergedListing
        }
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
        val mergedListings = allRemoteListings.map { preserveLocalInactiveState(it) }

        if (mergedListings.isNotEmpty()) {
            listingDao.upsertAll(mergedListings.toEntities())
        }
        return remote.copy(
            trending = remote.trending.map { preserveLocalInactiveState(it) },
            recent = remote.recent.map { preserveLocalInactiveState(it) }
        )
    }

    suspend fun getCachedActiveListings(): List<Listing> {
        return listingDao.getActive().map { it.toListing() }
    }

    suspend fun getCachedSoldListings(): List<Listing> {
        return listingDao.getAll().filter { !it.active }.map { it.toListing() }
    }

    suspend fun searchListings() = api.getListings()

    suspend fun syncSearchListings(): List<Listing> {
        val response = api.getListings()
        val body = response.body().orEmpty()
        if (response.isSuccessful && body.isNotEmpty()) {
            cacheRemoteListings(body)
        }
        return getCachedActiveListings()
    }

    suspend fun getSellerInfo(userId: Int): Response<User> = api.getUserById(userId)

    suspend fun getMe(): User = api.getMe()

    suspend fun getReviews(listingId: Int): Response<List<Review>> = api.getReviewsByListing(listingId)

    suspend fun getMyListings() = api.getMyListings()

    suspend fun cacheMyListings(listings: MyListingsResponse) {
        cacheRemoteListings(listings.active + listings.sold)
    }

    suspend fun markAsSoldLocally(listingId: Int): Listing? {
        val cachedListing = listingDao.getById(listingId)?.toListing() ?: return null
        val soldListing = cachedListing.copy(active = false)
        listingDao.upsert(soldListing.toEntity())
        return soldListing
    }

    suspend fun createListing(request: CreateListingRequest) = api.createListing(request)

    suspend fun deleteListing(listingId: Int): Response<Unit> = api.deleteListing(listingId)

    suspend fun addListingImage(listingId: Int, request: AddImageRequest) =
        api.addListingImage(listingId, request)

    suspend fun getCloudinarySignature(request: CloudinarySignatureRequest) =
        api.getCloudinarySignature(request)

    suspend fun sendMessage(request: SendMessageRequest) = api.sendMessageAsBuyer(request)
}