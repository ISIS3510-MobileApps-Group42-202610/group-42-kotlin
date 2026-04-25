package com.example.unimarketfrontend.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.unimarketfrontend.model.local.AppDatabase
import com.example.unimarketfrontend.model.utils.ConnectivityMonitor
import com.example.unimarketfrontend.model.utils.ErrorTranslator
import com.example.unimarketfrontend.model.network.client.RetrofitInstance
import com.example.unimarketfrontend.model.listing.Listing
import com.example.unimarketfrontend.model.listing.Review
import com.example.unimarketfrontend.model.message.SendMessageRequest
import com.example.unimarketfrontend.model.user.User
import com.example.unimarketfrontend.model.repository.ListingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
        // Usamos owner_user_id si existe, si no, retrocedemos a seller_id (comportamiento anterior)
        val sellerUserId = listing.owner_user_id ?: listing.seller_id
        
        val seller = runCatching { RetrofitInstance.api.getUserById(sellerUserId).body() }.getOrNull()
        val buyer = listing.buyer_id?.let { buyerId ->
            runCatching { RetrofitInstance.api.getUserById(buyerId).body() }.getOrNull()
        }
        val currentUser = runCatching { RetrofitInstance.api.getMe() }.getOrNull()
        val reviews = runCatching { RetrofitInstance.api.getReviewsByListing(listing.id).body() }.getOrDefault(emptyList()) ?: emptyList()
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
            ratingSummary = RatingSummaryUi(reviews.mapNotNull { it.rating }.average().takeIf { !it.isNaN() } ?: 0.0, reviews.size)
        )
    }

    fun markAsSold(onComplete: () -> Unit, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            val currentState = _uiState.value as? ListingDetailUiState.Success
                ?: return@launch onError("No se pudo actualizar la publicación")

            if (!currentState.isOwner) {
                onError("Solo el vendedor puede marcarla como vendida")
                return@launch
            }

            if (!currentState.canMarkAsSold) {
                onError("La publicación ya no está activa")
                return@launch
            }

            try {
                listingRepository.markAsSoldLocally(listingId)
                loadListing(forceLoading = false)
                onComplete()
            } catch (e: Exception) {
                onError(e.message ?: "No se pudo marcar como vendida")
            }
        }
    }

    fun deleteListing(onComplete: () -> Unit, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            val currentState = _uiState.value as? ListingDetailUiState.Success
                ?: return@launch onError("No se pudo obtener el estado actual")

            if (!currentState.isOwner) {
                onError("Solo el dueño puede eliminar la publicación")
                return@launch
            }

            try {
                val response = RetrofitInstance.api.deleteListing(listingId)
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

    fun sendFirstMessage(recipientUserId: Int, content: String, onComplete: () -> Unit, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val request = SendMessageRequest(seller_id = recipientUserId, content = content.trim())
                RetrofitInstance.api.sendMessageAsBuyer(request)
                onComplete()
            } catch (e: Exception) {
                android.util.Log.e("ERROR", "No se pudo mandar el mensaje", e)
                val userMessage = ErrorTranslator.getUserFriendlyMessage(e)
                onError(userMessage)
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