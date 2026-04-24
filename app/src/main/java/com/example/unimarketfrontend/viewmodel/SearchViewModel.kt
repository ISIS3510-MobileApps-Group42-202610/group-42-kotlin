package com.example.unimarketfrontend.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarketfrontend.model.network.client.RetrofitInstance
import com.example.unimarketfrontend.model.listing.Listing
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {
//LISTINGS ACTÚA CÓMO UN CACHE LOCAL PARA QUE SE PUEDA BUSCAR Y NO SATURE EL SERVER

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query


    // Usa directamente la lista de listings para poder filtrar por título, categoría y condición
    //USA EL MUTABLE FLOW OSEA QUE ES UN PATRON OBSERVER PARA PODER VER CUANDO UN ESTADO CAMBIA
    private val _results = MutableStateFlow<List<Listing>>(emptyList())
    //SE USA PATRON OBSERVER, EL RESULTS ES LA LISTA FILTRADA DE LA COPIA LOCAL
    //MUESTRA EL RESULTADO DE LA BUSQUEDA POR TÍTULO
    val results: StateFlow<List<Listing>> = _results

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading


    //EL ALL LISTINGS ES GUARDAR TODOS LOS PRODUCTOS EN LA BD CUANDO SE ABRE PANTALLA
    private var allListings: List<Listing> = emptyList()
    //ESTE USA LA LÍNEA DE LOS LISTINGS INTELIGENTE, MUESTRA EL RESULTADO DE LA BUSQUEDA POR CATEGORÍA
    // MUESTRA EL RESULTADO DE LA BUSQUEDA POR CONDICIÓN
    private var debounceJob: Job? = null

    init {
        fetchAllListings()
    }


    // AGRUPA TODOS LOS PRODUCTOS Y LANZA
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