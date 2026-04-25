package com.example.unimarketfrontend.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarketfrontend.model.local.AppDatabase
import com.example.unimarketfrontend.model.repository.ListingRepository
import com.example.unimarketfrontend.model.listing.Listing
import com.example.unimarketfrontend.model.utils.ErrorTranslator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ManageProductsState {
    object Loading : ManageProductsState()
    data class Success(
        val active: List<Listing>,
        val sold: List<Listing>
    ) : ManageProductsState()
    data class Error(val message: String) : ManageProductsState()
}

class ManageProductsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ListingRepository(
        listingDao = AppDatabase.getInstance(application).listingDao()
    )

    private val _state =
        MutableStateFlow<ManageProductsState>(ManageProductsState.Loading)

    val state: StateFlow<ManageProductsState> = _state

    init {
        loadProducts()
    }

    fun refresh() {
        loadProducts()
    }

    private fun loadProducts() {
        viewModelScope.launch {
            try {
                // Obtenemos el usuario actual a través del repositorio
                val currentUser = repository.getMe()
                val response = repository.getMyListings()

                if (response.isSuccessful) {
                    response.body()?.let { repository.cacheMyListings(it) }

                    // Ajustamos el filtro para usar owner_user_id si está presente, 
                    // de lo contrario usamos seller_id (retrocompatibilidad)
                    val allActive = repository.getCachedActiveListings()
                    val allSold = repository.getCachedSoldListings()

                    _state.value = ManageProductsState.Success(
                        active = allActive.filter { 
                            (it.owner_user_id ?: it.seller_id) == currentUser.id 
                        },
                        sold = allSold.filter { 
                            (it.owner_user_id ?: it.seller_id) == currentUser.id 
                        }
                    )
                } else {
                    val userMessage = ErrorTranslator.getUserFriendlyMessage(null)
                    _state.value = ManageProductsState.Error(userMessage)
                }
            } catch (e: Exception) {
                val userMessage = ErrorTranslator.getUserFriendlyMessage(e)
                _state.value = ManageProductsState.Error(userMessage)
            }
        }
    }
}