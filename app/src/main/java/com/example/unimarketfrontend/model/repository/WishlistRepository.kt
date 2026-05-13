package com.example.unimarketfrontend.model.repository

import android.content.Context
import com.example.unimarketfrontend.model.local.AppDatabase
import com.example.unimarketfrontend.model.local.WishlistEntity
import com.example.unimarketfrontend.model.listing.Listing
import com.example.unimarketfrontend.model.network.client.RetrofitInstance
import kotlinx.coroutines.flow.Flow
import com.example.unimarketfrontend.model.local.dao.WishlistDao


/**
 * SPRINT 4 — WishlistRepository
 *
 * Implements Cache-Then-Network strategy:
 * 1. Always return cached wishlist immediately (offline-first)
 * 2. Refresh from network in background when online
 * 3. If network fails, cached data remains visible (no Lost Content antipattern)
 *
 * Eventual Connectivity:
 * - Add/remove operations are attempted immediately when online
 * - If offline, they fail gracefully with a clear error message
 * - The wishlist is always readable from cache
 *
 * Multi-threading:
 * - All operations are suspend functions running on IO dispatcher via coroutines
 * - observeWishlist() returns a Flow for reactive UI updates (Observer pattern)
 */
class WishlistRepository(context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val wishlistDao = db.wishlistDao()
    private val api = RetrofitInstance.api

    // TTL: 5 minutes — same policy as conversations cache
    private val CACHE_TTL_MS = 5 * 60 * 1000L

    /** Reactive stream — UI observes this and updates automatically */
    fun observeWishlist(): Flow<List<WishlistEntity>> = wishlistDao.observeWishlist()

    /** Check if cache is stale (older than TTL or empty) */
    suspend fun isCacheStale(): Boolean {
        val oldest = wishlistDao.getOldestCachedAt() ?: return true
        return (System.currentTimeMillis() - oldest) > CACHE_TTL_MS
    }

    /** Check if a listing is in the wishlist (from local cache) */
    suspend fun isInWishlist(listingId: Int): Boolean {
        return wishlistDao.isInWishlist(listingId) > 0
    }

    /**
     * Refresh wishlist from network and update local cache.
     * Returns true on success, false on failure (cache remains intact).
     */
    suspend fun refreshFromNetwork(): Boolean {
        return try {
            val remoteWishlist: List<Listing> = api.getWishlist()
            val entities = remoteWishlist.map { listing ->
                WishlistEntity(
                    listingId = listing.id,
                    title = listing.title,
                    sellingPrice = listing.selling_price,
                    condition = listing.condition,
                    category = listing.category,
                    imageUrl = listing.images?.firstOrNull()?.url,
                    active = listing.active,
                    sellerId = listing.seller_id,
                    cachedAt = System.currentTimeMillis()
                )
            }
            wishlistDao.clearAll()
            wishlistDao.insertAll(entities)
            true
        } catch (e: Exception) {
            android.util.Log.e("WishlistRepository", "Failed to refresh wishlist: ${e.message}")
            false
        }
    }

    /**
     * Add a listing to wishlist.
     * Optimistic update: inserts locally first, then syncs with backend.
     * If backend fails, removes from local cache to maintain consistency.
     */
    suspend fun addToWishlist(listing: Listing): Result<Unit> {
        // Optimistic local insert
        val entity = WishlistEntity(
            listingId = listing.id,
            title = listing.title,
            sellingPrice = listing.selling_price,
            condition = listing.condition,
            category = listing.category,
            imageUrl = listing.images?.firstOrNull()?.url,
            active = listing.active,
            sellerId = listing.seller_id
        )
        wishlistDao.insert(entity)

        return try {
            api.addToWishlist(listing.id)
            Result.success(Unit)
        } catch (e: Exception) {
            // Rollback optimistic insert on failure
            wishlistDao.delete(listing.id)
            Result.failure(e)
        }
    }

    /**
     * Remove a listing from wishlist.
     * Removes locally first, then syncs with backend.
     */
    suspend fun removeFromWishlist(listingId: Int): Result<Unit> {
        // Remove locally first
        wishlistDao.delete(listingId)

        return try {
            api.removeFromWishlist(listingId)
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("WishlistRepository", "Failed to remove from backend: ${e.message}")
            // Local removal stays — will sync on next refresh
            Result.failure(e)
        }
    }
}
