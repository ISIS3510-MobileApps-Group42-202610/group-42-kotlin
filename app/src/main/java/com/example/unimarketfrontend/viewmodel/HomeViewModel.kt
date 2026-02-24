package com.example.unimarketfrontend.viewmodel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarketfrontend.network.RetrofitInstance
import com.example.unimarketfrontend.network.model.Listing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(
        val userName: String,
        val listings: List<Listing>,
        val categories: List<CategoryUi>
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

data class CategoryUi(
    val name: String,
    val count: Int
)

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        loadHome()
    }
    private fun loadHome() {
        //Si falla el lanzamiento devuelve error, se carga user listings, categorias.
        viewModelScope.launch {
            try {
                val user = RetrofitInstance.api.getMe()
                val listings = RetrofitInstance.api.getListings()

                val categories = listings
                    .groupBy { it.category ?: "Other"}
                    .map { entry ->
                        CategoryUi(
                            name = entry.key, count = entry.value.size) }

                _uiState.value = HomeUiState.Success(
                    userName = user.name,
                    listings = listings.take(15),
                    categories = categories
                )
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error("Failed to load home")
            }
        }
    }
}