package com.example.unimarketfrontend.viewmodel

    import android.app.Application
    import androidx.lifecycle.AndroidViewModel
    import androidx.lifecycle.viewModelScope
    import com.example.unimarketfrontend.local.db.AppDatabase
    import com.example.unimarketfrontend.network.ConnectivityMonitor
    import com.example.unimarketfrontend.network.RetrofitInstance
    import com.example.unimarketfrontend.network.model.CategoryRankDto
    import com.example.unimarketfrontend.network.model.Listing
    import com.example.unimarketfrontend.repository.HomeRepository
    import kotlinx.coroutines.flow.MutableStateFlow
    import kotlinx.coroutines.flow.StateFlow
    import kotlinx.coroutines.flow.distinctUntilChanged
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

    class HomeViewModel(application: Application) : AndroidViewModel(application) {

        private val homeRepository = HomeRepository(
            listingDao = AppDatabase.getInstance(application).listingDao()
        )
        private val connectivityMonitor = ConnectivityMonitor(application)

        private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
        val uiState: StateFlow<HomeUiState> = _uiState

        private var isLoadingHome = false
        private var shouldRetryWhenOnline = false

        init {
            observeConnectivity()
            loadHome()
        }

        private fun observeConnectivity() {
            viewModelScope.launch {
                connectivityMonitor.isOnline
                    .distinctUntilChanged()
                    .collect { online ->
                        if (online && shouldRetryWhenOnline && !isLoadingHome) {
                            shouldRetryWhenOnline = false
                            loadHome(forceLoading = false)
                        }
                    }
            }
        }

        private fun loadHome(forceLoading: Boolean = true) {
            if (isLoadingHome) return

            viewModelScope.launch {
                isLoadingHome = true

                if (forceLoading && _uiState.value !is HomeUiState.Success) {
                    _uiState.value = HomeUiState.Loading
                }

                val userName = runCatching { RetrofitInstance.api.getMe().name }.getOrDefault("there")
                val cachedListings = homeRepository.getCachedActiveListings()

                if (cachedListings.isNotEmpty()) {
                    _uiState.value = HomeUiState.Success(
                        userName = userName,
                        trending = cachedListings,
                        recent = emptyList(),
                        categories = categoriesFromListings(cachedListings)
                    )
                }

                try {
                    val homeResponse = homeRepository.refreshHomeData()
                    _uiState.value = HomeUiState.Success(
                        userName = userName,
                        trending = homeResponse.trending.filter { it.active },
                        recent = homeResponse.recent.filter { it.active },
                        categories = homeResponse.categories.toCategoryUi()
                    )
                    shouldRetryWhenOnline = false
                } catch (e: Exception) {
                    shouldRetryWhenOnline = true
                    if (cachedListings.isEmpty()) {
                        _uiState.value = HomeUiState.Error(e.message ?: "Unable to load home data")
                    }
                } finally {
                    isLoadingHome = false
                }
            }
        }

        private fun List<CategoryRankDto>.toCategoryUi(): List<CategoryUi> {
            return map { CategoryUi(name = it.category, count = it.count) }
        }

        private fun categoriesFromListings(listings: List<Listing>): List<CategoryUi> {
            return listings
                .groupingBy { it.category ?: "Others" }
                .eachCount()
                .map { CategoryUi(name = it.key, count = it.value) }
                .sortedByDescending { it.count }
        }
    }




