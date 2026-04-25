package com.example.unimarketfrontend.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarketfrontend.model.local.AppDatabase
import com.example.unimarketfrontend.model.listing.Listing
import com.example.unimarketfrontend.model.repository.ListingRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ListingRepository(
        listingDao = AppDatabase.getInstance(application).listingDao()
    )

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _results = MutableStateFlow<List<Listing>>(emptyList())
    val results: StateFlow<List<Listing>> = _results

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var allListings: List<Listing> = emptyList()
    private var debounceJob: Job? = null

    init {
        fetchAllListings()
    }

    fun refresh() {
        fetchAllListings()
    }

    private fun fetchAllListings() {
        viewModelScope.launch {
            try {
                allListings = repository.syncSearchListings()
            } catch (e: Exception) {
                allListings = repository.getCachedActiveListings()
            }

            if (_query.value.isNotBlank()) {
                _results.value = filterListings(_query.value)
            }
        }
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        debounceJob?.cancel()

        if (newQuery.isBlank()) {
            _results.value = emptyList()
            _isLoading.value = false
            return
        }

        debounceJob = viewModelScope.launch {
            delay(300)
            _isLoading.value = true
            _results.value = filterListings(newQuery)
            _isLoading.value = false
        }
    }

    private fun filterListings(query: String): List<Listing> {
        val lower = query.lowercase().trim()

        return allListings.filter { listing ->
            listing.active &&
                (
                    listing.title.lowercase().contains(lower) ||
                    listing.category?.lowercase()?.contains(lower) == true ||
                    listing.condition?.lowercase()?.contains(lower) == true ||
                    listing.product?.lowercase()?.contains(lower) == true
                )
        }
    }
}