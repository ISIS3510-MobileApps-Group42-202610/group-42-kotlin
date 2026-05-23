package com.example.unimarketfrontend.model.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.unimarketfrontend.model.local.dao.CourseDao
import com.example.unimarketfrontend.model.local.dao.ListingDao
import com.example.unimarketfrontend.model.local.dao.MessagesDao
import com.example.unimarketfrontend.model.local.dao.WishlistDao
import com.example.unimarketfrontend.model.local.dao.PendingWishlistDao

@Database(
    entities = [
        ListingEntity::class,
        CachedCourseEntity::class,
        ConversationEntity::class,
        PendingMessageEntity::class,
        MessageEntity::class,
        WishlistEntity::class,
        PendingWishlistEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun listingDao(): ListingDao
    abstract fun messagesDao(): MessagesDao
    abstract fun courseDao(): CourseDao
    abstract fun wishlistDao(): WishlistDao
    abstract fun pendingWishlistDao(): PendingWishlistDao

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
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
        }
    }
}
