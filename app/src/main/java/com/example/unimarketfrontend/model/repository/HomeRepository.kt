package com.example.unimarketfrontend.model.repository

import android.util.LruCache
import com.example.unimarketfrontend.model.local.dao.ListingDao
import com.example.unimarketfrontend.model.mappers.toEntities
import com.example.unimarketfrontend.model.mappers.toListing
import com.example.unimarketfrontend.model.network.api.ApiService
import com.example.unimarketfrontend.model.network.client.RetrofitInstance
import com.example.unimarketfrontend.model.listing.HomeResponseDto
import com.example.unimarketfrontend.model.listing.Listing

class HomeRepository(
    private val api: ApiService = RetrofitInstance.api,
    private val listingDao: ListingDao
) {
    private val listingMemoryCache = LruCache<Int, Listing>(MEMORY_CACHE_MAX_LISTINGS)
    private var memoryCacheUpdatedAtMs: Long = 0L

    private fun isMemoryCacheValid(nowMs: Long = System.currentTimeMillis()): Boolean {
        return memoryCacheUpdatedAtMs > 0L && nowMs - memoryCacheUpdatedAtMs <= MEMORY_CACHE_TTL_MS
    }

    private fun clearMemoryCache() {
        listingMemoryCache.evictAll()
        memoryCacheUpdatedAtMs = 0L
    }

    private fun cacheListings(listings: List<Listing>) {
        if (listings.isEmpty()) return
        synchronized(listingMemoryCache) {
            listings.forEach { listingMemoryCache.put(it.id, it) }
            memoryCacheUpdatedAtMs = System.currentTimeMillis()
        }
    }

    private fun getMemoryListings(): List<Listing> {
        if (!isMemoryCacheValid()) {
            clearMemoryCache()
            return emptyList()
        }
        synchronized(listingMemoryCache) {
            return listingMemoryCache.snapshot().values.toList()
        }
    }

    private suspend fun preserveLocalInactiveState(remoteListing: Listing): Listing {
        val cached = listingDao.getById(remoteListing.id)
        return if (cached != null && !cached.active) {
            remoteListing.copy(active = false)
        } else {
            remoteListing
        }
    }

    suspend fun getCachedActiveListings(): List<Listing> {
        val memoryListings = getMemoryListings().filter { it.active }
        if (memoryListings.isNotEmpty()) return memoryListings

        val localListings = listingDao.getActive().map { it.toListing() }
        cacheListings(localListings)
        return localListings
    }

    suspend fun refreshHomeData(): HomeResponseDto {
        val remote = api.getHomeRanking()
        val allRemoteListings = (remote.trending + remote.recent).distinctBy { it.id }
        val mergedListings = allRemoteListings.map { preserveLocalInactiveState(it) }

        if (mergedListings.isNotEmpty()) {
            listingDao.upsertAll(mergedListings.toEntities())
            cacheListings(mergedListings)
        }

        return remote.copy(
            trending = remote.trending.map { preserveLocalInactiveState(it) },
            recent = remote.recent.map { preserveLocalInactiveState(it) }
        )
    }

    private companion object {
        private const val MEMORY_CACHE_MAX_LISTINGS = 120
        private const val MEMORY_CACHE_TTL_MS = 5 * 60 * 1000L
    }
}