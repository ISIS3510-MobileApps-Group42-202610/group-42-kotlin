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
        val ratingSummary: RatingSummaryUi
    ) : ListingDetailUiState()

    data class Error(val message: String) : ListingDetailUiState()
}

data class RatingSummaryUi(
    val average: Double,
    val count: Int
)

private data class ListingDetailActionState(
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

    private val _uiState = MutableStateFlow<ListingDetailUiState>(ListingDetailUiState.Loading)
    val uiState: StateFlow<ListingDetailUiState> = _uiState

    private var isLoadingListing = false
    private var shouldRetryWhenOnline = false
    private var listingViewedTracked = false
    private var chatStartedTracked = false
    private var firstMessageSentTracked = false
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
                }
            }
        }
    }

    private fun loadListing(forceLoading: Boolean = true) {
        if (isLoadingListing) return

        val startMs = System.currentTimeMillis()

        viewModelScope.launch {
            isLoadingListing = true

            if (forceLoading && _uiState.value !is ListingDetailUiState.Success) {
                _uiState.value = ListingDetailUiState.Loading
            }

            try {
                val result: ListingCacheThenNetworkResult =
                    repository.getByIdCacheThenNetwork(listingId)

                val listing = result.remote ?: result.cached

                if (listing == null) {
                    _uiState.value = ListingDetailUiState.Error("Listing no encontrado")
                    trackLoadPerformance(startMs, "not_found")
                    return@launch
                }

                emitSuccess(listing)
                trackListingViewed(listing)
                trackLoadPerformance(startMs, "success")
                shouldRetryWhenOnline = false
            } catch (e: Exception) {
                shouldRetryWhenOnline = true

                val userMessage = ErrorTranslator.getUserFriendlyMessage(e)

                if (_uiState.value !is ListingDetailUiState.Success) {
                    _uiState.value = ListingDetailUiState.Error(userMessage)
                }

                trackLoadPerformance(startMs, "exception")
            } finally {
                isLoadingListing = false
            }
        }
    }

    private suspend fun emitSuccess(listing: Listing) {
        val sellerUserId = listing.owner_user_id ?: listing.seller_id

        val seller = runCatching {
            repository.getSellerInfo(sellerUserId)
        }.getOrNull()
            ?.takeIf { it.isSuccessful }
            ?.body()
            ?: runCatching {
                RetrofitInstance.api.getUserById(sellerUserId).body()
            }.getOrNull()

        val buyer = listing.buyer_id?.let { buyerId ->
            runCatching {
                RetrofitInstance.api.getUserById(buyerId).body()
            }.getOrNull()
        }

        val currentUser = runCatching {
            repository.getMe()
        }.getOrNull()

        val reviews = runCatching {
            repository.getReviews(listing.id)
        }.getOrNull()
            ?.takeIf { it.isSuccessful }
            ?.body()
            .orEmpty()

        val ratingSummary = buildRatingSummary(reviews)

        val actionState = buildListingDetailActionState(
            listing = listing,
            seller = seller,
            buyer = buyer,
            currentUser = currentUser,
            reviews = reviews
        )

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
            ratingSummary = ratingSummary
        )
    }

    private fun buildListingDetailActionState(
        listing: Listing,
        seller: User?,
        buyer: User?,
        currentUser: User?,
        reviews: List<Review>
    ): ListingDetailActionState {
        val sellerUserId = listing.owner_user_id ?: seller?.id ?: listing.seller_id
        val currentUserId = currentUser?.id

        val isOwner = currentUserId != null && currentUserId == sellerUserId
        val canMarkAsSold = isOwner && listing.active

        val sellerDisplayName = seller?.let {
            "${it.name} ${it.last_name}".trim()
        }?.takeIf { it.isNotBlank() } ?: "Vendedor"

        val buyerDisplayName = buyer?.let {
            "${it.name} ${it.last_name}".trim()
        }?.takeIf { it.isNotBlank() }

        val contactTargetId = if (isOwner) {
            buyer?.id ?: listing.buyer_id
        } else {
            listing.seller_id
        }

        val contactTargetName = if (isOwner) {
            buyerDisplayName ?: listing.buyer_id?.let { "Usuario #$it" }
        } else {
            sellerDisplayName
        }

        val contactActionLabel = if (isOwner) {
            "Contactar comprador"
        } else {
            "Contactar vendedor"
        }

        val externalComments = reviews.filter { review ->
            val reviewerId = review.reviewer_id ?: review.user_id
            reviewerId != null && reviewerId != currentUserId
        }

        return ListingDetailActionState(
            sellerDisplayName = sellerDisplayName,
            contactTargetId = contactTargetId,
            contactTargetName = contactTargetName,
            contactActionLabel = contactActionLabel,
            isOwner = isOwner,
            canMarkAsSold = canMarkAsSold,
            hasExternalComments = externalComments.isNotEmpty(),
            externalCommentsCount = externalComments.size
        )
    }

    private fun buildRatingSummary(reviews: List<Review>): RatingSummaryUi {
        val ratings = reviews.mapNotNull { review ->
            review.rating?.toDouble()
        }

        return RatingSummaryUi(
            average = if (ratings.isEmpty()) 0.0 else ratings.average(),
            count = ratings.size
        )
    }

    private fun trackLoadPerformance(startMs: Long, status: String) {
        BusinessAnalyticsProvider.tracker.trackPerformance(
            metricName = "listing_detail_load_time",
            valueMs = System.currentTimeMillis() - startMs,
            listingId = listingId,
            screenName = "ListingDetailScreen",
            metadata = mapOf("status" to status)
        )
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

    fun sendMessage(
        content: String,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val cleanContent = content.trim()

                if (cleanContent.isBlank()) {
                    onError()
                    return@launch
                }

                val sellerId = (uiState.value as? ListingDetailUiState.Success)
                    ?.listing
                    ?.seller_id
                    ?: return@launch

                repository.sendMessage(sellerId, cleanContent)

                trackFirstMessageSent(cleanContent.length)

                onSuccess()
            } catch (e: Exception) {
                onError()
            }
        }
    }

    fun markAsSold(
        onComplete: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val currentState = _uiState.value as? ListingDetailUiState.Success
                ?: return@launch onError("No se pudo actualizar la publicación")

            if (!currentState.canMarkAsSold) {
                onError("Esta publicación no se puede marcar como vendida")
                return@launch
            }

            try {
                trackTransactionCompleted()
                loadListing(forceLoading = false)
                onComplete()
            } catch (e: Exception) {
                onError(e.message ?: "No se pudo marcar como vendida")
            }
        }
    }

    fun deleteListing(
        onComplete: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val response = repository.deleteListing(listingId)

                if (response.isSuccessful) {
                    onComplete()
                } else {
                    onError("Error al eliminar en el servidor")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Error de red al eliminar")
            }
        }
    }

    fun sendFirstMessage(
        recipientUserId: Int,
        content: String,
        onComplete: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val cleanContent = content.trim()

                if (cleanContent.isBlank()) {
                    onError("El mensaje no puede estar vacío")
                    return@launch
                }

                repository.sendMessage(recipientUserId, cleanContent)

                trackFirstMessageSent(cleanContent.length)

                onComplete()
            } catch (e: Exception) {
                val userMessage = ErrorTranslator.getUserFriendlyMessage(e)
                onError(userMessage)
            }
        }
    }

    fun trackTransactionCompleted(
        rating: Int? = null,
        responseTimeMinutes: Int? = null
    ) {
        if (transactionTracked) return

        val state = uiState.value as? ListingDetailUiState.Success ?: return
        val listing = state.listing
        val seller = state.seller

        transactionTracked = true

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
                "response_time_minutes" to (responseTimeMinutes ?: 0).toString()
            )
        )
    }
}

class ListingDetailViewModelFactory(
    private val application: Application,
    private val listingId: Int
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ListingDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ListingDetailViewModel(application, listingId) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}