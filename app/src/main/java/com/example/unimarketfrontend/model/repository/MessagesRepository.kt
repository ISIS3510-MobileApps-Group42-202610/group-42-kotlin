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
 * Centraliza la persistencia local con Room y la sincronizacion bilateral con el Backend.
 * Maneja la identidad dinamica para asegurar que las burbujas de chat salgan del lado correcto.
 */
class MessagesRepository(context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val messagesDao = db.messagesDao()
    private val api = RetrofitInstance.api

    fun observeConversations(): Flow<List<ConversationEntity>> = messagesDao.observeConversations()

    // Táctica de Temporal Cache: 5 minutos
    suspend fun isCacheStale(maxAgeMs: Long = 5 * 60 * 1000L): Boolean {
        val oldest = messagesDao.getOldestCachedAt() ?: return true
        return (System.currentTimeMillis() - oldest) > maxAgeMs
    }

    // ACTUALIZACION BILATERAL: Traemos mensajes como comprador Y vendedor
    suspend fun refreshFromNetwork(): Boolean {
        return try {
            val myId = api.getMe().id
            val asBuyer = api.getMessagesAsBuyer()
            val asSeller = api.getMessagesAsSeller()

            val allMessages = (asBuyer + asSeller).distinctBy { it.id }

            // Mapeo Bilateral: El nombre es siempre el de la OTRA persona
            val entities = allMessages.groupBy {
                if (it.buyer_id == myId) it.seller_id else it.buyer_id
            }.mapNotNull { (otherId, msgs) ->
                if (otherId == null || otherId == 0) return@mapNotNull null
                val last = msgs.maxByOrNull { it.created_at ?: "" } ?: return@mapNotNull null
                val otherUser = if (last.buyer_id == myId) last.seller?.user else last.buyer?.user

                ConversationEntity(
                    otherPersonId = otherId,
                    otherPersonName = "${otherUser?.name ?: "Usuario"} ${otherUser?.last_name ?: ""}".trim(),
                    lastMessage = last.content,
                    lastMessageTime = last.created_at?.takeIf { it.length >= 16 }?.substring(11, 16) ?: "",
                    isRead = last.is_read,
                    cachedAt = System.currentTimeMillis()
                )
            }

            messagesDao.clearConversations()
            messagesDao.insertConversations(entities)
            true
        } catch (e: Exception) { false }
    }

    // HISTORIAL BILATERAL: Calcula isMine comparando con mi ID real
    suspend fun refreshThread(otherId: Int): Boolean {
        return try {
            val myId = api.getMe().id
            val messages = api.getThread(otherId)
            val entities = messages.map { msg ->
                MessageEntity(
                    id = msg.id,
                    content = msg.content,
                    sentBy = msg.sent_by,
                    conversationWithId = otherId,
                    createdAt = msg.created_at,
                    isMine = (msg.sent_by == "buyer" && msg.buyer_id == myId) ||
                            (msg.sent_by == "seller" && msg.seller_id == myId)
                )
            }
            messagesDao.clearMessagesWith(otherId)
            messagesDao.insertMessages(entities)
            true
        } catch (e: Exception) { false }
    }

    fun observeMessages(otherId: Int) = messagesDao.observeMessages(otherId)

    suspend fun sendMessage(sellerId: Int, content: String): Message {
        return api.sendMessageAsBuyer(SendMessageRequest(seller_id = sellerId, content = content))
    }

    // Conectividad Eventual: Guarda en Room si falla la red
    suspend fun savePendingMessage(sellerId: Int, content: String) {
        messagesDao.insertPending(
            PendingMessageEntity(sellerId = sellerId, content = content)
        )
    }

    // Reintento automatico
    suspend fun retryPendingMessages(): Int {
        val pendingMessages = messagesDao.getPending()
        var sentCount = 0
        pendingMessages.forEach { pending ->
            try {
                api.sendMessageAsBuyer(SendMessageRequest(seller_id = pending.sellerId, content = pending.content))
                messagesDao.deletePending(pending.localId)
                sentCount++
            } catch (_: Exception) { }
        }
        return sentCount
    }
}