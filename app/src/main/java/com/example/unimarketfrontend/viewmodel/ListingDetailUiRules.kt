package com.example.unimarketfrontend.viewmodel

import com.example.unimarketfrontend.model.listing.Listing
import com.example.unimarketfrontend.model.listing.Review
import com.example.unimarketfrontend.model.user.User

data class ListingDetailActionState(
    val sellerDisplayName: String,
    val contactTargetId: Int?,
    val contactTargetName: String?,
    val contactActionLabel: String,
    val isOwner: Boolean,
    val canMarkAsSold: Boolean,
    val hasExternalComments: Boolean,
    val externalCommentsCount: Int
)

private fun Review.authorUserId(): Int? {
    return user?.id ?: reviewer_id ?: user_id
}

fun buildListingDetailActionState(
    listing: Listing,
    seller: User?,
    buyer: User?,
    currentUser: User?,
    reviews: List<Review>
): ListingDetailActionState {
    val sellerDisplayName = seller?.let { "${it.name} ${it.last_name}".trim() }
        ?.takeIf { it.isNotBlank() }
        ?: "Usuario #${listing.seller_id}"

    val sellerActualUserId = listing.owner_user_id ?: seller?.id ?: -1
    val isOwner = currentUser?.id != null && currentUser.id == sellerActualUserId
    
    val contactTarget = when {
        isOwner -> buyer
        currentUser?.id == listing.buyer_id -> seller
        else -> seller
    }
    
    val contactTargetName = contactTarget?.let { "${it.name} ${it.last_name}".trim() }
        ?.takeIf { it.isNotBlank() }
        
    val contactActionLabel = when {
        isOwner && buyer != null -> "Contactar comprador"
        isOwner -> "Sin comprador"
        else -> "Contactar vendedor"
    }
    
    val externalCommentsCount = reviews.count { review ->
        val authorId = review.authorUserId()
        authorId != null && authorId != sellerActualUserId
    }

    return ListingDetailActionState(
        sellerDisplayName = sellerDisplayName,
        contactTargetId = contactTarget?.id,
        contactTargetName = contactTargetName,
        contactActionLabel = contactActionLabel,
        isOwner = isOwner,
        canMarkAsSold = isOwner && listing.active,
        hasExternalComments = externalCommentsCount > 0,
        externalCommentsCount = externalCommentsCount
    )
}
