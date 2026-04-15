package com.example.unimarketfrontend.repository

import com.example.unimarketfrontend.local.dao.ListingDao
import com.example.unimarketfrontend.local.entity.toEntities
import com.example.unimarketfrontend.local.entity.toListing
import com.example.unimarketfrontend.network.ApiService
import com.example.unimarketfrontend.network.RetrofitInstance
import com.example.unimarketfrontend.network.model.HomeResponseDto
import com.example.unimarketfrontend.network.model.Listing

class HomeRepository(
    private val api: ApiService = RetrofitInstance.api,
    private val listingDao: ListingDao
) {

    suspend fun getCachedActiveListings(): List<Listing> {
        return listingDao.getActive().map { it.toListing() }
    }

    suspend fun refreshHomeData(): HomeResponseDto {
        val remote = api.getHomeRanking()
        val allRemoteListings = (remote.trending + remote.recent).distinctBy { it.id }
        if (allRemoteListings.isNotEmpty()) {
            listingDao.upsertAll(allRemoteListings.toEntities())
        }
        return remote
    }
}