package com.example.unimarketfrontend.model.repository

import android.content.Context
import com.example.unimarketfrontend.model.local.AppDatabase
import com.example.unimarketfrontend.model.local.ConversationEntity
import com.example.unimarketfrontend.model.local.MessageEntity
import com.example.unimarketfrontend.model.local.PendingMessageEntity
import com.example.unimarketfrontend.model.message.Message
import com.example.unimarketfrontend.model.message.SendMessageRequest
import com.example.unimarketfrontend.model.network.client.RetrofitInstance
import kotlinx.coroutines.flow.Flow

class MessagesRepository(context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val messagesDao = db.messagesDao()
    private val api = RetrofitInstance.api

    fun observeConversations(): Flow<List<ConversationEntity>> =
        messagesDao.observeConversations()

    fun observeMessages(otherId: Int): Flow<List<MessageEntity>> =
        messagesDao.observeMessages(otherId)

    suspend fun isCacheStale(): Boolean {
        val oldest = messagesDao.getOldestCachedAt() ?: return true
        return (System.currentTimeMillis() - oldest) > (5 * 60 * 1000L)
    }

    suspend fun refreshFromNetwork(): Boolean {
        return try {
            val asBuyer = api.getMessagesAsBuyer()
            val asSeller = api.getMessagesAsSeller()

            // Conversaciones donde soy COMPRADOR
            val buyerConversations = asBuyer
                .groupBy { it.seller_id ?: 0 }
                .filter { (id, _) -> id != 0 }
                .mapNotNull { (sellerId, msgs) ->
                    val last = msgs.maxByOrNull { it.created_at ?: "" } ?: return@mapNotNull null
                    val sellerUser = last.seller?.user
                    ConversationEntity(
                        otherPersonId = sellerId,
                        otherPersonName = buildName(sellerUser?.name, sellerUser?.last_name)
                            .ifBlank { "Seller #$sellerId" },
                        lastMessage = last.content,
                        lastMessageTime = formatTime(last.created_at),
                        isRead = last.is_read,
                        cachedAt = System.currentTimeMillis(),
                        iAmBuyer = true
                    )
                }

            // Conversaciones donde soy VENDEDOR
            val sellerConversations = asSeller
                .groupBy { it.buyer_id ?: 0 }
                .filter { (id, _) -> id != 0 }
                .mapNotNull { (buyerId, msgs) ->
                    // Evitar duplicar si ya existe como conversación de comprador
                    if (buyerConversations.any { it.otherPersonId == buyerId }) return@mapNotNull null
                    val last = msgs.maxByOrNull { it.created_at ?: "" } ?: return@mapNotNull null
                    val buyerUser = last.buyer?.user
                    ConversationEntity(
                        otherPersonId = buyerId,
                        otherPersonName = buildName(buyerUser?.name, buyerUser?.last_name)
                            .ifBlank { "Buyer #$buyerId" },
                        lastMessage = last.content,
                        lastMessageTime = formatTime(last.created_at),
                        isRead = last.is_read,
                        cachedAt = System.currentTimeMillis(),
                        iAmBuyer = false
                    )
                }

            val all = buyerConversations + sellerConversations
            messagesDao.clearConversations()
            messagesDao.insertConversations(all)
            true
        } catch (e: Exception) {
            false
        }
    }

    // iAmBuyer viene directo del parámetro — sin consultar Room
    suspend fun refreshThread(otherId: Int, iAmBuyer: Boolean): Boolean {
        return try {
            val messages: List<Message> = if (iAmBuyer) {
                // otherId es seller entity ID
                api.getThread(otherId)
            } else {
                // otherId es buyer user ID
                api.getThreadAsSeller(otherId)
            }

            val entities = messages.map { msg ->
                // CORRECCIÓN: isMine depende solo de sent_by y el rol del usuario actual
                // Si soy comprador → mis mensajes tienen sent_by = "buyer"
                // Si soy vendedor → mis mensajes tienen sent_by = "seller"
                // No necesitamos comparar user IDs — la conversación es entre exactamente dos personas
                val isMine = if (iAmBuyer) {
                    msg.sent_by == "buyer"
                } else {
                    msg.sent_by == "seller"
                }

                MessageEntity(
                    id = msg.id,
                    content = msg.content,
                    sentBy = msg.sent_by,
                    conversationWithId = otherId,
                    createdAt = msg.created_at,
                    isMine = isMine
                )
            }

            messagesDao.clearMessagesWith(otherId)
            messagesDao.insertMessages(entities)
            true
        } catch (e: Exception) {
            false
        }
    }

    // iAmBuyer viene directo del parámetro
    suspend fun sendMessage(otherId: Int, content: String, iAmBuyer: Boolean): Message {
        return if (iAmBuyer) {
            api.sendMessageAsBuyer(SendMessageRequest(seller_id = otherId, content = content))
        } else {
            // otherId es buyer user ID cuando sos vendedor
            api.sendMessageAsSeller(mapOf("buyer_id" to otherId, "content" to content))
        }
    }

    suspend fun savePendingMessage(otherId: Int, content: String) {
        messagesDao.insertPending(
            PendingMessageEntity(sellerId = otherId, content = content)
        )
    }

    suspend fun retryPendingMessages(): Int {
        val pending = messagesDao.getPending()
        var count = 0
        for (msg in pending) {
            try {
                // Los pendientes siempre son de comprador (se envían cuando se reintenta)
                api.sendMessageAsBuyer(
                    SendMessageRequest(seller_id = msg.sellerId, content = msg.content)
                )
                messagesDao.deletePending(msg.localId)
                count++
            } catch (e: Exception) { }
        }
        return count
    }

    private fun buildName(first: String?, last: String?): String =
        listOfNotNull(first, last).joinToString(" ").trim()

    private fun formatTime(isoDate: String?): String {
        if (isoDate == null) return ""
        return try { isoDate.substring(11, 16) } catch (e: Exception) { "" }
    }
}