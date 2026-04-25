package com.example.unimarketfrontend.model.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.unimarketfrontend.model.local.dao.ListingDao
import com.example.unimarketfrontend.model.local.dao.MessagesDao

/*
 * Esta es la base de datos principal de la app usando Room.
 * Aca centralizamos todas las tablas (entities) y los puntos de acceso (DAOs).
 * Sigue el patron Singleton para no abrir multiples conexiones al archivo .db.
 */
@Database(
    entities = [ListingEntity::class, ConversationEntity::class, PendingMessageEntity::class],
    version = 4, // Subimos la versión porque añadimos ownerUserId a ListingEntity
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun listingDao(): ListingDao
    abstract fun messagesDao(): MessagesDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "unimarket.db"
                )
                    .fallbackToDestructiveMigration() // Si cambiamos el esquema, borra y recrea la DB
                    .build()
                    .also { instance = it }
            }
        }
    }
}