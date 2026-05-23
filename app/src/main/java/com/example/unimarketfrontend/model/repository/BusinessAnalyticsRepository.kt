package com.example.unimarketfrontend.model.repository

import android.util.Log
import com.example.unimarketfrontend.model.utils.analytics.AnalyticsConfig
import com.example.unimarketfrontend.model.analytics.BusinessEventName
import com.example.unimarketfrontend.model.analytics.BusinessEventRequest
import com.example.unimarketfrontend.model.analytics.PerformanceEventRequest
import com.example.unimarketfrontend.model.network.api.ApiService
import com.example.unimarketfrontend.model.network.client.RetrofitInstance
import com.example.unimarketfrontend.model.network.api.AnalyticsApiService
import com.example.unimarketfrontend.model.network.client.AnalyticsRetrofitInstance
import com.example.unimarketfrontend.model.network.interceptor.AnalyticsAuthInterceptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

interface BusinessAnalyticsRepository {
    suspend fun sendEvent(event: BusinessEventRequest): Result<Unit>
    suspend fun sendPerformance(event: PerformanceEventRequest): Result<Unit>
}

class DefaultBusinessAnalyticsRepository(
    private val analyticsApiService: AnalyticsApiService = AnalyticsRetrofitInstance.api
) : BusinessAnalyticsRepository {

    override suspend fun sendEvent(event: BusinessEventRequest): Result<Unit> {
        return try {
            val response = analyticsApiService.sendBusinessEvent(event)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorBody = response.errorBody()?.string() ?: ""
                Result.failure(IllegalStateException("HTTP ${response.code()}: ${response.message()} - $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendPerformance(event: PerformanceEventRequest): Result<Unit> {
        return try {
            val response = analyticsApiService.sendPerformanceEvent(event)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorBody = response.errorBody()?.string() ?: ""
                Result.failure(IllegalStateException("HTTP ${response.code()}: ${response.message()} - $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class BusinessAnalyticsTracker(
    private val repository: BusinessAnalyticsRepository,
    private val appApiService: ApiService
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var cachedUserId: Int? = null

    fun setBusinessEventEnabled(eventName: BusinessEventName, enabled: Boolean) {
        AnalyticsConfig.businessEventFlags[eventName.value] = enabled
    }

    fun trackListingViewed(
        listingId: Int,
        sellerId: Int?,
        metadata: Map<String, Any?>? = null
    ) {
        trackEvent(BusinessEventName.LISTING_VIEWED, listingId, sellerId, metadata)
    }

    fun trackChatStarted(
        listingId: Int,
        sellerId: Int?,
        metadata: Map<String, Any?>? = null
    ) {
        trackEvent(BusinessEventName.CHAT_STARTED, listingId, sellerId, metadata)
    }

    fun trackFirstMessageSent(
        listingId: Int,
        sellerId: Int?,
        metadata: Map<String, Any?>? = null
    ) {
        trackEvent(BusinessEventName.FIRST_MESSAGE_SENT, listingId, sellerId, metadata)
    }

    fun trackTransactionCompleted(
        listingId: Int,
        sellerId: Int?,
        metadata: Map<String, Any?>? = null
    ) {
        trackEvent(BusinessEventName.TRANSACTION_COMPLETED, listingId, sellerId, metadata)
    }

    fun trackWishlistItemAdded(
        listingId: Int,
        sellerId: Int?,
        metadata: Map<String, Any?>? = null
    ) {
        trackEvent(BusinessEventName.WISHLIST_ITEM_ADDED, listingId, sellerId, metadata)
    }

    fun trackWishlistItemRemoved(
        listingId: Int,
        sellerId: Int? = null,
        metadata: Map<String, Any?>? = null
    ) {
        trackEvent(BusinessEventName.WISHLIST_ITEM_REMOVED, listingId, sellerId, metadata)
    }

    fun trackPerformance(
        metricName: String,
        valueMs: Long,
        listingId: Int? = null,
        screenName: String? = null,
        metadata: Map<String, Any?>? = null
    ) {
        val event = PerformanceEventRequest(
            metric_name = metricName,
            value_ms = valueMs,
            timestamp = nowIsoUtc(),
            listing_id = listingId,
            screen_name = screenName,
            metadata = metadata,
            client_event_id = buildClientEventId(metricName, listingId)
        )

        scope.launch {
            repository.sendPerformance(event).onFailure {
                Log.e("AnalyticsTracker", "Performance Error: ${it.message}", it)
            }
        }
    }

    private fun trackEvent(
        eventName: BusinessEventName,
        listingId: Int,
        sellerId: Int?,
        metadata: Map<String, Any?>?
    ) {
        val enabled = AnalyticsConfig.businessEventFlags[eventName.value] ?: true
        if (!enabled) return

        scope.launch {
            val buyerId = resolveCurrentUserId()
            val event = BusinessEventRequest(
                event_name = eventName.value,
                listing_id = listingId,
                buyer_user_id = buyerId,
                seller_user_id = sellerId,
                timestamp = nowIsoUtc(),
                metadata = metadata,
                client_event_id = buildClientEventId(eventName.value, listingId)
            )
            repository.sendEvent(event).onFailure {
                Log.e("AnalyticsTracker", "Event Error (${eventName.value}): ${it.message}", it)
            }
        }
    }

    private suspend fun resolveCurrentUserId(): Int? {
        if (cachedUserId != null) return cachedUserId
        return try {
            val me = appApiService.getMe()
            cachedUserId = me.id
            cachedUserId
        } catch (_: Exception) {
            null
        }
    }

    fun trackCampusBannerShown(
        listingId: Int,
        sellerId: Int?,
        metadata: Map<String, Any?>? = null,
        buildingName: String = "",
        latitude: Double = 0.0,
        longitude: Double = 0.0
    ) {
        scope.launch {
            try {
                val buyerId = resolveCurrentUserId()
                val request = com.example.unimarketfrontend.model.analytics.CampusLocationEventRequest(
                    event_name = "campus_banner_shown",
                    user_id = buyerId ?: 0,
                    listing_id = listingId,
                    seller_id = sellerId ?: 0,
                    building_name = buildingName,
                    latitude = latitude,
                    longitude = longitude,
                    timestamp = nowIsoUtc(),
                    metadata = metadata?.mapValues { it.value?.toString() ?: "" } ?: emptyMap()
                )
                AnalyticsRetrofitInstance.api.sendCampusEvent(request)
            } catch (e: Exception) { }
        }
    }

    fun trackListingCreated(
        listingId: Int,
        sellerId: Int?,
        metadata: Map<String, Any?>? = null
    ) {
        trackEvent(BusinessEventName.LISTING_CREATED, listingId, sellerId, metadata)
    }

    private fun nowIsoUtc(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    private fun buildClientEventId(eventName: String, listingId: Int?): String {
        val eventPart = eventName
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .take(20)
            .ifBlank { "evt" }

        val listingPart = listingId?.toString() ?: "na"
        val randomPart = UUID.randomUUID().toString().replace("-", "").takeLast(12)

        return "an-$eventPart-$listingPart-$randomPart".take(64)
    }
}

object BusinessAnalyticsProvider {
    val tracker: BusinessAnalyticsTracker by lazy {
        val repository: BusinessAnalyticsRepository = DefaultBusinessAnalyticsRepository(
            analyticsApiService = AnalyticsRetrofitInstance.api
        )

        BusinessAnalyticsTracker(
            repository = repository,
            appApiService = RetrofitInstance.api
        )
    }
}
