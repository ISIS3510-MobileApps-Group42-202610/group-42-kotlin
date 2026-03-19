package com.example.unimarketfrontend.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarketfrontend.network.RetrofitInstance
import com.example.unimarketfrontend.network.model.Listing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.Response

sealed class ManageProductsState {
    object Loading : ManageProductsState()
    data class Success(
        val active: List<Listing>,
        val sold: List<Listing>
    ) : ManageProductsState()
    data class Error(val message: String) : ManageProductsState()
}

class ManageProductsViewModel : ViewModel() {

    private val _state =
        MutableStateFlow<ManageProductsState>(ManageProductsState.Loading)

    val state: StateFlow<ManageProductsState> = _state

    init {
        loadProducts()
    }

    private fun loadProducts() {
        viewModelScope.launch {
            try {

                val response =
                    RetrofitInstance.api.getMyListings()

                if (response.isSuccessful) {

                    val body = response.body()

                    _state.value = ManageProductsState.Success(
                        active = body?.active ?: emptyList(),
                        sold = body?.sold ?: emptyList()
                    )

                } else {

                    _state.value = ManageProductsState.Error(
                        "HTTP ${response.code()}"
                    )
                }

            } catch (e: Exception) {

                _state.value = ManageProductsState.Error(
                    "Exception: ${e.localizedMessage}"
                )
            }
        }
    }
}