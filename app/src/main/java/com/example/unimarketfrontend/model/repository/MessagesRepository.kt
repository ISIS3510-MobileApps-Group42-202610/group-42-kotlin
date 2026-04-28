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

    // Cache del user ID actual para determinar isMine correctamente
    private var cachedMyUserId: Int? = null
    private var cachedMySellerId: Int? = null

    fun observeConversations(): Flow<List<ConversationEntity>> =
        messagesDao.observeConversations()

    fun observeMessages(otherId: Int): Flow<List<MessageEntity>> =
        messagesDao.observeMessages(otherId)

    suspend fun isCacheStale(): Boolean {
        val oldest = messagesDao.getOldestCachedAt() ?: return true
        return (System.currentTimeMillis() - oldest) > (5 * 60 * 1000L)
    }

    // Obtener y cachear el user ID actual
    private suspend fun getMyUserId(): Int {
        if (cachedMyUserId == null) {
            cachedMyUserId = try {
                api.getMe().id
            } catch (e: Exception) {
                android.util.Log.e("MessagesRepository", "Error getting user ID: ${e.message}")
                -1
            }
        }
        return cachedMyUserId ?: -1
    }

    // Obtener el seller entity ID del usuario actual (si es vendedor)
    private suspend fun getMySellerId(): Int? {
        if (cachedMySellerId == null) {
            try {
                // Obtener mis listings para extraer el seller_id
                val myListings = api.getMyListings()
                if (myListings.isSuccessful) {
                    val listings = myListings.body()?.active ?: emptyList()
                    if (listings.isNotEmpty()) {
                        cachedMySellerId = listings.first().seller_id
                        android.util.Log.d("MessagesRepository", "My seller entity ID: $cachedMySellerId")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MessagesRepository", "Error getting seller ID: ${e.message}")
            }
        }
        return cachedMySellerId
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
            val myUserId = getMyUserId()
            val mySellerId = getMySellerId()
            android.util.Log.d("MessagesRepository", "refreshThread - otherId=$otherId, iAmBuyer=$iAmBuyer, myUserId=$myUserId, mySellerId=$mySellerId")

            val messages: List<Message> = if (iAmBuyer) {
                // otherId es seller entity ID
                android.util.Log.d("MessagesRepository", "Fetching as buyer from seller $otherId")
                api.getThread(otherId)
            } else {
                // otherId es buyer user ID
                android.util.Log.d("MessagesRepository", "Fetching as seller from buyer $otherId")
                api.getThreadAsSeller(otherId)
            }

            android.util.Log.d("MessagesRepository", "Received ${messages.size} messages")

            val entities = messages.map { msg ->
                // CORRECCIÓN CRÍTICA: Determinar isMine basado en los IDs del mensaje
                val isMine = if (iAmBuyer) {
                    // Si soy comprador, mis mensajes tienen buyer_id == myUserId
                    msg.buyer_id == myUserId
                } else {
                    // Si soy vendedor, mis mensajes tienen seller_id == mySellerId (entity ID)
                    msg.seller_id == mySellerId
                }

                android.util.Log.d("MessagesRepository", "Message ${msg.id}: sent_by=${msg.sent_by}, buyer_id=${msg.buyer_id}, seller_id=${msg.seller_id}, myUserId=$myUserId, mySellerId=$mySellerId, isMine=$isMine")

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
            android.util.Log.d("MessagesRepository", "Successfully saved ${entities.size} messages to Room")
            true
        } catch (e: Exception) {
            android.util.Log.e("MessagesRepository", "Error refreshing thread: ${e.message}", e)
            false
        }
    }

    // iAmBuyer viene directo del parámetro
    suspend fun sendMessage(otherId: Int, content: String, iAmBuyer: Boolean): Message {
        android.util.Log.d("MessagesRepository", "sendMessage - otherId=$otherId, iAmBuyer=$iAmBuyer, content='$content'")

        val sentMessage = if (iAmBuyer) {
            // Como comprador: otherId es el seller entity ID
            android.util.Log.d("MessagesRepository", "Sending as buyer to seller entity $otherId")
            api.sendMessageAsBuyer(SendMessageRequest(seller_id = otherId, content = content))
        } else {
            // Como vendedor: otherId es el buyer user ID
            android.util.Log.d("MessagesRepository", "Sending as seller to buyer user $otherId")
            api.sendMessageAsSeller(mapOf("buyer_id" to otherId, "content" to content))
        }

        android.util.Log.d("MessagesRepository", "Message sent successfully - id=${sentMessage.id}, sent_by=${sentMessage.sent_by}")
        return sentMessage
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