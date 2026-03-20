package com.example.unimarketfrontend.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimarketfrontend.network.RetrofitInstance
import com.example.unimarketfrontend.network.model.Listing
import com.example.unimarketfrontend.network.model.Purchase
import com.example.unimarketfrontend.network.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val _user      = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user

    private val _wishlist  = MutableStateFlow<List<Listing>>(emptyList())
    val wishlist: StateFlow<List<Listing>> = _wishlist

    private val _purchases = MutableStateFlow<List<Purchase>>(emptyList())
    val purchases: StateFlow<List<Purchase>> = _purchases

    private val _following = MutableStateFlow<List<User>>(emptyList())
    val following: StateFlow<List<User>> = _following

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMsg  = MutableStateFlow<String?>(null)
    val errorMsg: StateFlow<String?> = _errorMsg

    init { loadProfile() }

    fun loadProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMsg.value  = null
            try {
                _user.value = RetrofitInstance.api.getMe()
            } catch (e: Exception) {
                _errorMsg.value = "Error getting user: ${e.message}"
            }
            try {
                _wishlist.value = RetrofitInstance.api.getWishlist()
            } catch (e: Exception) {

            }
            try {
                val response = RetrofitInstance.api.getPurchases()
                _purchases.value = response.purchases ?: emptyList()
            } catch (e: Exception) {
            }
            try {
                _following.value = RetrofitInstance.api.getFollowing()
            } catch (e: Exception) {
            }
            _isLoading.value = false
        }
    }

    fun removeFromWishlist(listingId: Int) {
        viewModelScope.launch {
            try {
                RetrofitInstance.api.removeFromWishlist(listingId)
                _wishlist.value = _wishlist.value.filter { it.id != listingId }
            } catch (e: Exception) {
                _errorMsg.value = "Error trying to delete from wishlist"
            }
        }
    }
}