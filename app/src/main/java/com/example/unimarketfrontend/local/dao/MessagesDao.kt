package com.example.unimarketfrontend.local.dao

import androidx.room.*
import com.example.unimarketfrontend.local.entity.ConversationEntity
import com.example.unimarketfrontend.local.entity.PendingMessageEntity
import kotlinx.coroutines.flow.Flow

/*
 * Data Access Object (DAO) para el modulo de mensajeria.
 * Define como interactuamos con la base de datos Room.
 * Usamos Flows para que la UI se actualice sola cuando cambien los datos (Patron Observer).
 */
@Dao
interface MessagesDao {
    
    // Observamos los chats: si Room cambia, la pantalla se entera sola.
    // Ordenamos por cachedAt para que los chats mas recientes salgan primero.
    @Query("SELECT * FROM conversations ORDER BY cachedAt DESC")
    fun observeConversations(): Flow<List<ConversationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversations(list: List<ConversationEntity>)

    @Query("DELETE FROM conversations")
    suspend fun clearConversations()

    @Query("SELECT MIN(cachedAt) FROM conversations")
    suspend fun getOldestCachedAt(): Long?

    // --- Cola de mensajes pendientes (Eventual Connectivity) ---
    
    @Insert
    suspend fun insertPending(msg: PendingMessageEntity)

    @Query("SELECT * FROM pending_messages ORDER BY createdAt ASC")
    suspend fun getPending(): List<PendingMessageEntity>

    @Query("DELETE FROM pending_messages WHERE localId = :id")
    suspend fun deletePending(id: Int)

    @Query("SELECT COUNT(*) FROM pending_messages")
    suspend fun countPending(): Int
}
