package com.example.unimarketfrontend.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarketfrontend.network.RetrofitInstance
import com.example.unimarketfrontend.network.model.Listing
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {

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

    private fun fetchAllListings() {
        viewModelScope.launch {
            try {
                allListings = RetrofitInstance.api.getListings().body() ?: emptyList()
            } catch (e: Exception) {
                allListings = emptyList()
            }
        }
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        debounceJob?.cancel()
        if (newQuery.isBlank()) {
            _results.value = emptyList()
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
        val lower = query.lowercase()
        return allListings.filter { listing ->
            listing.title.lowercase().contains(lower) ||
                    listing.category?.lowercase()?.contains(lower) == true ||
                    listing.condition?.lowercase()?.contains(lower) == true
        }
    }
}