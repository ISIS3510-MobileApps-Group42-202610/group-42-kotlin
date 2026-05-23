package com.example.unimarketfrontend.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarketfrontend.model.listing.Listing
import com.example.unimarketfrontend.model.mappers.toListing
import com.example.unimarketfrontend.model.repository.AuthRepository
import com.example.unimarketfrontend.model.repository.BusinessAnalyticsProvider
import com.example.unimarketfrontend.model.repository.WishlistRepository
import com.example.unimarketfrontend.model.user.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class PurchaseItem(
    val transactionId: Int,
    val listingId: Int,
    val title: String,
    val price: Double,
    val imageUrl: String?,
    val sellerName: String?,
    val date: String?,
    val status: String?
)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()
    private val wishlistRepository = WishlistRepository(application)
    private val analyticsTracker = BusinessAnalyticsProvider.tracker

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user

    private val _wishlist = MutableStateFlow<List<Listing>>(emptyList())
    val wishlist: StateFlow<List<Listing>> = _wishlist

    private val _purchases = MutableStateFlow<List<PurchaseItem>>(emptyList())
    val purchases: StateFlow<List<PurchaseItem>> = _purchases

    private val _following = MutableStateFlow<List<User>>(emptyList())
    val following: StateFlow<List<User>> = _following

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMsg = MutableStateFlow<String?>(null)
    val errorMsg: StateFlow<String?> = _errorMsg

    init {
        loadProfile()
        observeWishlist()
    }

    private fun observeWishlist() {
        viewModelScope.launch {
            wishlistRepository.observeWishlist().collectLatest { entities ->
                _wishlist.value = entities.map { it.toListing() }
            }
        }
    }

    fun loadProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMsg.value = null
            try {
                _user.value = authRepository.getMyProfile()
            } catch (e: Exception) {
                _errorMsg.value = "Error getting user: ${e.message}"
            }

            launch { wishlistRepository.refreshFromNetwork() }

            try {
                val response = authRepository.getPurchases()
                _purchases.value = response.purchases.orEmpty().map { purchase ->
                    val listing = purchase.listing
                    PurchaseItem(
                        transactionId = purchase.id,
                        listingId = listing?.id ?: 0,
                        title = listing?.title ?: "Item deleted",
                        price = listing?.selling_price ?: 0.0,
                        imageUrl = listing?.images?.firstOrNull()?.url,
                        sellerName = null,
                        date = purchase.created_at,
                        status = "Completed"
                    )
                }
            } catch (_: Exception) {
            }

            try {
                _following.value = authRepository.getFollowing()
            } catch (_: Exception) {
            }

            _isLoading.value = false
        }
    }

    fun removeFromWishlist(listingId: Int) {
        viewModelScope.launch {
            try {
                val removedListing = _wishlist.value.firstOrNull { it.id == listingId }
                wishlistRepository.removeFromWishlist(listingId)
                trackEvent(
                    "wishlist_removed",
                    mapOf(
                        "listing_id" to listingId.toString(),
                        "course_id" to removedListing?.course_id?.toString(),
                        "category" to removedListing?.category,
                        "condition" to removedListing?.condition,
                        "source_screen" to "Profile"
                    )
                )
            } catch (_: Exception) {
                _errorMsg.value = "Error trying to delete from wishlist"
            }
        }
    }

    fun trackProfileWishlistViewed() {
        trackEvent("profile_wishlist_viewed", mapOf("source_screen" to "Profile"))
    }

    fun trackProfilePurchasesViewed() {
        trackEvent("profile_purchases_viewed", mapOf("source_screen" to "Profile"))
    }

    private fun trackEvent(eventName: String, params: Map<String, String?>) {
        try {
            analyticsTracker.trackEvent(eventName, params)
        } catch (_: Exception) {
        }
    }

    fun logout(onSuccess: () -> Unit) {
        authRepository.logout()
        onSuccess()
    }

    fun deleteAccount(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val userId = _user.value?.id ?: return@launch
                authRepository.deleteAccount(userId)
                authRepository.logout()
                onSuccess()
            } catch (e: Exception) {
                onError("Error al eliminar cuenta: ${e.message}")
            }
        }
    }
}
