package com.example.unimarketfrontend.ui.components.factory

import com.example.unimarketfrontend.network.model.Listing

interface ListingCardPresentation {
    val imageUrl: String
    val title: String
    val price: String
    val condition: String
    val listing: Listing
}

class ListingCardPresentationImpl(override val listing: Listing) : ListingCardPresentation {
    override val imageUrl: String
        get() = listing.images?.firstOrNull()?.url ?: ""

    override val title: String
        get() = listing.title

    override val price: String
        get() = "$${listing.selling_price}"

    override val condition: String
        get() = listing.condition ?: "Unknown"
}

object ListingCardFactory {
    fun createPresentation(listing: Listing): ListingCardPresentation {
        return ListingCardPresentationImpl(listing)
    }
}

