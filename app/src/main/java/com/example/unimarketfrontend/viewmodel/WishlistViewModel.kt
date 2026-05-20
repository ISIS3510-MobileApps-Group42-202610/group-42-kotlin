package com.example.unimarketfrontend.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarketfrontend.model.local.WishlistEntity
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
 * Implementa arquitectura Offline-First y sincronización de BQ7.
 */
class WishlistViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WishlistRepository(application)

    val wishlist: StateFlow<List<WishlistEntity>> = repository.observeWishlist()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val isOffline: StateFlow<Boolean> = ConnectivityMonitor.isOnline
        .map { !it }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    init {
        observeConnectivity()
        loadWishlist()
    }

    private fun observeConnectivity() {
        viewModelScope.launch {
            ConnectivityMonitor.isOnline.collectLatest { online ->
                if (online) {
                    // SPRINT 4: Sincronizar acciones pendientes al recuperar red
                    repository.retryPendingActions()
                    repository.refreshFromNetwork()
                }
            }
        }
    }

    fun loadWishlist() {
        viewModelScope.launch {
            _isRefreshing.value = true
            if (repository.isCacheStale() || wishlist.value.isEmpty()) {
                repository.refreshFromNetwork()
            }
            _isRefreshing.value = false
        }
    }

    fun removeFromWishlist(listingId: Int) {
        viewModelScope.launch {
            repository.removeFromWishlist(listingId)
            // Tracking para BQ7
            BusinessAnalyticsProvider.tracker.trackWishlistItemRemoved(listingId)
        }
    }
}
