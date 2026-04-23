package com.example.unimarketfrontend.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.unimarketfrontend.analytics.BusinessAnalyticsProvider
import com.example.unimarketfrontend.local.db.AppDatabase
import com.example.unimarketfrontend.network.ConnectivityMonitor
import com.example.unimarketfrontend.network.RetrofitInstance
import com.example.unimarketfrontend.network.model.Listing
import com.example.unimarketfrontend.network.model.Review
import com.example.unimarketfrontend.network.model.SendMessageRequest
import com.example.unimarketfrontend.network.model.User
import com.example.unimarketfrontend.repository.ListingRepository
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
    private val listingId: Int,
    private val listingRepository: ListingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ListingDetailUiState>(ListingDetailUiState.Loading)
    val uiState: StateFlow<ListingDetailUiState> = _uiState

    private var isLoadingListing = false
    private var shouldRetryWhenOnline = false

    init {
        observeConnectivity()
        loadListing()
    }

    private fun observeConnectivity() {
        viewModelScope.launch {
            ConnectivityMonitor.isOnline.collect { online ->
                if (online && shouldRetryWhenOnline && !isLoadingListing) {
                    shouldRetryWhenOnline = false
                    loadListing(forceLoading = false)
                }
            }
        }
    }

    private fun loadListing(forceLoading: Boolean = true) {
        if (isLoadingListing) return
        isLoadingListing = true
        val startMs = System.currentTimeMillis()

        viewModelScope.launch {
            if (forceLoading && _uiState.value !is ListingDetailUiState.Success) {
                _uiState.value = ListingDetailUiState.Loading
            }

            val cachedListing = listingRepository.getCachedById(listingId)
            if (cachedListing != null) {
                emitSuccess(cachedListing)
            }

            try {
                val remoteListing = listingRepository.refreshById(listingId)
                if (remoteListing != null) {
                    emitSuccess(remoteListing)
                } else if (cachedListing == null) {
                    _uiState.value = ListingDetailUiState.Error("Producto no encontrado")
                }
                shouldRetryWhenOnline = false
            } catch (e: Exception) {
                shouldRetryWhenOnline = true
                if (cachedListing == null) {
                    _uiState.value = ListingDetailUiState.Error("Fallo la carga: ${e.message}")
                }
            } finally {
                isLoadingListing = false
            }
        }
    }

    private suspend fun emitSuccess(listing: Listing) {
        val seller = runCatching { RetrofitInstance.api.getUserById(listing.seller_id).body() }.getOrNull()
        val reviews = runCatching { RetrofitInstance.api.getReviewsByListing(listing.id).body() }.getOrDefault(emptyList()) ?: emptyList()

        _uiState.value = ListingDetailUiState.Success(
            listing = listing,
            seller = seller,
            reviews = reviews,
            ratingSummary = RatingSummaryUi(reviews.mapNotNull { it.rating }.average().takeIf { !it.isNaN() } ?: 0.0, reviews.size)
        )
    }

    fun sendFirstMessage(sellerId: Int, content: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                val request = SendMessageRequest(seller_id = sellerId, content = content)
                RetrofitInstance.api.sendMessageAsBuyer(request)
                onComplete()
            } catch (e: Exception) {
                android.util.Log.e("ERROR", "No se pudo mandar el mensaje")
            }
        }
    }
}
class ListingDetailViewModelFactory(
    private val application: Application,
    private val listingId: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repository = ListingRepository(
            listingDao = AppDatabase.getInstance(application).listingDao()
        )
        return ListingDetailViewModel(listingId, repository) as T
    }
}