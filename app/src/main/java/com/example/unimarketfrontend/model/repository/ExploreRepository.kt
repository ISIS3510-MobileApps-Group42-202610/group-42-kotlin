package com.example.unimarketfrontend.model.repository

import android.util.LruCache
import com.example.unimarketfrontend.model.listing.Listing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

data class ExplorePayload(
    val listings: List<Listing>,
    val coursesResult: CourseResult,
    val wishlistIds: Set<Int>
)

class ExploreRepository(
    private val listingRepository: ListingRepository,
    private val courseRepository: CourseRepository,
    private val authRepository: AuthRepository
) {
    private var payloadMemoryCache: ExplorePayload? = null
    private var payloadCachedAtMs: Long = 0L
    private val listingByIdMemoryCache = LruCache<Int, Listing>(MAX_LISTING_CACHE_SIZE)

    private fun isPayloadCacheValid(nowMs: Long = System.currentTimeMillis()): Boolean {
        return payloadMemoryCache != null && nowMs - payloadCachedAtMs <= PAYLOAD_CACHE_TTL_MS
    }

    private fun cachePayload(payload: ExplorePayload) {
        payloadMemoryCache = payload
        payloadCachedAtMs = System.currentTimeMillis()
        synchronized(listingByIdMemoryCache) {
            listingByIdMemoryCache.evictAll()
            payload.listings.forEach { listingByIdMemoryCache.put(it.id, it) }
        }
    }

    fun getListingFromMemory(listingId: Int): Listing? {
        synchronized(listingByIdMemoryCache) {
            return listingByIdMemoryCache.get(listingId)
        }
    }

    suspend fun loadExplorePayload(): ExplorePayload = coroutineScope {
        if (isPayloadCacheValid()) {
            return@coroutineScope payloadMemoryCache!!
        }

        val listingsDeferred = async(Dispatchers.IO) {
            runCatching { listingRepository.syncSearchListings() }
                .getOrElse { listingRepository.getCachedActiveListings() }
        }
        val coursesDeferred = async(Dispatchers.IO) { courseRepository.getCourses() }
        val wishlistDeferred = async(Dispatchers.IO) {
            runCatching { authRepository.getWishlist().map { it.id }.toSet() }
                .getOrDefault(emptySet())
        }

        val payload = ExplorePayload(
            listings = listingsDeferred.await(),
            coursesResult = coursesDeferred.await(),
            wishlistIds = wishlistDeferred.await()
        )
        cachePayload(payload)
        payload
    }

    suspend fun setWishlistStatus(listingId: Int, shouldBeWishlisted: Boolean) {
        if (shouldBeWishlisted) {
            authRepository.addToWishlist(listingId)
        } else {
            authRepository.removeFromWishlist(listingId)
        }
    }

    companion object {
        private const val PAYLOAD_CACHE_TTL_MS = 30_000L
        private const val MAX_LISTING_CACHE_SIZE = 200
    }
}
