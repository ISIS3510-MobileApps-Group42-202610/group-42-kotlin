package com.example.unimarketfrontend.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarketfrontend.model.local.WishlistEntity
import com.example.unimarketfrontend.model.listing.Listing
import com.example.unimarketfrontend.model.repository.BusinessAnalyticsProvider
import com.example.unimarketfrontend.model.repository.WishlistRepository
import com.example.unimarketfrontend.model.utils.ConnectivityMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * SPRINT 4 — WishlistViewModel
 *
 * Architecture: MVVM with Repository pattern
 * Multi-threading: viewModelScope.launch with coroutines on IO dispatcher
 * Caching: Cache-then-network via WishlistRepository
 * Eventual Connectivity: Shows cached data offline, refreshes when online
 * Observer pattern: StateFlow + collectAsState() in Compose
 */
class WishlistViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WishlistRepository(application)

    /** Reactive wishlist from Room — updates automatically when DB changes */
    val wishlist: StateFlow<List<WishlistEntity>> = repository.observeWishlist()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val isOffline: StateFlow<Boolean> = ConnectivityMonitor.isOnline
        .map { !it }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    init {
        // Observe connectivity — refresh when coming back online
        observeConnectivity()
        // Initial load
        loadWishlist()
    }

    private fun observeConnectivity() {
        viewModelScope.launch {
            ConnectivityMonitor.isOnline.collectLatest { online ->
                if (online) {
                    // Refresh from network when connectivity returns
                    repository.refreshFromNetwork()
                }
            }
        }
    }

    fun loadWishlist() {
        viewModelScope.launch {
            _isRefreshing.value = true
            // Cache-then-network: Room Flow already shows cached data immediately.
            // We only hit the network if cache is stale or empty.
            if (repository.isCacheStale() || wishlist.value.isEmpty()) {
                repository.refreshFromNetwork()
            }
            _isRefreshing.value = false
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.refreshFromNetwork()
            _isRefreshing.value = false
        }
    }

    fun addToWishlist(listing: Listing) {
        viewModelScope.launch {
            val result = repository.addToWishlist(listing)
            if (result.isSuccess) {
                // Track BQ7 analytics event
                BusinessAnalyticsProvider.tracker.trackWishlistItemAdded(
                    listingId = listing.id,
                    sellerId = listing.seller_id,
                    metadata = mapOf(
                        "category" to (listing.category ?: "unknown"),
                        "price" to listing.selling_price.toString()
                    )
                )
            } else {
                _errorMessage.value = "Could not add to wishlist. Please try again."
            }
        }
    }

    fun removeFromWishlist(listingId: Int) {
        viewModelScope.launch {
            val result = repository.removeFromWishlist(listingId)
            if (result.isSuccess) {
                // Track BQ7 analytics event
                BusinessAnalyticsProvider.tracker.trackWishlistItemRemoved(
                    listingId = listingId,
                    metadata = mapOf("source" to "wishlist_screen")
                )
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
