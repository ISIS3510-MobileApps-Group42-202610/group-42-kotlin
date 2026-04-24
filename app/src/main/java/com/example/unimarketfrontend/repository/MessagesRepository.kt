package com.example.unimarketfrontend.repository

import com.example.unimarketfrontend.local.dao.MessagesDao
import com.example.unimarketfrontend.model.local.ConversationEntity
import com.example.unimarketfrontend.model.local.PendingMessageEntity
import com.example.unimarketfrontend.network.client.RetrofitInstance
import com.example.unimarketfrontend.model.message.SendMessageRequest
import kotlinx.coroutines.flow.Flow

/*
 * El repositorio es el corazon de la estrategia de datos del Sprint 3.
 * Sigue el patron Repository (Ch. 7 del libro) para decidir de donde vienen los datos:
 * si del cache local (Room) o del servidor (Retrofit).
 */
class MessagesRepository(
    private val messagesDao: MessagesDao
) {

    // Observamos los chats guardados. Esto permite que la UI sea reactiva:
    // si llega un mensaje nuevo en background, Room se actualiza y la pantalla tambien.
    fun observeConversations(): Flow<List<ConversationEntity>> =
        messagesDao.observeConversations()

    /*
     * Estrategia Temporal Cache (Ch. 10): 
     * Verificamos si los datos en el celular tienen mas de 5 minutos.
     * Si son muy viejos, disparamos un refresh obligatorio.
     */
    suspend fun isCacheStale(): Boolean {
        val oldest = messagesDao.getOldestCachedAt() ?: return true
        val fiveMinutes = 5 * 60 * 1000L
        return (System.currentTimeMillis() - oldest) > fiveMinutes
    }

    /*
     * Implementamos Cache-then-Network:
     * Esta funcion descarga los mensajes del servidor, los procesa y los guarda en Room.
     * Al guardar en Room, el Flow de arriba (observeConversations) se dispara solo.
     */
    suspend fun refreshFromNetwork(): Boolean {
        return try {
            val messages = RetrofitInstance.api.getMessagesAsBuyer()

            val entities = messages
                .groupBy { it.seller?.id }
                .mapNotNull { (sellerId, msgs) ->
                    if (sellerId == null) return@mapNotNull null
                    
                    val last = msgs.maxByOrNull { it.created_at ?: "" } ?: return@mapNotNull null
                    val user = last.seller?.user

                    ConversationEntity(
                        otherPersonId = sellerId,
                        otherPersonName = buildName(user?.name, user?.last_name),
                        lastMessage = last.content,
                        lastMessageTime = formatTime(last.created_at),
                        isRead = last.is_read
                    )
                }

            // Limpiamos lo viejo y metemos lo nuevo (Atomic Update)
            messagesDao.clearConversations()
            messagesDao.insertConversations(entities)
            true
        } catch (e: Exception) {
            false // Si falla el internet, devolvemos false para que el ViewModel sepa
        }
    }

    // --- Logica de Conectividad Eventual (Cola de pendientes) ---

    // Guarda el mensaje en Room cuando no hay red (Evita antipatron UC)
    suspend fun savePendingMessage(sellerId: Int, content: String) {
        messagesDao.insertPending(
            PendingMessageEntity(sellerId = sellerId, content = content)
        )
    }

    // Intenta vaciar la cola de mensajes pendientes cuando vuelve el internet
    suspend fun retryPendingMessages(): Int {
        val pending = messagesDao.getPending()
        var successCount = 0
        
        for (msg in pending) {
            try {
                RetrofitInstance.api.sendMessageAsBuyer(
                    SendMessageRequest(seller_id = msg.sellerId, content = msg.content)
                )
                messagesDao.deletePending(msg.localId)
                successCount++
            } catch (e: Exception) {
                // Si falla uno, paramos o seguimos con el otro? Seguimos intentando.
            }
        }
        return successCount
    }

    // Helpers de formato (mantenemos consistencia con Sprint 2)
    private fun buildName(first: String?, last: String?): String =
        listOfNotNull(first, last).joinToString(" ").trim().ifBlank { "Vendedor Desconocido" }

    private fun formatTime(isoDate: String?): String {
        if (isoDate == null) return ""
        return try { isoDate.substring(11, 16) } catch (e: Exception) { "" }
    }
}
