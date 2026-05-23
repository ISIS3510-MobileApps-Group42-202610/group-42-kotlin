package com.example.unimarketfrontend.model.repository

import android.content.Context
import android.util.Log
import com.example.unimarketfrontend.model.local.AppDatabase
import com.example.unimarketfrontend.model.local.WishlistEntity
import com.example.unimarketfrontend.model.local.PendingWishlistEntity
import com.example.unimarketfrontend.model.listing.Listing
import com.example.unimarketfrontend.model.network.client.RetrofitInstance
import com.example.unimarketfrontend.model.utils.ConnectivityMonitor
import kotlinx.coroutines.flow.Flow

/**
 * SPRINT 4 — WishlistRepository (Esteban Hernandez)
 * 
 * Implements Senior Offline-First architecture:
 * 1. Cache-Then-Network: Immediate local display, background remote sync.
 * 2. Eventual Connectivity: Queue write actions (ADD/REMOVE) when offline.
 * 3. Reactive UI: Uses Room Flow to update all screens automatically.
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
    private val pendingDao = db.pendingWishlistDao()
    private val api = RetrofitInstance.api

    private val CACHE_TTL_MS = 5 * 60 * 1000L

    /** Observe all wishlist items reactively */
    fun observeWishlist(): Flow<List<WishlistEntity>> = wishlistDao.observeWishlist()

    /** Check if cache is stale (older than 5 min) */
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

    /** Check if a specific listing is in favorites (Room source of truth) */
    /** Check if a listing is in the wishlist (from local cache) */
    suspend fun isInWishlist(listingId: Int): Boolean {
        return wishlistDao.isInWishlist(listingId) > 0
    }

    /** Sync local cache with server */
    suspend fun refreshFromNetwork(): Boolean {
        return try {
            val remoteWishlist = api.getWishlist()
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
            Log.e("WishlistRepo", "Network sync failed: ${e.message}")
            android.util.Log.e("WishlistRepository", "Failed to refresh wishlist: ${e.message}")
            false
        }
    }

    /** Add to wishlist with Optimistic Update and Eventual Connectivity */
    suspend fun addToWishlist(listing: Listing): Result<Unit> {
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
        
        // 1. Local update (0ms latency for UI)
        wishlistDao.insert(entity)

        // 2. Try remote sync or queue
        return try {
            if (ConnectivityMonitor.isOnline.value) {
                api.addToWishlist(listing.id)
            } else {
                pendingDao.insert(PendingWishlistEntity(listingId = listing.id, action = "ADD"))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            pendingDao.insert(PendingWishlistEntity(listingId = listing.id, action = "ADD"))
            Result.success(Unit) // We return success because it's guaranteed eventually
        }
    }

    /** Remove from wishlist with Optimistic Update */
    suspend fun removeFromWishlist(listingId: Int): Result<Unit> {
        // 1. Local update
        wishlistDao.delete(listingId)

        // 2. Remote sync or queue
        return try {
            if (ConnectivityMonitor.isOnline.value) {
                api.removeFromWishlist(listingId)
            } else {
                pendingDao.insert(PendingWishlistEntity(listingId = listingId, action = "REMOVE"))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            pendingDao.insert(PendingWishlistEntity(listingId = listingId, action = "REMOVE"))
            Result.success(Unit)
        }
    }

    /** Process queued actions when connectivity returns */
    suspend fun retryPendingActions(): Int {
        val pending = pendingDao.getAllPending()
        var count = 0
        for (action in pending) {
            try {
                if (action.action == "ADD") {
                    api.addToWishlist(action.listingId)
                } else {
                    api.removeFromWishlist(action.listingId)
                }
                pendingDao.deleteById(action.id)
                count++
            } catch (e: Exception) {
                Log.e("WishlistRepo", "Retry failed for ${action.listingId}: ${e.message}")
            }
        }
        return count
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
