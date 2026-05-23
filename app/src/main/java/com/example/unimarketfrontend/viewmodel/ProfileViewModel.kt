package com.example.unimarketfrontend.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarketfrontend.model.listing.Listing
import com.example.unimarketfrontend.model.listing.ListingImage
import com.example.unimarketfrontend.model.listing.Purchase
import com.example.unimarketfrontend.model.mappers.toListing
import com.example.unimarketfrontend.model.repository.AuthRepository
import com.example.unimarketfrontend.model.repository.WishlistRepository
import com.example.unimarketfrontend.model.user.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * SPRINT 4 — ProfileViewModel
 * Updated to use WishlistRepository for reactive local storage (Tank Architecture).
 * Updated to use WishlistRepository for reactive local storage.
 */
class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()
    private val wishlistRepository = WishlistRepository(application)

    private val _user      = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user

    private val _wishlist  = MutableStateFlow<List<Listing>>(emptyList())
    val wishlist: StateFlow<List<Listing>> = _wishlist

    private val _purchases = MutableStateFlow<List<Purchase>>(emptyList())
    val purchases: StateFlow<List<Purchase>> = _purchases

    private val _following = MutableStateFlow<List<User>>(emptyList())
    val following: StateFlow<List<User>> = _following

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMsg  = MutableStateFlow<String?>(null)
    val errorMsg: StateFlow<String?> = _errorMsg

    init { 
        loadProfile()
        observeWishlist()
    }

    private fun observeWishlist() {
        viewModelScope.launch {
            wishlistRepository.observeWishlist().collectLatest { entities ->
                _wishlist.value = entities.map { it.toListing() }
                _wishlist.value = entities.map { entity ->
                    Listing(
                        id = entity.listingId,
                        seller_id = entity.sellerId,
                        owner_user_id = null,
                        buyer_id = null,
                        course_id = null,
                        title = entity.title,
                        product = null,
                        category = entity.category,
                        condition = entity.condition,
                        original_price = null,
                        selling_price = entity.sellingPrice,
                        created_at = "",
                        updated_at = "",
                        active = entity.active,
                        images = if (entity.imageUrl != null) listOf(
                            ListingImage(
                                id = 0,
                                listing_id = entity.listingId,
                                is_primary = true,
                                uploaded_at = "",
                                url = entity.imageUrl
                            )
                        ) else emptyList(),
                        priceHistory = null
                    )
                }
            }
        }
    }

    fun loadProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMsg.value  = null
            try {
                _user.value = authRepository.getMyProfile()
            } catch (e: Exception) {
                _errorMsg.value = "Error getting user: ${e.message}"
            }
            
            // Trigger a network refresh to ensure local cache is up to date
            // Background refresh to ensure cache is fresh
            launch {
                wishlistRepository.refreshFromNetwork()
            }

            try {
                val response = authRepository.getPurchases()
                _purchases.value = response.purchases ?: emptyList()
            } catch (e: Exception) {
            }
            try {
                _following.value = authRepository.getFollowing()
            } catch (e: Exception) {
            }
            _isLoading.value = false
        }
    }

    fun removeFromWishlist(listingId: Int) {
        viewModelScope.launch {
            try {
                wishlistRepository.removeFromWishlist(listingId)
                // UI will update automatically via Flow observation
            } catch (e: Exception) {
                _errorMsg.value = "Error trying to delete from wishlist"
            }
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
