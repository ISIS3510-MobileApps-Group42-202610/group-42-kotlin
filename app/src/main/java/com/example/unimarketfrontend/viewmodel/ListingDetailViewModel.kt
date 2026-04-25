package com.example.unimarketfrontend.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.unimarketfrontend.model.listing.Listing
import com.example.unimarketfrontend.model.listing.Review
import com.example.unimarketfrontend.model.repository.ListingRepository
import com.example.unimarketfrontend.model.repository.ListingCacheThenNetworkResult
import com.example.unimarketfrontend.model.user.User
import com.example.unimarketfrontend.model.local.AppDatabase
import com.example.unimarketfrontend.model.repository.BusinessAnalyticsProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ListingDetailUiState {
    object Loading : ListingDetailUiState()
    data class Success(
        val listing: Listing,
        val seller: User?,
        val reviews: List<Review>,
        val ratingSummary: RatingSummaryUi
    ) : ListingDetailUiState()
    data class Error(val message: String) : ListingDetailUiState()
}

data class RatingSummaryUi(
    val average: Double,
    val count: Int
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

    private var listingViewedTracked    = false
    private var chatStartedTracked      = false
    private var firstMessageSentTracked = false

    init { loadListing() }

    private fun loadListing() {
        val startMs = System.currentTimeMillis()
        viewModelScope.launch {
            try {
                val result: ListingCacheThenNetworkResult = repository.getByIdCacheThenNetwork(listingId)
                val listing = result.remote ?: result.cached

                if (listing == null) {
                    _uiState.value = ListingDetailUiState.Error("Listing no encontrado")
                    trackLoadPerformance(startMs, "not_found")
                    return@launch
                }

                val sellerResponse = repository.getSellerInfo(listing.seller_id)
                val seller: User?  = if (sellerResponse.isSuccessful) sellerResponse.body() else null

                val reviewsResponse = repository.getReviews(listing.id)
                val reviews: List<Review> = if (reviewsResponse.isSuccessful) reviewsResponse.body().orEmpty() else emptyList()

                val ratingSummary = buildRatingSummary(reviews)

                _uiState.value = ListingDetailUiState.Success(
                    listing       = listing,
                    seller        = seller,
                    reviews       = reviews,
                    ratingSummary = ratingSummary
                )
                trackListingViewed(listing)
                trackLoadPerformance(startMs, "success")

            } catch (e: Exception) {
                _uiState.value = ListingDetailUiState.Error(e.message ?: "Error inesperado")
                trackLoadPerformance(startMs, "exception")
            }
        }
    }

    private fun buildRatingSummary(reviews: List<Review>): RatingSummaryUi {
        val ratings = reviews.mapNotNull { it.rating?.toDouble() }
        return RatingSummaryUi(
            average = if (ratings.isEmpty()) 0.0 else ratings.average(),
            count   = ratings.size
        )
    }

    private fun trackLoadPerformance(startMs: Long, status: String) {
        BusinessAnalyticsProvider.tracker.trackPerformance(
            metricName = "listing_detail_load_time",
            valueMs    = System.currentTimeMillis() - startMs,
            listingId  = listingId,
            screenName = "ListingDetailScreen",
            metadata   = mapOf("status" to status)
        )
    }

    private fun trackListingViewed(listing: Listing) {
        if (listingViewedTracked) return
        listingViewedTracked = true
        BusinessAnalyticsProvider.tracker.trackListingViewed(
            listingId = listing.id,
            sellerId  = listing.seller_id,
            metadata  = mapOf("source" to "listing_detail_screen")
        )
    }

    fun trackChatStarted() {
        if (chatStartedTracked) return
        val listing = (uiState.value as? ListingDetailUiState.Success)?.listing ?: return
        chatStartedTracked = true
        BusinessAnalyticsProvider.tracker.trackChatStarted(
            listingId = listing.id,
            sellerId  = listing.seller_id,
            metadata  = mapOf("entrypoint" to "contact_seller_button")
        )
    }

    fun trackFirstMessageSent(messageLength: Int) {
        if (firstMessageSentTracked) return
        val listing = (uiState.value as? ListingDetailUiState.Success)?.listing ?: return
        firstMessageSentTracked = true
        BusinessAnalyticsProvider.tracker.trackFirstMessageSent(
            listingId = listing.id,
            sellerId  = listing.seller_id,
            metadata  = mapOf(
                "entrypoint"     to "detail_screen",
                "message_length" to messageLength.toString()
            )
        )
    }

    fun sendFirstMessage(sellerId: Int, content: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.sendMessage(sellerId, content)
                trackFirstMessageSent(content.length)
            } catch (_: Exception) { }
            onComplete()
        }
    }

    fun trackTransactionCompleted(rating: Int? = null, responseTimeMinutes: Int? = null) {
        val state   = uiState.value as? ListingDetailUiState.Success ?: return
        val listing = state.listing
        val seller  = state.seller

        BusinessAnalyticsProvider.tracker.trackTransactionCompleted(
            listingId = listing.id,
            sellerId  = listing.seller_id,
            metadata  = mapOf(
                "entrypoint"             to "detail_screen",
                "category"               to (listing.category ?: "unknown"),
                "product"                to (listing.product ?: "unknown"),
                "selling_price"          to listing.selling_price.toString(),
                "semester"               to (seller?.semester?.toString() ?: "unknown"),
                "seller_id"              to listing.seller_id,
                "rating"                 to (rating ?: 0),
                "response_time_minutes"  to (responseTimeMinutes ?: 0),
                "seeded_bq5"             to false,
            )
        )
    }
}

class ListingDetailViewModelFactory(
    private val application: Application,
    private val listingId: Int
) : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ListingDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ListingDetailViewModel(application, listingId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}