package com.example.unimarketfrontend.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarketfrontend.model.local.AppDatabase
import com.example.unimarketfrontend.model.listing.Listing
import com.example.unimarketfrontend.model.repository.BusinessAnalyticsProvider
import com.example.unimarketfrontend.model.repository.ListingRepository
import com.example.unimarketfrontend.model.repository.WishlistRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * SPRINT 4 — SearchViewModel
 * Updated to include Wishlist integration for quick actions from search results.
 */
class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ListingRepository(
        listingDao = AppDatabase.getInstance(application).listingDao()
    )
    
    private val wishlistRepository = WishlistRepository(application)

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _results = MutableStateFlow<List<Listing>>(emptyList())
    val results: StateFlow<List<Listing>> = _results

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // SPRINT 4: Set of IDs in the wishlist for fast lookup
    val wishlistIds: StateFlow<Set<Int>> = wishlistRepository.observeWishlist()
        .map { list -> list.map { it.listingId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptySet())

    private var allListings: List<Listing> = emptyList()
    private var debounceJob: Job? = null

    init {
        fetchAllListings()
    }

    fun refresh() {
        fetchAllListings()
    }

    private fun fetchAllListings() {
        viewModelScope.launch {
            try {
                allListings = repository.syncSearchListings()
            } catch (e: Exception) {
                allListings = repository.getCachedActiveListings()
            }

            if (_query.value.isNotBlank()) {
                _results.value = filterListings(_query.value)
            }
        }
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        debounceJob?.cancel()

        if (newQuery.isBlank()) {
            _results.value = emptyList()
            _isLoading.value = false
            return
        }

        debounceJob = viewModelScope.launch {
            delay(300)
            _isLoading.value = true
            _results.value = filterListings(newQuery)
            _isLoading.value = false
        }
    }

    private fun filterListings(query: String): List<Listing> {
        val lower = query.lowercase().trim()

        return allListings.filter { listing ->
            listing.active &&
                (
                    listing.title.lowercase().contains(lower) ||
                    listing.category?.lowercase()?.contains(lower) == true ||
                    listing.condition?.lowercase()?.contains(lower) == true ||
                    listing.product?.lowercase()?.contains(lower) == true
                )
        }
    }

    fun toggleWishlist(listing: Listing) {
        val isInWishlist = wishlistIds.value.contains(listing.id)
        viewModelScope.launch {
            if (isInWishlist) {
                wishlistRepository.removeFromWishlist(listing.id)
                BusinessAnalyticsProvider.tracker.trackWishlistItemRemoved(
                    listingId = listing.id,
                    metadata = mapOf("source" to "search_results")
                )
            } else {
                wishlistRepository.addToWishlist(listing)
                BusinessAnalyticsProvider.tracker.trackWishlistItemAdded(
                    listingId = listing.id,
                    sellerId = listing.seller_id,
                    metadata = mapOf(
                        "category" to (listing.category ?: "unknown"),
                        "price" to listing.selling_price.toString(),
                        "source" to "search_results"
                    )
                )
            }
        }
    }
}
