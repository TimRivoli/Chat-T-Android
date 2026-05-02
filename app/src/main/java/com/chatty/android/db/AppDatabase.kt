package com.chatty.android.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ConversationEntity::class,
        ConversationDeletionEntity::class,
        MessageEntity::class,
        UsageEntity::class,
        SamplePromptEntity::class,
        ChatModeEntity::class,
        NoteEntity::class,
        NoteCategoryEntity::class,
        NoteDeletionEntity::class,
        PriceEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun conversationDeletionDao(): ConversationDeletionDao
    abstract fun messageDao(): MessageDao
    abstract fun usageDao(): UsageDao
    abstract fun samplePromptDao(): SamplePromptDao
    abstract fun chatModeDao(): ChatModeDao
    abstract fun noteDao(): NoteDao
    abstract fun noteCategoryDao(): NoteCategoryDao
    abstract fun noteDeletionDao(): NoteDeletionDao
    abstract fun priceDao(): PriceDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun build(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context, AppDatabase::class.java, "ChatMessagesDB")
                // Existing local data re-syncs from Firebase on next launch.
                .fallbackToDestructiveMigrationFrom(1, 2, 3, 4)
                .allowMainThreadQueries()
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        db.execSQL(
                            "INSERT OR IGNORE INTO notes_categories (categoryID, categoryName) VALUES (0, 'General')"
                        )
                    }
                })
                .build().also { INSTANCE = it }
        }
    }
}
