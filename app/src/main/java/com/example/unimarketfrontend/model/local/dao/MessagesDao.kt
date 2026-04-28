package com.example.unimarketfrontend.model.local.dao

import androidx.room.*
import com.example.unimarketfrontend.model.local.ConversationEntity
import com.example.unimarketfrontend.model.local.MessageEntity
import com.example.unimarketfrontend.model.local.PendingMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessagesDao {

    @Query("SELECT * FROM conversations ORDER BY cachedAt DESC")
    fun observeConversations(): Flow<List<ConversationEntity>>

    // NUEVO: Necesario para saber si soy comprador o vendedor al cargar el thread
    @Query("SELECT * FROM conversations WHERE otherPersonId = :otherId LIMIT 1")
    suspend fun getConversationById(otherId: Int): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversations(list: List<ConversationEntity>)

    @Query("DELETE FROM conversations")
    suspend fun clearConversations()

    @Query("SELECT MIN(cachedAt) FROM conversations")
    suspend fun getOldestCachedAt(): Long?

    @Insert
    suspend fun insertPending(msg: PendingMessageEntity)

    @Query("SELECT * FROM pending_messages ORDER BY createdAt ASC")
    suspend fun getPending(): List<PendingMessageEntity>

    @Query("DELETE FROM pending_messages WHERE localId = :id")
    suspend fun deletePending(id: Int)

    @Query("SELECT COUNT(*) FROM pending_messages")
    suspend fun countPending(): Int

    @Query("SELECT * FROM messages WHERE conversationWithId = :otherId ORDER BY createdAt ASC")
    fun observeMessages(otherId: Int): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("DELETE FROM messages WHERE conversationWithId = :otherId")
    suspend fun clearMessagesWith(otherId: Int)
}
