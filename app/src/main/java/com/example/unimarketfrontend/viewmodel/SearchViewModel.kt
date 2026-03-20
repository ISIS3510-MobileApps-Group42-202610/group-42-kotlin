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

    // El texto que el usuario está escribiendo
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    // Los resultados filtrados de la búsqueda
    private val _results = MutableStateFlow<List<Listing>>(emptyList())
    val results: StateFlow<List<Listing>> = _results

    // Si está procesando una búsqueda en este momento
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Guarda TODOS los listings del backend en memoria
    // Así no tenemos que llamar al backend en cada letra que el usuario escribe
    private var allListings: List<Listing> = emptyList()

    // Job es una referencia a una coroutine en ejecución
    // La guardamos para poder cancelarla si el usuario escribe otra letra antes
    // de que termine el delay de 300ms
    private var debounceJob: Job? = null

    init {
        // Al crear el ViewModel, carga todos los listings una sola vez
        fetchAllListings()
    }

    // Descarga todos los listings y los guarda en memoria
    private fun fetchAllListings() {
        viewModelScope.launch {
            try {
                // getListings() devuelve Response<List<Listing>>
                // .body() extrae la lista del response, o null si falló
                // ?: emptyList() usa lista vacía si body() es null
                allListings = RetrofitInstance.api.getListings().body() ?: emptyList()
            } catch (e: Exception) {
                allListings = emptyList()
            }
        }
    }

    // Se llama cada vez que el usuario escribe una letra
    fun onQueryChange(newQuery: String) {
        _query.value = newQuery

        // Cancela la búsqueda anterior si todavía no terminó
        // Esto se llama "debounce": esperar a que el usuario pare de escribir
        debounceJob?.cancel()

        // Si el campo quedó vacío, limpia los resultados y termina
        if (newQuery.isBlank()) {
            _results.value = emptyList()
            return
        }

        // Lanza una nueva búsqueda con 300ms de espera
        debounceJob = viewModelScope.launch {
            delay(300) // espera 300ms — si el usuario escribe otra letra, esta línea nunca termina porque el job se cancela arriba
            _isLoading.value = true
            _results.value = filterListings(newQuery)
            _isLoading.value = false
        }
    }

    // Filtra la lista en memoria según el texto buscado
    // .lowercase() convierte a minúsculas para que la búsqueda no sea case-sensitive
    // .contains(lower) devuelve true si el texto contiene la cadena buscada
    private fun filterListings(query: String): List<Listing> {
        val lower = query.lowercase()
        return allListings.filter { listing ->
            listing.title.lowercase().contains(lower) ||
                    listing.category?.lowercase()?.contains(lower) == true ||
                    listing.condition?.lowercase()?.contains(lower) == true
        }
    }
}