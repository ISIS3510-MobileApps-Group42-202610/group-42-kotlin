package com.example.unimarketfrontend.repository

import com.example.unimarketfrontend.local.dao.ListingDao
import com.example.unimarketfrontend.local.entity.toEntity
import com.example.unimarketfrontend.local.entity.toListing
import com.example.unimarketfrontend.network.ApiService
import com.example.unimarketfrontend.network.RetrofitInstance
import com.example.unimarketfrontend.network.model.Listing

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
}
