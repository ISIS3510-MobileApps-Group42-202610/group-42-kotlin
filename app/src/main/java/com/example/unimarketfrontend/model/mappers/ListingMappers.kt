package com.example.unimarketfrontend.model.mappers

import com.example.unimarketfrontend.model.listing.Listing
import com.example.unimarketfrontend.model.listing.ListingImage
import com.example.unimarketfrontend.model.local.ListingEntity

fun Listing.toEntity(): ListingEntity {
    val primaryUrl = images?.firstOrNull { it.is_primary }?.url ?: images?.firstOrNull()?.url

    return ListingEntity(
        id = id,
        sellerId = seller_id,
        buyerId = buyer_id,
        courseId = course_id,
        title = title,
        product = product,
        category = category,
        condition = condition,
        originalPrice = original_price,
        sellingPrice = selling_price,
        createdAt = created_at,
        updatedAt = updated_at,
        active = active,
        primaryImageUrl = primaryUrl
    )
}

fun List<Listing>.toEntities(): List<ListingEntity> = map { it.toEntity() }

fun ListingEntity.toListing(): Listing {
    return Listing(
        id = id,
        seller_id = sellerId,
        buyer_id = buyerId,
        course_id = courseId,
        title = title,
        product = product,
        category = category,
        condition = condition,
        original_price = originalPrice,
        selling_price = sellingPrice,
        created_at = createdAt,
        updated_at = updatedAt,
        active = active,
        images = primaryImageUrl?.let {
            listOf(
                ListingImage(
                    id = 0,
                    listing_id = id,
                    is_primary = true,
                    uploaded_at = createdAt,
                    url = it
                )
            )
        },
        priceHistory = emptyList()
    )
}

fun List<ListingEntity>.toListings(): List<Listing> = map { it.toListing() }
