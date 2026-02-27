package com.example.unimarketfrontend.network
import com.example.unimarketfrontend.network.model.LoginResponse
import com.example.unimarketfrontend.network.model.LoginRequest
import com.example.unimarketfrontend.network.model.Listing
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import com.example.unimarketfrontend.network.model.User
import com.example.unimarketfrontend.network.model.Message


interface ApiService {
    @POST("api/v1/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): LoginResponse

    @GET("api/v1/listings")
    suspend fun getListings(): List<Listing>

    @GET("api/v1/users/me")
    suspend fun getMe(): User
}
// TODO: Revisar cómo funciona el bsackend y conectar
// @GET("api/v1/messages/as-buyer")
// suspend fun getMessagesAsBuyer(): List<Message>
//}
//    @POST("api/v1/messages")
//    suspend fun sendMessage(
//        @Body request: MessageRequest
//    ): MessageResponse
