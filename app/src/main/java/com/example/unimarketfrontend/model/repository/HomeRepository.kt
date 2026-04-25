package com.example.unimarketfrontend.model.repository

import com.example.unimarketfrontend.model.local.dao.ListingDao
import com.example.unimarketfrontend.model.mappers.toEntities
import com.example.unimarketfrontend.model.mappers.toListing
import com.example.unimarketfrontend.model.network.api.ApiService
import com.example.unimarketfrontend.model.network.client.RetrofitInstance
import com.example.unimarketfrontend.model.listing.HomeResponseDto
import com.example.unimarketfrontend.model.listing.Listing

class HomeRepository(
    private val api: ApiService = RetrofitInstance.api,
    private val listingDao: ListingDao
) {

    private suspend fun preserveLocalInactiveState(remoteListing: Listing): Listing {
        val cached = listingDao.getById(remoteListing.id)
        return if (cached != null && !cached.active) {
            remoteListing.copy(active = false)
        } else {
            remoteListing
        }
    }

    suspend fun getCachedActiveListings(): List<Listing> {
        return listingDao.getActive().map { it.toListing() }
    }

    suspend fun refreshHomeData(): HomeResponseDto {
        val remote = api.getHomeRanking()
        val allRemoteListings = (remote.trending + remote.recent).distinctBy { it.id }
        val mergedListings = allRemoteListings.map { preserveLocalInactiveState(it) }

        if (mergedListings.isNotEmpty()) {
            listingDao.upsertAll(mergedListings.toEntities())
        }

        return remote.copy(
            trending = remote.trending.map { preserveLocalInactiveState(it) },
            recent = remote.recent.map { preserveLocalInactiveState(it) }
        )
    }
}