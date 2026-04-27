package com.example.unimarketfrontend.model.repository

import android.content.Context
import com.example.unimarketfrontend.model.analytics.BusinessEventRequest
import com.example.unimarketfrontend.model.local.AppDatabase
import com.example.unimarketfrontend.model.local.ConversationEntity
import com.example.unimarketfrontend.model.local.MessageEntity
import com.example.unimarketfrontend.model.local.PendingMessageEntity
import com.example.unimarketfrontend.model.message.Message
import com.example.unimarketfrontend.model.message.SendMessageRequest
import com.example.unimarketfrontend.model.network.api.AnalyticsApiService
import com.example.unimarketfrontend.model.network.client.AnalyticsRetrofitInstance
import com.example.unimarketfrontend.model.network.client.RetrofitInstance
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.flow.Flow

class MessagesRepository(context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val messagesDao = db.messagesDao()
    private val api = RetrofitInstance.api
    private val analyticsApiService: AnalyticsApiService = AnalyticsRetrofitInstance.api

    fun observeConversations(): Flow<List<ConversationEntity>> =
        messagesDao.observeConversations()

    suspend fun isCacheStale(): Boolean {
        val oldest = messagesDao.getOldestCachedAt() ?: return true
        return (System.currentTimeMillis() - oldest) > (5 * 60 * 1000L)
    }

    suspend fun refreshFromNetwork(): Boolean {
        return try {
            val myId = getMyId()
            val asBuyer = api.getMessagesAsBuyer()
            val asSeller = api.getMessagesAsSeller()
            val allMessages = (asBuyer + asSeller).distinctBy { it.id }

            messagesDao.clearConversations()
            messagesDao.insertConversations(mapToEntities(allMessages, myId))

            true
        } catch (e: Exception) {
            false
        }
    }

    private fun mapToEntities(messages: List<Message>, myId: Int?): List<ConversationEntity> {
        return messages
            .groupBy { message -> message.resolveOtherUserId(myId) ?: 0 }
            .mapNotNull { (otherId, msgs) ->
                if (otherId == 0) return@mapNotNull null

                val last = msgs.maxByOrNull { it.created_at ?: "" }
                    ?: return@mapNotNull null

                val otherUser = last.resolveOtherUser(myId)

                ConversationEntity(
                    otherPersonId = otherId,
                    otherPersonName = "${otherUser?.name ?: "Usuario"} ${otherUser?.last_name ?: ""}".trim(),
                    lastMessage = last.content,
                    lastMessageTime = last.created_at
                        ?.takeIf { it.length >= 16 }
                        ?.substring(11, 16)
                        ?: "",
                    isRead = last.is_read,
                    cachedAt = System.currentTimeMillis()
                )
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
        val pending = messagesDao.getPending()
        var count = 0

        for (msg in pending) {
            try {
                api.sendMessageAsBuyer(
                    SendMessageRequest(
                        seller_id = msg.sellerId,
                        content = msg.content
                    )
                )
                messagesDao.deletePending(msg.localId)
                count++
            } catch (e: Exception) {
                // Se mantiene pendiente para reintentar después.
            }
        }

        return count
    }

    suspend fun getThread(sellerId: Int): List<Message> =
        api.getThread(sellerId)

    suspend fun sendMessage(sellerId: Int, content: String): Message {
        return api.sendMessageAsBuyer(
            SendMessageRequest(
                seller_id = sellerId,
                content = content
            )
        )
    }

    private suspend fun getMyId(): Int? =
        runCatching { api.getMe().id }.getOrNull()

    suspend fun refreshThread(otherId: Int): Boolean {
        return try {
            val myId = getMyId() ?: return false
            val messages = api.getThread(otherId)

            val entities = messages.map { msg ->
                MessageEntity(
                    id = msg.id,
                    content = msg.content,
                    sentBy = msg.sent_by,
                    conversationWithId = otherId,
                    createdAt = msg.created_at,
                    isMine = msg.resolveSenderUserId() == myId
                )
            }

            messagesDao.clearMessagesWith(otherId)
            messagesDao.insertMessages(entities)

            true
        } catch (e: Exception) {
            false
        }
    }

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
                client_event_id = "$eventName-$listingId-$buyerUserId-${System.currentTimeMillis()}"
            )

            analyticsApiService.sendBusinessEvent(event)

            true
        } catch (e: Exception) {
            false
        }
    }

    fun observeMessages(otherId: Int): Flow<List<MessageEntity>> =
        messagesDao.observeMessages(otherId)

    private fun Message.resolveBuyerUserId(): Int? =
        buyer?.user?.id ?: buyer_id

    private fun Message.resolveSellerUserId(): Int? =
        seller?.user?.id ?: seller_id

    private fun Message.resolveSenderUserId(): Int? {
        return when (sent_by) {
            "buyer" -> resolveBuyerUserId()
            "seller" -> resolveSellerUserId()
            else -> null
        }
    }

    private fun Message.resolveOtherUserId(myId: Int?): Int? {
        val buyerUserId = resolveBuyerUserId()
        val sellerUserId = resolveSellerUserId()

        return when (myId) {
            buyerUserId -> sellerUserId
            sellerUserId -> buyerUserId
            else -> {
                if (sent_by == "buyer") sellerUserId else buyerUserId
            }
        }
    }

    private fun Message.resolveOtherUser(myId: Int?): com.example.unimarketfrontend.model.user.User? {
        val buyerUserId = resolveBuyerUserId()
        val sellerUserId = resolveSellerUserId()

        return when (myId) {
            buyerUserId -> seller?.user
            sellerUserId -> buyer?.user
            else -> {
                if (sent_by == "buyer") seller?.user else buyer?.user
            }
        }
    }
}