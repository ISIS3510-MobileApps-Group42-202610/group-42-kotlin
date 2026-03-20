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
        val trending: List<Listing>,
        val recent: List<Listing>,
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
        viewModelScope.launch {
            try {
                val user = RetrofitInstance.api.getMe()
                val homeResponse = RetrofitInstance.api.getHomeRanking()

                val categoriesUi = homeResponse.categories.map { categoryDto ->
                    CategoryUi(
                        name = categoryDto.category,
                        count = categoryDto.count
                    )
                }

                _uiState.value = HomeUiState.Success(
                    userName = user.name,
                    trending = homeResponse.trending,
                    recent = homeResponse.recent,
                    categories = categoriesUi
                )

            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = HomeUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}