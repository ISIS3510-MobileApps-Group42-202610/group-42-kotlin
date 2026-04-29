package com.example.unimarketfrontend.model.repository

import android.content.Context
import com.example.unimarketfrontend.model.local.AppDatabase
import com.example.unimarketfrontend.model.local.ConversationEntity
import com.example.unimarketfrontend.model.local.MessageEntity
import com.example.unimarketfrontend.model.local.PendingMessageEntity
import com.example.unimarketfrontend.model.message.Message
import com.example.unimarketfrontend.model.message.SendMessageAsSellerRequest
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

            val buyerConversations = asBuyer
                .groupBy { it.seller_id ?: 0 }
                .filter { (id, _) -> id != 0 }
                .mapNotNull { (sellerId, msgs) ->
                    val last = msgs.maxByOrNull { it.created_at ?: "" } ?: return@mapNotNull null
                    val sellerUser = last.seller?.user
                    // Solo marcar como no leido si el ultimo mensaje es del vendedor (no mio)
                    val lastIsFromOther = last.sent_by.equals("seller", ignoreCase = true)
                    val unread = lastIsFromOther && !last.is_read
                    ConversationEntity(
                        otherPersonId = sellerId,
                        otherPersonName = buildName(sellerUser?.name, sellerUser?.last_name)
                            .ifBlank { "Seller #$sellerId" },
                        lastMessage = last.content,
                        lastMessageTime = formatTime(last.created_at),
                        isRead = !unread,
                        cachedAt = System.currentTimeMillis(),
                        iAmBuyer = true
                    )
                }

            val sellerConversations = asSeller
                .groupBy { it.buyer_id ?: 0 }
                .filter { (id, _) -> id != 0 }
                .mapNotNull { (buyerId, msgs) ->
                    if (buyerConversations.any { it.otherPersonId == buyerId }) return@mapNotNull null
                    val last = msgs.maxByOrNull { it.created_at ?: "" } ?: return@mapNotNull null
                    val buyerUser = last.buyer?.user

                    val buyerUserId = buyerUser?.id ?: buyerId

                    // Solo marcar como no leido si el ultimo mensaje es del comprador (no mio)
                    val lastIsFromOther = last.sent_by.equals("buyer", ignoreCase = true)
                    val unread = lastIsFromOther && !last.is_read

                    ConversationEntity(
                        otherPersonId = buyerUserId,
                        otherPersonName = buildName(buyerUser?.name, buyerUser?.last_name)
                            .ifBlank { "Buyer #$buyerId" },
                        lastMessage = last.content,
                        lastMessageTime = formatTime(last.created_at),
                        isRead = !unread,
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

    suspend fun refreshThread(otherId: Int, iAmBuyer: Boolean): Boolean {
        return try {
            val messages: List<Message> = if (iAmBuyer) {
                api.getThread(otherId)
            } else {
                api.getThreadAsSeller(otherId)
            }

            val entities = messages.map { msg ->
                val isMine = if (iAmBuyer) {
                    msg.sent_by.equals("buyer", ignoreCase = true)
                } else {
                    msg.sent_by.equals("seller", ignoreCase = true)
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

            // Borrar solo mensajes del servidor (ID positivo), mantener pendientes (ID negativo)
            messagesDao.clearServerMessagesWith(otherId)
            messagesDao.insertMessages(entities)

            // Si hay pendientes que ya fueron enviados (aparecen en el servidor), limpiarlos
            val pendingCount = messagesDao.countPendingMessagesWith(otherId)
            if (pendingCount > 0 && entities.isNotEmpty()) {
                // Los pendientes ya se enviaron y aparecen en el servidor, borrar los locales
                messagesDao.clearPendingMessagesWith(otherId)
            }

            true
        } catch (e: Exception) {
            android.util.Log.e("MessagesRepository", "Error refreshing thread: ${e.message}", e)
            false
        }
    }

    suspend fun sendMessage(otherId: Int, content: String, iAmBuyer: Boolean): Message {
        return if (iAmBuyer) {
            api.sendMessageAsBuyer(SendMessageRequest(seller_id = otherId, content = content))
        } else {
            api.sendMessageAsSeller(SendMessageAsSellerRequest(buyer_id = otherId, content = content))
        }
    }

    suspend fun savePendingMessage(otherId: Int, content: String) {
        android.util.Log.d("MessagesRepository", "savePendingMessage: otherId=$otherId, content='$content'")

        // 1. Guardar en la cola de pendientes para reenviar cuando vuelva internet
        messagesDao.insertPending(
            PendingMessageEntity(sellerId = otherId, content = content)
        )

        // 2. Insertar también en la tabla de mensajes para que aparezca en la UI inmediatamente
        // Usamos un ID negativo temporal basado en timestamp para evitar colisiones
        val tempId = -(System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        val now = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }.format(java.util.Date())

        messagesDao.insertMessages(listOf(
            MessageEntity(
                id = tempId,
                content = content,
                sentBy = "pending",
                conversationWithId = otherId,
                createdAt = now,
                isMine = true
            )
        ))
        android.util.Log.d("MessagesRepository", "Pending message saved with tempId=$tempId")
    }

    suspend fun retryPendingMessages(): Int {
        val pending = messagesDao.getPending()
        var count = 0
        for (msg in pending) {
            try {
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
