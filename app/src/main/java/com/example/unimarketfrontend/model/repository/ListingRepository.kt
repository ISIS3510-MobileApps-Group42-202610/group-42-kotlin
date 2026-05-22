package com.example.unimarketfrontend.model.repository

import com.example.unimarketfrontend.model.course.Course
import com.example.unimarketfrontend.model.local.dao.CourseDao
import com.example.unimarketfrontend.model.local.dao.ListingDao
import com.example.unimarketfrontend.model.mappers.toDomainCourses
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

data class ListingCacheThenNetworkResult(
    val cached: Listing?,
    val remote: Listing?,
    val networkError: Throwable?
)

class ListingRepository(
    private val listingDao: ListingDao,
    private val api: ApiService = RetrofitInstance.api
) {

    private suspend fun preserveLocalInactiveState(remoteListing: Listing): Listing {
        val cached = listingDao.getById(remoteListing.id)
        return if (cached != null && !cached.active) {
            remoteListing.copy(active = false)
        } else {
            remoteListing
        }
    }

    suspend fun cacheRemoteListings(listings: List<Listing>) {
        val mergedListings = listings.map { preserveLocalInactiveState(it) }
        if (mergedListings.isNotEmpty()) {
            listingDao.upsertAll(mergedListings.toEntities())
        }
    }

    suspend fun getCachedById(listingId: Int): Listing? {
        return listingDao.getById(listingId)?.toListing()
    }

    suspend fun getCachedActiveListings(): List<Listing> {
        return listingDao.getActive().map { it.toListing() }
    }

    suspend fun getCachedSoldListings(): List<Listing> {
        return listingDao.getAll().filter { !it.active }.map { it.toListing() }
    }

    suspend fun refreshById(listingId: Int): Listing? {
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
            val response = api.getListings()
            val remoteListing = response.body()?.firstOrNull { it.id == listingId } ?: return null
            val mergedListing = preserveLocalInactiveState(remoteListing)
            listingDao.upsert(mergedListing.toEntity())
            return mergedListing
        }
    }

    suspend fun refreshHomeData(): HomeResponseDto {
        val remote = api.getHomeRanking()
        val allRemoteListings = (remote.trending + remote.recent).distinctBy { it.id }
        cacheRemoteListings(allRemoteListings)

        return remote.copy(
            trending = remote.trending.map { preserveLocalInactiveState(it) },
            recent = remote.recent.map { preserveLocalInactiveState(it) }
        )
    }

    suspend fun getByIdCacheThenNetwork(listingId: Int): ListingCacheThenNetworkResult {
        val cached = getCachedById(listingId)
        return try {
            val remote = refreshById(listingId)
            ListingCacheThenNetworkResult(cached, remote, null)
        } catch (e: Exception) {
            ListingCacheThenNetworkResult(cached, null, e)
        }
    }

    suspend fun searchListings() = api.getListings()

    suspend fun syncSearchListings(): List<Listing> {
        val response = api.getListings()
        val body = response.body().orEmpty()
        if (response.isSuccessful && body.isNotEmpty()) {
            cacheRemoteListings(body)
        }
        return getCachedActiveListings()
    }

    suspend fun getSellerInfo(userId: Int): Response<User> = api.getUserById(userId)

    suspend fun getMe(): User = api.getMe()

    suspend fun getReviews(listingId: Int): Response<List<Review>> = api.getReviewsByListing(listingId)

    suspend fun getMyListings() = api.getMyListings()

    suspend fun sendMessage(sellerId: Int, content: String) =
        api.sendMessageAsBuyer(SendMessageRequest(seller_id = sellerId, content = content))

    suspend fun markAsSoldLocally(listingId: Int): Listing? {
        val cachedListing = listingDao.getById(listingId)?.toListing() ?: return null
        val soldListing = cachedListing.copy(active = false)
        listingDao.upsert(soldListing.toEntity())
        return soldListing
    }

    suspend fun markAsSoldRemotely(listingId: Int): Response<Unit> {
        return api.deleteListing(listingId)
    }

    suspend fun createListing(request: CreateListingRequest) = api.createListing(request)

    suspend fun deleteListing(listingId: Int): Response<Unit> = api.deleteListing(listingId)

    suspend fun getCloudinarySignature(request: CloudinarySignatureRequest) =
        api.getCloudinarySignature(request)

    suspend fun sendMessage(request: SendMessageRequest) = api.sendMessageAsBuyer(request)
}

enum class DataSource {
    NETWORK,
    CACHE
}

sealed class CourseResult {
    data class Success(
        val courses: List<Course>,
        val source: DataSource,
        val lastUpdated: Long?
    ) : CourseResult()

    data class Error(
        val message: String
    ) : CourseResult()
}

class CourseRepository(
    private val courseDao: CourseDao,
    private val api: ApiService = RetrofitInstance.api
) {
    suspend fun getCourses(forceRefresh: Boolean = false): CourseResult {
        val cached = courseDao.getCourses()
        val lastUpdated = courseDao.getLastUpdated()

        if (cached.isNotEmpty() && !forceRefresh) {
            return CourseResult.Success(
                courses = cached.toDomainCourses(),
                source = DataSource.CACHE,
                lastUpdated = lastUpdated
            )
        }

        return try {
            val remote = api.getCourses()
            val now = System.currentTimeMillis()
            val entities = remote.map { it.toEntity(now) }
            if (entities.isNotEmpty()) {
                courseDao.upsertCourses(entities)
            }
            CourseResult.Success(
                courses = entities.toDomainCourses(),
                source = DataSource.NETWORK,
                lastUpdated = courseDao.getLastUpdated()
            )
        } catch (e: Exception) {
            if (cached.isNotEmpty()) {
                CourseResult.Success(
                    courses = cached.toDomainCourses(),
                    source = DataSource.CACHE,
                    lastUpdated = lastUpdated
                )
            } else {
                CourseResult.Error("Could not load courses")
            }
        }
    }
}
