package com.example.unimarketfrontend.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarketfrontend.analytics.AnalyticsLogger
import com.example.unimarketfrontend.network.RetrofitInstance
import com.example.unimarketfrontend.network.model.Category
import com.example.unimarketfrontend.network.model.Listing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(
        val userName: String,
        val listings: List<Listing>,
        val categories: List<Category>
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

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
                val ranking = RetrofitInstance.api.getHomeRanking()

                // Map the category counts map to a list of Category objects
                val categories = ranking.categories.map { (name, count) ->
                    Category(
                        name = name.replaceFirstChar { it.uppercase() },
                        count = count
                    )
                }

                // Register user ID for analytics events
                AnalyticsLogger.setUserId(user.id ?: 0)
                AnalyticsLogger.log("home_screen_opened")

                _uiState.value = HomeUiState.Success(
                    userName = user.name ?: "Student",
                    listings = ranking.trending,
                    categories = categories
                )
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}