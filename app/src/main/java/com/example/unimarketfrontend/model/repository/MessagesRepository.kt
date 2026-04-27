package com.example.unimarketfrontend.model.repository

import com.example.unimarketfrontend.model.local.ConversationEntity
import com.example.unimarketfrontend.model.local.PendingMessageEntity
import com.example.unimarketfrontend.model.local.dao.MessagesDao
import com.example.unimarketfrontend.model.message.Message
import com.example.unimarketfrontend.model.message.SendMessageRequest
import com.example.unimarketfrontend.model.network.client.AnalyticsRetrofitInstance
import com.example.unimarketfrontend.model.network.client.RetrofitInstance
import com.example.unimarketfrontend.model.network.api.AnalyticsApiService
import com.example.unimarketfrontend.model.analytics.BusinessEventRequest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.flow.Flow

class MessagesRepository(
    private val messagesDao: MessagesDao
) {
    private val api = RetrofitInstance.api
    private val analyticsApiService = AnalyticsRetrofitInstance.api

    fun observeConversations(): Flow<List<ConversationEntity>> =
        messagesDao.observeConversations()

    suspend fun isCacheStale(maxAgeMs: Long = 10 * 60 * 1000L): Boolean {
        val oldest = messagesDao.getOldestCachedAt() ?: return true
        return System.currentTimeMillis() - oldest > maxAgeMs
    }

    suspend fun refreshFromNetwork(): Boolean {
        return try {
            val messages = api.getMessagesAsBuyer()
            messagesDao.insertConversations(messages.toConversationEntities())
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun savePendingMessage(sellerId: Int, content: String) {
        messagesDao.insertPending(
            PendingMessageEntity(
                sellerId = sellerId,
                content = content
            )
        )
    }

    suspend fun retryPendingMessages(): Int {
        val pendingMessages = messagesDao.getPending()
        var sent = 0

        pendingMessages.forEach { pending ->
            try {
                api.sendMessageAsBuyer(
                    SendMessageRequest(
                        seller_id = pending.sellerId,
                        content = pending.content
                    )
                )
                messagesDao.deletePending(pending.localId)
                sent++
            } catch (_: Exception) {
            }
        }

        return sent
    }

    suspend fun getThread(sellerId: Int): List<Message> =
        api.getThread(sellerId)

    suspend fun sendMessage(sellerId: Int, content: String): Message =
        api.sendMessageAsBuyer(
            SendMessageRequest(
                seller_id = sellerId,
                content = content
            )
        )

    suspend fun sendBusinessEvent(
        eventName: String,
        listingId: Long,
        buyerUserId: Long?,
        sellerUserId: Long?,
        metadata: Map<String, Any?>
    ): Boolean {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val timestamp = sdf.format(Date())

            val event = BusinessEventRequest(
                event_name = eventName,
                listing_id = listingId.toInt(),
                buyer_user_id = buyerUserId?.toInt(),
                seller_user_id = sellerUserId?.toInt(),
                timestamp = timestamp,
                metadata = metadata,
                client_event_id = "$eventName-${listingId}-${buyerUserId}-${System.currentTimeMillis()}"
            )
            analyticsApiService.sendBusinessEvent(event)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun List<Message>.toConversationEntities(): List<ConversationEntity> {
        return groupBy { message ->
            message.seller_id ?: message.buyer_id ?: message.id
        }.map { (otherId, messages) ->
            val last = messages.maxByOrNull { it.created_at.orEmpty() }

            ConversationEntity(
                otherPersonId = otherId,
                otherPersonName = last?.seller?.user?.let {
                    "${it.name} ${it.last_name}".trim()
                } ?: last?.buyer?.user?.let {
                    "${it.name} ${it.last_name}".trim()
                } ?: "Usuario #$otherId",
                lastMessage = last?.content.orEmpty(),
                lastMessageTime = last?.created_at.orEmpty(),
                isRead = last?.is_read ?: false,
                cachedAt = System.currentTimeMillis()
            )
        }
    }
}