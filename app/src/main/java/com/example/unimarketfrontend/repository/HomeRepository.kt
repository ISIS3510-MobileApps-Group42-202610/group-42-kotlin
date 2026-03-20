package com.example.unimarketfrontend.repository


import com.example.unimarketfrontend.network.model.HomeResponseDto
import com.example.unimarketfrontend.network.RetrofitInstance

class HomeRepository {

    suspend fun getHomeData(): HomeResponseDto {
        return RetrofitInstance.api.getHomeRanking()
    }
}