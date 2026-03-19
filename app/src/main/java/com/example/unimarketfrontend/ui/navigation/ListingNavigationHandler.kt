package com.example.unimarketfrontend.ui.navigation

import androidx.navigation.NavController

object ListingRoutesFactory {
    const val DETAIL_ROUTE = "listingDetail/{listingId}"
    fun detailPath(listingId: Int): String = "listingDetail/$listingId"
}

interface ListingNavigationHandler {
    fun navigateToListing(listingId: Int)
    fun navigateBack()
}

class ListingNavigationHandlerImpl(
    private val navController: NavController
) : ListingNavigationHandler {

    override fun navigateToListing(listingId: Int) {
        navController.navigate(ListingRoutesFactory.detailPath(listingId))
    }

    override fun navigateBack() {
        navController.popBackStack()
    }
}

fun NavController.createListingNavigationHandler(): ListingNavigationHandler {
    return ListingNavigationHandlerImpl(this)
}
