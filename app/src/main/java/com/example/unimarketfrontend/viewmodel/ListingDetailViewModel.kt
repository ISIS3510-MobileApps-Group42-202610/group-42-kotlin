package com.example.unimarketfrontend.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.unimarketfrontend.model.local.AppDatabase
import com.example.unimarketfrontend.model.listing.Listing
import com.example.unimarketfrontend.model.listing.Review
import com.example.unimarketfrontend.model.network.client.RetrofitInstance
import com.example.unimarketfrontend.model.repository.BusinessAnalyticsProvider
import com.example.unimarketfrontend.model.repository.ListingCacheThenNetworkResult
import com.example.unimarketfrontend.model.repository.ListingRepository
import com.example.unimarketfrontend.model.repository.WishlistRepository
import com.example.unimarketfrontend.model.user.User
import com.example.unimarketfrontend.model.utils.ConnectivityMonitor
import com.example.unimarketfrontend.model.utils.ErrorTranslator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed class ListingDetailUiState {
    object Loading : ListingDetailUiState()

    data class Success(
        val listing: Listing,
        val seller: User?,
        val sellerDisplayName: String,
        val contactTargetId: Int?,
        val contactTargetName: String?,
        val contactActionLabel: String,
        val isOwner: Boolean,
        val canMarkAsSold: Boolean,
        val hasExternalComments: Boolean,
        val externalCommentsCount: Int,
        val reviews: List<Review>,
        val ratingSummary: RatingSummaryUi,
        val isInWishlist: Boolean = false
    ) : ListingDetailUiState()

    data class Error(val message: String) : ListingDetailUiState()
}

data class RatingSummaryUi(val average: Double, val count: Int)

class ListingDetailViewModel(
    application: Application,
    private val listingId: Int
) : AndroidViewModel(application) {

    private val repository = ListingRepository(listingDao = AppDatabase.getInstance(application).listingDao())
    private val wishlistRepository = WishlistRepository(application)

    private val _uiState = MutableStateFlow<ListingDetailUiState>(ListingDetailUiState.Loading)
    val uiState: StateFlow<ListingDetailUiState> = _uiState

    private var isLoadingListing = false
    private var shouldRetryWhenOnline = false
    private var transactionTracked = false

    init {
        observeConnectivity()
        loadListing()
    }

    private fun observeConnectivity() {
        viewModelScope.launch {
            ConnectivityMonitor.isOnline.collectLatest { online ->
                if (online && shouldRetryWhenOnline && !isLoadingListing) {
                    shouldRetryWhenOnline = false
                    loadListing(forceLoading = false)
                    wishlistRepository.retryPendingActions()
                }
            }
        }
    }

    private fun loadListing(forceLoading: Boolean = true) {
        if (isLoadingListing) return
        viewModelScope.launch {
            isLoadingListing = true
            if (forceLoading && _uiState.value !is ListingDetailUiState.Success) {
                _uiState.value = ListingDetailUiState.Loading
            }
            try {
                val result = repository.getByIdCacheThenNetwork(listingId)
                val listing = result.remote ?: result.cached
                if (listing == null) {
                    _uiState.value = ListingDetailUiState.Error("Listing no encontrado")
                    return@launch
                }
                emitSuccess(listing)
                shouldRetryWhenOnline = false
            } catch (e: Exception) {
                shouldRetryWhenOnline = true
                if (_uiState.value !is ListingDetailUiState.Success) {
                    _uiState.value = ListingDetailUiState.Error(ErrorTranslator.getUserFriendlyMessage(e))
                }
            } finally {
                isLoadingListing = false
            }
        }
    }

    private suspend fun emitSuccess(listing: Listing) {
        val sellerUserId = listing.owner_user_id ?: listing.seller_id
        val seller = runCatching { RetrofitInstance.api.getUserById(sellerUserId).body() }.getOrNull()
        val buyer = listing.buyer_id?.let { runCatching { RetrofitInstance.api.getUserById(it).body() }.getOrNull() }
        val currentUser = runCatching { repository.getMe() }.getOrNull()
        val reviews = runCatching { repository.getReviews(listing.id).body() }.getOrNull().orEmpty()
        val isInWishlist = wishlistRepository.isInWishlist(listing.id)
        
        val actionState = buildListingDetailActionState(listing, seller, buyer, currentUser, reviews)

        _uiState.value = ListingDetailUiState.Success(
            listing = listing, seller = seller, sellerDisplayName = actionState.sellerDisplayName,
            contactTargetId = actionState.contactTargetId, contactTargetName = actionState.contactTargetName,
            contactActionLabel = actionState.contactActionLabel, isOwner = actionState.isOwner,
            canMarkAsSold = actionState.canMarkAsSold, hasExternalComments = actionState.hasExternalComments,
            externalCommentsCount = actionState.externalCommentsCount, reviews = reviews,
            ratingSummary = RatingSummaryUi(if (reviews.isEmpty()) 0.0 else reviews.mapNotNull { it.rating?.toDouble() }.average(), reviews.size),
            isInWishlist = isInWishlist
        )
    }

    fun toggleWishlist() {
        val currentState = _uiState.value as? ListingDetailUiState.Success ?: return
        viewModelScope.launch {
            if (currentState.isInWishlist) {
                wishlistRepository.removeFromWishlist(listingId)
                BusinessAnalyticsProvider.tracker.trackWishlistItemRemoved(listingId)
            } else {
                wishlistRepository.addToWishlist(currentState.listing)
                BusinessAnalyticsProvider.tracker.trackWishlistItemAdded(listingId, currentState.listing.seller_id)
            }
            _uiState.value = currentState.copy(isInWishlist = !currentState.isInWishlist)
        }
    }

    fun markAsSold(onComplete: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.markAsSoldRemotely(listingId)
                repository.markAsSoldLocally(listingId)
                trackTransactionCompleted()
                onComplete()
            } catch (e: Exception) { onError(e.message ?: "Error") }
        }
    }

    fun trackTransactionCompleted() {
        if (transactionTracked) return
        val state = uiState.value as? ListingDetailUiState.Success ?: return
        transactionTracked = true
        BusinessAnalyticsProvider.tracker.trackTransactionCompleted(
            listingId = state.listing.id,
            sellerId = state.listing.seller_id,
            metadata = mapOf("is_from_wishlist" to state.isInWishlist.toString())
        )
    }

    fun sendFirstMessage(recipientUserId: Int, content: String, onComplete: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.sendMessage(recipientUserId, content)
                onComplete()
            } catch (e: Exception) { onError(ErrorTranslator.getUserFriendlyMessage(e)) }
        }
    }
    
    fun trackChatStarted() {
        val listing = (uiState.value as? ListingDetailUiState.Success)?.listing ?: return
        BusinessAnalyticsProvider.tracker.trackChatStarted(listing.id, listing.seller_id)
    }
}

class ListingDetailViewModelFactory(private val application: Application, private val listingId: Int) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ListingDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ListingDetailViewModel(application, listingId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
