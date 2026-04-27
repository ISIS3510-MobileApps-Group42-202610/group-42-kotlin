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

/*
 * Repositorio de Mensajeria - SPRINT 3
 * Corregido para manejar el contexto y centralizar la logica bilateral.
 */
class MessagesRepository(context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val messagesDao = db.messagesDao()
    private val api = RetrofitInstance.api

    fun observeConversations(): Flow<List<ConversationEntity>> = messagesDao.observeConversations()

    // --- SPRINT 3: TACTICA DE CACHING ---
    suspend fun isCacheStale(): Boolean {
        val oldest = messagesDao.getOldestCachedAt() ?: return true
        return (System.currentTimeMillis() - oldest) > (5 * 60 * 1000L)
    }

    suspend fun refreshFromNetwork(): Boolean {
        return try {
            val asBuyer = api.getMessagesAsBuyer()
            val asSeller = api.getMessagesAsSeller()
            val allMessages = (asBuyer + asSeller).distinctBy { it.id }

            messagesDao.clearConversations()
            messagesDao.insertConversations(mapToEntities(allMessages))
            true
        } catch (e: Exception) { false }
    }

    private fun mapToEntities(messages: List<Message>): List<ConversationEntity> {
        return messages.groupBy { it.seller_id ?: it.buyer_id ?: 0 }.mapNotNull { (otherId, msgs) ->
            if (otherId == 0) return@mapNotNull null
            val last = msgs.maxByOrNull { it.created_at ?: "" } ?: return@mapNotNull null
            val otherUser = if (last.sent_by == "buyer") last.seller?.user else last.buyer?.user

            ConversationEntity(
                otherPersonId = otherId,
                otherPersonName = "${otherUser?.name ?: "Usuario"} ${otherUser?.last_name ?: ""}".trim(),
                lastMessage = last.content,
                lastMessageTime = last.created_at?.takeIf { it.length >= 16 }?.substring(11, 16) ?: "",
                isRead = last.is_read,
                cachedAt = System.currentTimeMillis()
            )
        }
    }

    // --- SPRINT 3: CONECTIVIDAD EVENTUAL ---
    suspend fun savePendingMessage(sellerId: Int, content: String) {
        messagesDao.insertPending(PendingMessageEntity(sellerId = sellerId, content = content))
    }

    suspend fun retryPendingMessages(): Int {
        val pending = messagesDao.getPending()
        var count = 0
        for (msg in pending) {
            try {
                api.sendMessageAsBuyer(SendMessageRequest(seller_id = msg.sellerId, content = msg.content))
                messagesDao.deletePending(msg.localId)
                count++
            } catch (e: Exception) { }
        }
        return count
    }

    suspend fun getThread(sellerId: Int) = api.getThread(sellerId)

    suspend fun sendMessage(sellerId: Int, content: String): Message {
        return api.sendMessageAsBuyer(SendMessageRequest(seller_id = sellerId, content = content))
    }

    // Necesitamos saber quién soy yo para marcar "isMine"
    private suspend fun getMyId(): Int? = runCatching { api.getMe().id }.getOrNull()

    suspend fun refreshThread(otherId: Int): Boolean {
        return try {
            val myId = getMyId() ?: return false
            val messages = api.getThread(otherId)

            val entities = messages.map { msg ->
                // Lógica Bilateral: ¿Quién mandó el mensaje?
                val isMine = if (msg.sent_by == "buyer") {
                    msg.buyer_id == myId || msg.buyer?.id == myId
                } else {
                    msg.seller_id == myId || msg.seller?.id == myId
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
        } catch (e: Exception) { false }
    }

    fun observeMessages(otherId: Int) = messagesDao.observeMessages(otherId)
}