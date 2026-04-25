package com.example.unimarketfrontend.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarketfrontend.model.local.AppDatabase
import com.example.unimarketfrontend.model.utils.ConnectivityMonitor
import com.example.unimarketfrontend.model.utils.ErrorTranslator
import com.example.unimarketfrontend.model.network.client.RetrofitInstance
import com.example.unimarketfrontend.model.listing.Listing
import com.example.unimarketfrontend.model.repository.HomeRepository
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

/*
 * ViewModel de la Home para el Sprint 3.
 * Aca manejamos la logica de "Cache-then-Network" para que la app cargue rapido.
 * Si no hay red, mostramos los productos guardados en Room.
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val homeRepository = HomeRepository(
        listingDao = AppDatabase.getInstance(application).listingDao()
    )

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState

    private var isLoadingHome = false
    private var shouldRetryWhenOnline = false

    init {
        observeConnectivity()
        loadHome()
    }

    fun refreshHome() {
        loadHome(forceLoading = false)
    }

    // Escuchamos al monitor de red para saber si volvio el internet
    private fun observeConnectivity() {
        viewModelScope.launch {
            // Usamos el objeto directamente sin instanciar
            ConnectivityMonitor.isOnline.collect { online ->
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

            // Sacamos el nombre del usuario logueado para saludarlo
            val userName = runCatching { RetrofitInstance.api.getMe().name }.getOrDefault("there")

            // Primero mostramos lo que hay en el celular (Cache local)
            val cachedListings = homeRepository.getCachedActiveListings()

            if (cachedListings.isNotEmpty()) {
                _uiState.value = HomeUiState.Success(
                    userName = userName,
                    trending = cachedListings,
                    recent = emptyList(),
                    categories = categoriesFromListings(cachedListings)
                )
            }

            // Intentamos bajar los datos mas frescos del servidor
            try {
                val homeResponse = homeRepository.refreshHomeData()
                _uiState.value = HomeUiState.Success(
                    userName = userName,
                    trending = homeResponse.trending.filter { it.active },
                    recent = homeResponse.recent.filter { it.active },
                    categories = homeResponse.categories.map { CategoryUi(it.category, it.count) }
                )
                shouldRetryWhenOnline = false
            } catch (e: Exception) {
                shouldRetryWhenOnline = true
                // Si Room esta vacio y falla el internet, mostramos error amigable
                if (cachedListings.isEmpty()) {
                    val userMessage = ErrorTranslator.getUserFriendlyMessage(e)
                    _uiState.value = HomeUiState.Error(userMessage)
                }
            } finally {
                isLoadingHome = false
            }
        }
    }

    private fun categoriesFromListings(listings: List<Listing>): List<CategoryUi> {
        return listings
            .groupingBy { it.category ?: "Others" }
            .eachCount()
            .map { CategoryUi(name = it.key, count = it.value) }
            .sortedByDescending { it.count }
    }
}
