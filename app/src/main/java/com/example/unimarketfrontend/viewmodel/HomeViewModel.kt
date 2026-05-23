package com.example.unimarketfrontend.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarketfrontend.model.local.AppDatabase
import com.example.unimarketfrontend.model.utils.ConnectivityMonitor
import com.example.unimarketfrontend.model.utils.ErrorTranslator
import com.example.unimarketfrontend.model.network.client.RetrofitInstance
import com.example.unimarketfrontend.model.listing.Listing
import com.example.unimarketfrontend.model.repository.HomeRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(
        val userName: String,
        val trending: List<Listing>,
        val recent: List<Listing>,
        val categories: List<CategoryUi>,
        val wishlistListingIds: Set<Int> = emptySet()
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

data class CategoryUi(
    val name: String,
    val count: Int
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val homeRepository = HomeRepository(
        listingDao = AppDatabase.getInstance(application).listingDao()
    )

    private val authRepository = com.example.unimarketfrontend.model.repository.AuthRepository()

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState

    private var isLoadingHome = false
    private var shouldRetryWhenOnline = false

    init {
        observeConnectivity()
        loadHome()
    }

    fun refreshHome() {
        loadHome(forceLoading = false)
    }

    // Escuchamos al monitor de red para saber si volvio el internet
    private fun observeConnectivity() {
        viewModelScope.launch {
            ConnectivityMonitor.isOnline.collect { online ->
                if (online && shouldRetryWhenOnline && !isLoadingHome) {
                    shouldRetryWhenOnline = false
                    loadHome(forceLoading = false)
                }
            }
        }
    }

    private fun loadHome(forceLoading: Boolean = true) {
        if (isLoadingHome) return

        viewModelScope.launch {
            isLoadingHome = true

            if (forceLoading && _uiState.value !is HomeUiState.Success) {
                _uiState.value = HomeUiState.Loading
            }

            val cachedListings = homeRepository.getCachedActiveListings()

            if (cachedListings.isNotEmpty() && _uiState.value !is HomeUiState.Success) {
                _uiState.value = HomeUiState.Success(
                    userName = "...",
                    trending = cachedListings,
                    recent = emptyList(),
                    categories = categoriesFromListings(cachedListings)
                )
            }

            try {
                coroutineScope {
                    val userDeferred = async {
                        runCatching {
                            val me = RetrofitInstance.api.getMe()
                            // Set analytics user ID whenever we fetch the user
                            com.example.unimarketfrontend.model.utils.analytics.AnalyticsLogger.setUserId(me.id)
                            me.name
                        }.getOrDefault("there")
                    }
                    val homeDataDeferred = async {
                        homeRepository.refreshHomeData()
                    }
                    val wishlistDeferred = async {
                        runCatching { authRepository.getWishlist() }.getOrNull()
                    }

                    val userName = userDeferred.await()
                    val homeResponse = homeDataDeferred.await()
                    val wishlist = wishlistDeferred.await()

                    _uiState.value = HomeUiState.Success(
                        userName = userName,
                        trending = homeResponse.trending.filter { it.active },
                        recent = homeResponse.recent.filter { it.active },
                        categories = homeResponse.categories.map { CategoryUi(it.category, it.count) },
                        wishlistListingIds = wishlist?.map { it.id }?.toSet() ?: emptySet()
                    )
                }
                shouldRetryWhenOnline = false
            } catch (e: Exception) {
                shouldRetryWhenOnline = true
                // Si Room esta vacio y falla el internet, mostramos error amigable
                if (cachedListings.isEmpty()) {
                    val userMessage = ErrorTranslator.getUserFriendlyMessage(e)
                    _uiState.value = HomeUiState.Error(userMessage)
                }
            } finally {
                isLoadingHome = false
            }
        }
    }

    private fun categoriesFromListings(listings: List<Listing>): List<CategoryUi> {
        return listings
            .groupingBy { it.category ?: "Others" }
            .eachCount()
            .map { CategoryUi(name = it.key, count = it.value) }
            .sortedByDescending { it.count }
    }

    fun toggleWishlist(listingId: Int) {
        val currentState = _uiState.value as? HomeUiState.Success ?: return
        val wasWishlisted = currentState.wishlistListingIds.contains(listingId)
        val updated = if (wasWishlisted) {
            currentState.wishlistListingIds - listingId
        } else {
            currentState.wishlistListingIds + listingId
        }
        
        _uiState.value = currentState.copy(wishlistListingIds = updated)

        viewModelScope.launch {
            try {
                if (wasWishlisted) {
                    authRepository.removeFromWishlist(listingId)
                } else {
                    authRepository.addToWishlist(listingId)
                }
                val listing = (currentState.trending + currentState.recent).firstOrNull { it.id == listingId }
                com.example.unimarketfrontend.model.repository.BusinessAnalyticsProvider.tracker.trackCustomEvent(
                    eventName = if (wasWishlisted) "wishlist_removed" else "wishlist_added",
                    listingId = listingId,
                    metadata = mapOf("source_screen" to "home")
                )
                com.example.unimarketfrontend.model.repository.BusinessAnalyticsProvider.tracker.trackEvent(
                    eventName = if (wasWishlisted) "wishlist_removed" else "wishlist_added",
                    params = mapOf(
                        "listing_id" to listingId.toString(),
                        "course_id" to listing?.course_id?.toString(),
                        "category" to listing?.category,
                        "condition" to listing?.condition,
                        "source_screen" to "Home"
                    )
                )
            } catch (e: Exception) {
                _uiState.value = currentState.copy(wishlistListingIds = currentState.wishlistListingIds)
            }
        }
    }
}
