package com.example.unimarketfrontend.model.repository

import com.example.unimarketfrontend.model.local.dao.ListingDao
import com.example.unimarketfrontend.model.mappers.toEntities
import com.example.unimarketfrontend.model.mappers.toEntity
import com.example.unimarketfrontend.model.mappers.toListing
import com.example.unimarketfrontend.model.network.api.ApiService
import com.example.unimarketfrontend.model.network.client.RetrofitInstance
import com.example.unimarketfrontend.model.listing.*
import com.example.unimarketfrontend.model.message.SendMessageRequest
import com.example.unimarketfrontend.model.uploads.CloudinarySignatureRequest
import com.example.unimarketfrontend.model.user.User
import retrofit2.Response

/*
 * Clase de resultado dual para la estrategia Cache-then-Network.
 * Permite que la UI reciba el cache rapido y luego la actualizacion de red.
 */
data class ListingCacheThenNetworkResult(
    val cached: Listing?,
    val remote: Listing?,
    val networkError: Throwable?
)

/*
 * Repositorio Maestro de Listings.
 * SPRINT 3: Actua como una Fachada (Facade) para ocultar la complejidad de red y DB.
 */
class ListingRepository(
    private val listingDao: ListingDao,
    private val api: ApiService = RetrofitInstance.api
) {

    // --- LOGICA DE SINCRONIZACION Y PERSISTENCIA ---

    // Evitamos el antipatron de inconsistencia: si un listing es localmente 'sold', no lo revivimos
    private suspend fun preserveLocalInactiveState(remoteListing: Listing): Listing {
        val cached = listingDao.getById(remoteListing.id)
        return if (cached != null && !cached.active) {
            remoteListing.copy(active = false)
        } else {
            remoteListing
        }
    }

    // Guarda una lista de listings en Room de forma segura
    suspend fun cacheRemoteListings(listings: List<Listing>) {
        val mergedListings = listings.map { preserveLocalInactiveState(it) }
        if (mergedListings.isNotEmpty()) {
            listingDao.upsertAll(mergedListings.toEntities())
        }
    }

    // --- CONSULTAS DE CACHE (Local Storage) ---

    suspend fun getCachedById(listingId: Int): Listing? {
        return listingDao.getById(listingId)?.toListing()
    }

    suspend fun getCachedActiveListings(): List<Listing> {
        return listingDao.getActive().map { it.toListing() }
    }

    suspend fun getCachedSoldListings(): List<Listing> {
        return listingDao.getAll().filter { !it.active }.map { it.toListing() }
    }

    // --- LLAMADAS DE RED (Network Layer) ---

    suspend fun refreshById(listingId: Int): Listing? {
        // Intento 1: Endpoint especifico por ID
        return runCatching {
            val response = api.getListingById(listingId)
            if (response.isSuccessful && response.body() != null) {
                val remoteListing = response.body()!!
                val mergedListing = preserveLocalInactiveState(remoteListing)
                listingDao.upsert(mergedListing.toEntity())
                return@runCatching mergedListing
            }
            null
        }.getOrNull() ?: run {
            // Fallback: Busqueda manual en la lista global (en caso de que falle el endpoint /id)
            val response = api.getListings()
            val remoteListing = response.body()?.firstOrNull { it.id == listingId } ?: return null
            val mergedListing = preserveLocalInactiveState(remoteListing)
            listingDao.upsert(mergedListing.toEntity())
            return mergedListing
        }
    }

    suspend fun refreshHomeData(): HomeResponseDto {
        val remote = api.getHomeRanking()
        // Procesamos tendencias y recientes para guardarlos en Room
        val allRemoteListings = (remote.trending + remote.recent).distinctBy { it.id }
        cacheRemoteListings(allRemoteListings)

        return remote.copy(
            trending = remote.trending.map { preserveLocalInactiveState(it) },
            recent = remote.recent.map { preserveLocalInactiveState(it) }
        )
    }

    // Implementacion completa de Cache-then-Network (Ch. 10 del libro)
    suspend fun getByIdCacheThenNetwork(listingId: Int): ListingCacheThenNetworkResult {
        val cached = getCachedById(listingId)
        return try {
            val remote = refreshById(listingId)
            ListingCacheThenNetworkResult(cached, remote, null)
        } catch (e: Exception) {
            ListingCacheThenNetworkResult(cached, null, e)
        }
    }

    // --- INTEGRACION CON BUSQUEDA ---

    suspend fun searchListings() = api.getListings()

    suspend fun syncSearchListings(): List<Listing> {
        val response = api.getListings()
        val body = response.body().orEmpty()
        if (response.isSuccessful && body.isNotEmpty()) {
            cacheRemoteListings(body)
        }
        return getCachedActiveListings()
    }

    // --- SERVICIOS EXTERNOS Y OTROS ---

    suspend fun getSellerInfo(userId: Int): Response<User> = api.getUserById(userId)

    suspend fun getMe(): User = api.getMe()

    suspend fun getReviews(listingId: Int): Response<List<Review>> = api.getReviewsByListing(listingId)

    suspend fun getMyListings() = api.getMyListings()

    // Envio de mensajes a traves del repositorio de listings (Shortcut para UX)
    suspend fun sendMessage(sellerId: Int, content: String) =
        api.sendMessageAsBuyer(SendMessageRequest(seller_id = sellerId, content = content))

    suspend fun createListing(request: CreateListingRequest) = api.createListing(request)

    suspend fun deleteListing(listingId: Int): Response<Unit> = api.deleteListing(listingId)

    suspend fun getCloudinarySignature(request: CloudinarySignatureRequest) =
        api.getCloudinarySignature(request)
}