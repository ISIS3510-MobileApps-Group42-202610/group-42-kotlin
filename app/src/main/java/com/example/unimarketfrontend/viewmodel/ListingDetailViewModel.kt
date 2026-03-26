package com.example.unimarketfrontend.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.unimarketfrontend.analytics.BusinessAnalyticsProvider
import com.example.unimarketfrontend.network.RetrofitInstance
import com.example.unimarketfrontend.network.model.Listing
import com.example.unimarketfrontend.network.model.Review
import com.example.unimarketfrontend.network.model.User
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
    private val listingId: Int
) : ViewModel() {

    private val _uiState = MutableStateFlow<ListingDetailUiState>(ListingDetailUiState.Loading)
    val uiState: StateFlow<ListingDetailUiState> = _uiState

    private var listingViewedTracked   = false
    private var chatStartedTracked     = false
    private var firstMessageSentTracked = false

    init {
        loadListing()
    }

    private fun loadListing() {
        val startMs = System.currentTimeMillis()
        viewModelScope.launch {
            try {
                val listingResponse = RetrofitInstance.api.getListings()
                if (!listingResponse.isSuccessful || listingResponse.body() == null) {
                    _uiState.value = ListingDetailUiState.Error("No se pudo cargar el listing")
                    BusinessAnalyticsProvider.tracker.trackPerformance(
                        metricName  = "listing_detail_load_time",
                        valueMs     = System.currentTimeMillis() - startMs,
                        listingId   = listingId,
                        screenName  = "ListingDetailScreen",
                        metadata    = mapOf("status" to "http_error")
                    )
                    return@launch
                }

                val listing = listingResponse.body()?.firstOrNull { it.id == listingId }
                if (listing == null) {
                    _uiState.value = ListingDetailUiState.Error("Listing no encontrado")
                    BusinessAnalyticsProvider.tracker.trackPerformance(
                        metricName  = "listing_detail_load_time",
                        valueMs     = System.currentTimeMillis() - startMs,
                        listingId   = listingId,
                        screenName  = "ListingDetailScreen",
                        metadata    = mapOf("status" to "not_found")
                    )
                    return@launch
                }

                val seller        = loadSeller(listing.seller_id)
                val reviews       = loadReviews(listing.id)
                val ratingSummary = loadAverageRating(listing.id, reviews)

                _uiState.value = ListingDetailUiState.Success(
                    listing       = listing,
                    seller        = seller,
                    reviews       = reviews,
                    ratingSummary = ratingSummary
                )
                trackListingViewed(listing)
                BusinessAnalyticsProvider.tracker.trackPerformance(
                    metricName  = "listing_detail_load_time",
                    valueMs     = System.currentTimeMillis() - startMs,
                    listingId   = listing.id,
                    screenName  = "ListingDetailScreen",
                    metadata    = mapOf("status" to "success")
                )
            } catch (e: Exception) {
                _uiState.value = ListingDetailUiState.Error(e.message ?: "Error inesperado")
                BusinessAnalyticsProvider.tracker.trackPerformance(
                    metricName  = "listing_detail_load_time",
                    valueMs     = System.currentTimeMillis() - startMs,
                    listingId   = listingId,
                    screenName  = "ListingDetailScreen",
                    metadata    = mapOf("status" to "exception")
                )
            }
        }
    }

    private suspend fun loadSeller(sellerId: Int): User? {
        return try {
            val response = RetrofitInstance.api.getUserById(sellerId)
            if (response.isSuccessful) response.body() else null
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun loadReviews(listingId: Int): List<Review> {
        return try {
            val response = RetrofitInstance.api.getReviewsByListing(listingId)
            if (response.isSuccessful) response.body().orEmpty() else emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private suspend fun loadAverageRating(listingId: Int, reviews: List<Review>): RatingSummaryUi {
        return try {
            val response = RetrofitInstance.api.getAverageByListing(listingId)
            val body     = if (response.isSuccessful) response.body().orEmpty() else emptyMap()
            val averageFromApi = pickFirstNumber(body, listOf("average", "avg", "rating_average", "ratingAverage"))
            val countFromApi   = pickFirstNumber(body, listOf("count", "total", "total_reviews", "reviews_count"))?.toInt()
            val fallbackAverage = calculateAverageFromReviews(reviews)
            RatingSummaryUi(
                average = averageFromApi ?: fallbackAverage,
                count   = countFromApi   ?: reviews.size
            )
        } catch (_: Exception) {
            RatingSummaryUi(
                average = calculateAverageFromReviews(reviews),
                count   = reviews.size
            )
        }
    }

    private fun pickFirstNumber(source: Map<String, Any?>, keys: List<String>): Double? {
        for (key in keys) {
            val value   = source[key] ?: continue
            val numeric = when (value) {
                is Number -> value.toDouble()
                is String -> value.toDoubleOrNull()
                else      -> null
            }
            if (numeric != null) return numeric
        }
        return null
    }

    private fun calculateAverageFromReviews(reviews: List<Review>): Double {
        val ratings = reviews.mapNotNull { it.rating }
        if (ratings.isEmpty()) return 0.0
        return ratings.average()
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
    fun sendFirstMessage(sellerId: Int, content: String, onComplete: () -> Unit) {
        viewModelScope.launch {
                android.util.Log.d("CHAT_DEBUG", "Enviando a sellerId: $sellerId con contenido: $content")

                val request = com.example.unimarketfrontend.network.model.SendMessageRequest(
                    seller_id = sellerId,
                    content = content
                )

                val response = RetrofitInstance.api.sendMessageAsBuyer(request)

                android.util.Log.d("CHAT_DEBUG", "Servidor confirmó envío: ${response.id}")
                onComplete()
        }
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

   fun trackTransactionCompleted() {
        val state   = uiState.value as? ListingDetailUiState.Success ?: return
        val listing = state.listing
        val seller  = state.seller

        BusinessAnalyticsProvider.tracker.trackTransactionCompleted(
            listingId = listing.id,
            sellerId  = listing.seller_id,
            metadata  = mapOf(
                "entrypoint"    to "detail_screen",
                "category"      to (listing.category ?: "unknown"),
                "product"       to listing.product,
                "selling_price" to listing.selling_price.toString(),
                "semester"      to (seller?.semester?.toString() ?: "unknown")
            )
        )
    }
}

class ListingDetailViewModelFactory(
    private val listingId: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ListingDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ListingDetailViewModel(listingId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
