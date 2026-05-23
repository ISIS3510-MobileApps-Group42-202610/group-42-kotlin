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
 */
class WishlistRepository(context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val wishlistDao = db.wishlistDao()
    private val pendingDao = db.pendingWishlistDao()
    private val api = RetrofitInstance.api

    private val CACHE_TTL_MS = 5 * 60 * 1000L

    /** Observe all wishlist items reactively from Room */
    fun observeWishlist(): Flow<List<WishlistEntity>> = wishlistDao.observeWishlist()

    /** Check if cache is stale (older than 5 min) */
    suspend fun isCacheStale(): Boolean {
        val oldest = wishlistDao.getOldestCachedAt() ?: return true
        return (System.currentTimeMillis() - oldest) > CACHE_TTL_MS
    }

    /** Check if a specific listing is in favorites (Room source of truth) */
    suspend fun isInWishlist(listingId: Int): Boolean {
        return wishlistDao.isInWishlist(listingId) > 0
    }

    /** Sync local cache with server */
    suspend fun refreshFromNetwork(): Boolean {
        return try {
            val remoteWishlist = api.getWishlist()
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
            false
        }
    }

    /** Add to wishlist with Optimistic Update and Eventual Connectivity */
    suspend fun addToWishlist(listing: Listing): Result<Unit> {
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
                Log.d("WishlistRepo", "Successfully added listing ${listing.id} to server")
            } else {
                Log.d("WishlistRepo", "Offline: queueing ADD action for listing ${listing.id}")
                pendingDao.insert(PendingWishlistEntity(listingId = listing.id, action = "ADD"))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("WishlistRepo", "Server sync failed for listing ${listing.id}, queueing: ${e.message}")
            pendingDao.insert(PendingWishlistEntity(listingId = listing.id, action = "ADD"))
            Result.success(Unit) // Guaranteed eventually
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
                Log.d("WishlistRepo", "Successfully removed listing $listingId from server")
            } else {
                Log.d("WishlistRepo", "Offline: queueing REMOVE action for listing $listingId")
                pendingDao.insert(PendingWishlistEntity(listingId = listingId, action = "REMOVE"))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("WishlistRepo", "Server sync failed for listing $listingId, queueing: ${e.message}")
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
                Log.e("WishlistRepo", "Retry failed for listing ${action.listingId}: ${e.message}")
            }
        }
        return count
    }
}
