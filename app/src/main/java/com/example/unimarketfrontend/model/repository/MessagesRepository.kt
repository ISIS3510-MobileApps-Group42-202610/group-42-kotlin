package com.example.unimarketfrontend.repository

import com.example.unimarketfrontend.model.local.ConversationEntity
import com.example.unimarketfrontend.model.local.PendingMessageEntity
import com.example.unimarketfrontend.model.local.dao.MessagesDao
import com.example.unimarketfrontend.model.message.Message
import com.example.unimarketfrontend.model.message.SendMessageRequest
import com.example.unimarketfrontend.model.network.client.RetrofitInstance
import kotlinx.coroutines.flow.Flow

/*
 * Repositorio de Mensajes - SPRINT 3
 * Este compa es el mediador entre la base de datos (Room) y el servidor (Retrofit).
 * Aplicamos el patron Repository para que los ViewModels no tengan que ensuciarse con SQL o URLs.
 */
class MessagesRepository(
    private val messagesDao: MessagesDao // Inyectamos el archivista (DAO) aca
) {
    // Usamos el cliente de red centralizado
    private val api = RetrofitInstance.api

    // --- LOGICA DE CACHE (Patron Observer) ---

    // La pantalla se suscribe a este Flow. Si llega algo nuevo, Room avisa solo.
    fun observeConversations(): Flow<List<ConversationEntity>> =
        messagesDao.observeConversations()

    // Verificamos si los datos locales caducaron (Temporal Cache de 5 min)
    suspend fun isCacheStale(): Boolean {
        val oldest = messagesDao.getOldestCachedAt() ?: return true
        val fiveMinutes = 5 * 60 * 1000L
        return (System.currentTimeMillis() - oldest) > fiveMinutes
    }

    // --- LOGICA DE RED (Cache-then-Network) ---

    // Descargamos lo nuevo, limpiamos lo viejo y guardamos en el disco (Room)
    suspend fun refreshFromNetwork(): Boolean {
        return try {
            val messages = api.getMessagesAsBuyer()
            val entities = messages.groupBy { it.seller?.id }.mapNotNull { (sellerId, msgs) ->
                if (sellerId == null) return@mapNotNull null
                val last = msgs.maxByOrNull { it.created_at ?: "" } ?: return@mapNotNull null

                ConversationEntity(
                    otherPersonId = sellerId,
                    otherPersonName = "${last.seller?.user?.name ?: "Vendedor"} ${last.seller?.user?.last_name ?: ""}".trim(),
                    lastMessage = last.content,
                    lastMessageTime = last.created_at?.substring(11, 16) ?: "",
                    isRead = last.is_read
                )
            }
            // Borramos cache viejo y metemos lo nuevo de forma atomica
            messagesDao.clearConversations()
            messagesDao.insertConversations(entities)
            true
        } catch (e: Exception) {
            false // Fallo el internet, seguimos con los datos locales
        }
    }

    // --- CONECTIVIDAD EVENTUAL (Cola de Pendientes) ---

    // Si no hay red, guardamos el mensaje en "borradores" (Room)
    suspend fun savePendingMessage(sellerId: Int, content: String) {
        messagesDao.insertPending(
            PendingMessageEntity(sellerId = sellerId, content = content)
        )
    }

    // Intenta mandar todos los mensajes que se quedaron trabados sin red
    suspend fun retryPendingMessages(): Int {
        val pending = messagesDao.getPending()
        var count = 0
        for (msg in pending) {
            try {
                api.sendMessageAsBuyer(SendMessageRequest(seller_id = msg.sellerId, content = msg.content))
                messagesDao.deletePending(msg.localId) // Si se va, lo borramos de la DB
                count++
            } catch (e: Exception) { /* Si falla uno, el loop sigue con el otro */ }
        }
        return count
    }

    // --- FUNCIONES DE ACCESO DIRECTO ---

    // Trae el hilo completo con un vendedor
    suspend fun getThread(sellerId: Int): List<Message> = api.getThread(sellerId)

    // Envia un mensaje real al servidor NestJS
    suspend fun sendMessage(sellerId: Int, content: String): Message =
        api.sendMessageAsBuyer(SendMessageRequest(seller_id = sellerId, content = content))
}