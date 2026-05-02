package com.chatty.android.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationID = :conversationID ORDER BY timeStamp")
    fun getByConversation(conversationID: Long): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: MessageEntity)

    @Query("DELETE FROM messages WHERE conversationID = :conversationID AND timeStamp = :timeStamp")
    fun delete(conversationID: Long, timeStamp: Long)

    @Query("DELETE FROM messages WHERE conversationID = :conversationID")
    fun deleteByConversation(conversationID: Long)

    @Query("DELETE FROM messages")
    fun deleteAll()
}
