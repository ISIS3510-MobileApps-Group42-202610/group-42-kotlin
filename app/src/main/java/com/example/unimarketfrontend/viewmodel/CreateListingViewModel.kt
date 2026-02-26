package com.example.unimarketfrontend.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarketfrontend.network.RetrofitInstance
import com.example.unimarketfrontend.network.model.CreateListingRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.Response
import com.example.unimarketfrontend.network.model.Listing

sealed class CreateListingState {
    object Idle : CreateListingState()
    object Loading : CreateListingState()
    object Success : CreateListingState()
    data class Error(val message: String) : CreateListingState()
}

class CreateListingViewModel : ViewModel() {

    private val _state =
        MutableStateFlow<CreateListingState>(CreateListingState.Idle)

    val state: StateFlow<CreateListingState> = _state

    fun createListing(request: CreateListingRequest) {

        viewModelScope.launch {

            _state.value = CreateListingState.Loading

            try {

                val response: Response<Listing> =
                    RetrofitInstance.api.createListing(request)

                if (response.isSuccessful) {
                    _state.value = CreateListingState.Success
                } else {
                    _state.value =
                        CreateListingState.Error("Error ${response.code()}")
                }

            } catch (e: Exception) {
                _state.value =
                    CreateListingState.Error(e.message ?: "Network error")
            }
        }
    }
}