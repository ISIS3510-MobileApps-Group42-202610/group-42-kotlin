package com.example.unimarketfrontend.model.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.unimarketfrontend.model.local.dao.CourseDao
import com.example.unimarketfrontend.model.local.dao.ListingDao
import com.example.unimarketfrontend.model.local.dao.MessagesDao

/*
 * Esta es la base de datos principal de la app usando Room.
 * Aca centralizamos todas las tablas (entities) y los puntos de acceso (DAOs).
 * Sigue el patron Singleton para no abrir multiples conexiones al archivo .db.
 */
@Database(
    entities = [
        ListingEntity::class,
        CachedCourseEntity::class,
        ConversationEntity::class,
        PendingMessageEntity::class,
        MessageEntity::class // SPRINT 3: Tabla para el historial bilateral de mensajes
    ],
    version = 7, // Subimos la version por la nueva tabla CachedCourseEntity
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun listingDao(): ListingDao
    abstract fun messagesDao(): MessagesDao
    abstract fun courseDao(): CourseDao

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