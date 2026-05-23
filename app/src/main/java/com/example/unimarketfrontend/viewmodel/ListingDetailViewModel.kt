package com.example.unimarketfrontend.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.unimarketfrontend.model.listing.Listing
import com.example.unimarketfrontend.model.listing.Review
import com.example.unimarketfrontend.model.local.AppDatabase
import com.example.unimarketfrontend.model.network.client.RetrofitInstance
import com.example.unimarketfrontend.model.repository.BusinessAnalyticsProvider
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

data class ListingDetailActionState(
    val sellerDisplayName: String,
    val contactTargetId: Int?,
    val contactTargetName: String?,
    val contactActionLabel: String,
    val isOwner: Boolean,
    val canMarkAsSold: Boolean,
    val hasExternalComments: Boolean,
    val externalCommentsCount: Int
)

class ListingDetailViewModel(
    application: Application,
    private val listingId: Int
) : AndroidViewModel(application) {

    private val repository = ListingRepository(
        listingDao = AppDatabase.getInstance(application).listingDao()
    )
    private val wishlistRepository = WishlistRepository(application)

    private val _uiState = MutableStateFlow<ListingDetailUiState>(ListingDetailUiState.Loading)
    val uiState: StateFlow<ListingDetailUiState> = _uiState

    private var isLoadingListing = false
    private var shouldRetryWhenOnline = false
    private var transactionTracked = false
    private var listingViewedTracked = false
    private var chatStartedTracked = false
    private var firstMessageSentTracked = false

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
                trackListingViewed(listing)
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

        val seller = runCatching {
            repository.getSellerInfo(sellerUserId)
        }.getOrNull()?.takeIf { it.isSuccessful }?.body()
            ?: runCatching { RetrofitInstance.api.getUserById(sellerUserId).body() }.getOrNull()

        val buyer = listing.buyer_id?.let { buyerId ->
            runCatching { RetrofitInstance.api.getUserById(buyerId).body() }.getOrNull()
        }

        val currentUser = runCatching { repository.getMe() }.getOrNull()

        val reviews = runCatching {
            repository.getReviews(listing.id)
        }.getOrNull()?.takeIf { it.isSuccessful }?.body().orEmpty()

        val ratingSummary = buildRatingSummary(reviews)
        val actionState = buildListingDetailActionState(listing, seller, buyer, currentUser, reviews)
        val isInWishlist = wishlistRepository.isInWishlist(listing.id)

        _uiState.value = ListingDetailUiState.Success(
            listing = listing,
            seller = seller,
            sellerDisplayName = actionState.sellerDisplayName,
            contactTargetId = actionState.contactTargetId,
            contactTargetName = actionState.contactTargetName,
            contactActionLabel = actionState.contactActionLabel,
            isOwner = actionState.isOwner,
            canMarkAsSold = actionState.canMarkAsSold,
            hasExternalComments = actionState.hasExternalComments,
            externalCommentsCount = actionState.externalCommentsCount,
            reviews = reviews,
            ratingSummary = ratingSummary,
            isInWishlist = isInWishlist
        )
    }

    private fun buildRatingSummary(reviews: List<Review>): RatingSummaryUi {
        val ratings = reviews.mapNotNull { it.rating?.toDouble() }
        if (ratings.isEmpty()) return RatingSummaryUi(0.0, 0)
        return RatingSummaryUi(ratings.average(), ratings.size)
    }

    private fun buildListingDetailActionState(
        listing: Listing,
        seller: User?,
        buyer: User?,
        currentUser: User?,
        reviews: List<Review>
    ): ListingDetailActionState {
        val isOwner = currentUser?.id == (listing.owner_user_id ?: listing.seller_id)
        val isBuyer = buyer?.id == currentUser?.id || listing.buyer_id == currentUser?.id
        val sellerName = seller?.let { "${it.name} ${it.last_name}".trim() }.orEmpty().ifBlank { "Vendedor" }

        return ListingDetailActionState(
            sellerDisplayName = sellerName,
            contactTargetId = if (isOwner) buyer?.id else seller?.id,
            contactTargetName = if (isOwner) buyer?.let { "${it.name} ${it.last_name}".trim() } else sellerName,
            contactActionLabel = if (isOwner) "Contactar comprador" else "Contactar vendedor",
            isOwner = isOwner,
            canMarkAsSold = isOwner && listing.active,
            hasExternalComments = reviews.any { it.user_id != currentUser?.id },
            externalCommentsCount = reviews.count { it.user_id != currentUser?.id && !it.content.isNullOrBlank() }
        )
    }

    fun toggleWishlist() {
        val currentState = _uiState.value as? ListingDetailUiState.Success ?: return
        viewModelScope.launch {
            if (currentState.isInWishlist) {
                wishlistRepository.removeFromWishlist(listingId)
                BusinessAnalyticsProvider.tracker.trackWishlistItemRemoved(
                    listingId = listingId,
                    sellerId = currentState.listing.seller_id,
                    metadata = mapOf("source" to "listing_detail")
                )
            } else {
                wishlistRepository.addToWishlist(currentState.listing)
                BusinessAnalyticsProvider.tracker.trackWishlistItemAdded(
                    listingId = listingId,
                    sellerId = currentState.listing.seller_id,
                    metadata = mapOf(
                        "category" to (currentState.listing.category ?: "unknown"),
                        "price" to currentState.listing.selling_price.toString()
                    )
                )
            }
            _uiState.value = currentState.copy(isInWishlist = !currentState.isInWishlist)
        }
    }

    private fun trackListingViewed(listing: Listing) {
        if (listingViewedTracked) return
        listingViewedTracked = true
        BusinessAnalyticsProvider.tracker.trackListingViewed(
            listingId = listing.id,
            sellerId = listing.seller_id,
            metadata = mapOf("source" to "listing_detail_screen")
        )
    }

    fun trackChatStarted() {
        if (chatStartedTracked) return
        val listing = (uiState.value as? ListingDetailUiState.Success)?.listing ?: return
        chatStartedTracked = true
        BusinessAnalyticsProvider.tracker.trackChatStarted(
            listingId = listing.id,
            sellerId = listing.seller_id,
            metadata = mapOf("entrypoint" to "contact_seller_button")
        )
    }

    fun trackFirstMessageSent(messageLength: Int) {
        if (firstMessageSentTracked) return
        val listing = (uiState.value as? ListingDetailUiState.Success)?.listing ?: return
        firstMessageSentTracked = true
        BusinessAnalyticsProvider.tracker.trackFirstMessageSent(
            listingId = listing.id,
            sellerId = listing.seller_id,
            metadata = mapOf(
                "entrypoint" to "detail_screen",
                "message_length" to messageLength.toString()
            )
        )
    }

    fun markAsSold(
        rating: Int,
        onComplete: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.markAsSoldRemotely(listingId)
                repository.markAsSoldLocally(listingId)
                trackTransactionCompleted(rating = rating)
                loadListing(forceLoading = false)
                onComplete()
            } catch (e: Exception) {
                onError(e.message ?: "Error")
            }
        }
    }

    fun sendFirstMessage(recipientUserId: Int, content: String, onComplete: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.sendMessage(recipientUserId, content)
                trackFirstMessageSent(content.length)
                onComplete()
            } catch (e: Exception) {
                onError(ErrorTranslator.getUserFriendlyMessage(e))
            }
        }
    }

    fun trackTransactionCompleted(rating: Int? = null, responseTimeMinutes: Int? = null) {
        if (transactionTracked) return
        transactionTracked = true

        val state = uiState.value as? ListingDetailUiState.Success ?: return
        val listing = state.listing
        val seller = state.seller

        BusinessAnalyticsProvider.tracker.trackTransactionCompleted(
            listingId = listing.id,
            sellerId = listing.seller_id,
            metadata = mapOf(
                "entrypoint" to "detail_screen",
                "category" to (listing.category ?: "unknown"),
                "product" to (listing.product ?: "unknown"),
                "selling_price" to listing.selling_price.toString(),
                "semester" to (seller?.semester?.toString() ?: "unknown"),
                "seller_id" to listing.seller_id.toString(),
                "rating" to (rating ?: 0).toString(),
                "response_time_minutes" to (responseTimeMinutes ?: 0).toString(),
                "is_from_wishlist" to state.isInWishlist.toString()
            )
        )
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
