package com.chatty.android.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ConversationDeletionDao {
    @Query("SELECT * FROM conversations_deleted")
    fun getAll(): List<ConversationDeletionEntity>

    @Insert
    fun insert(entity: ConversationDeletionEntity)

    @Query("DELETE FROM conversations_deleted")
    fun deleteAll()
}
