package com.chatty.android.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations WHERE userID = :userID ORDER BY dateAccessed DESC")
    fun getAll(userID: String): List<ConversationEntity>

    @Query("SELECT * FROM conversations WHERE userID = :userID ORDER BY dateAccessed DESC LIMIT :limit")
    fun getAllLimited(userID: String, limit: Int): List<ConversationEntity>

    @Query("SELECT * FROM conversations WHERE userID = :userID AND title LIKE :search ORDER BY dateAccessed DESC")
    fun search(userID: String, search: String): List<ConversationEntity>

    @Query("SELECT * FROM conversations WHERE userID = :userID AND title LIKE :search ORDER BY dateAccessed DESC LIMIT :limit")
    fun searchLimited(userID: String, search: String, limit: Int): List<ConversationEntity>

    @Query("SELECT MAX(dateModified) FROM conversations WHERE userID = :userID")
    fun getLastModified(userID: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(entity: ConversationEntity)

    @Query("DELETE FROM conversations WHERE conversationID = :id")
    fun delete(id: Long)

    @Query("DELETE FROM conversations")
    fun deleteAll()

    @Query("SELECT * FROM conversations WHERE conversationID = :id AND userID = :userID LIMIT 1")
    fun get(id: Long, userID: String): ConversationEntity?

    @Query("UPDATE conversations SET dateCreated = :created, dateAccessed = :accessed, dateModified = :modified WHERE conversationID = :id")
    fun updateDates(id: Long, created: Long, accessed: Long, modified: Long)

    @Query("UPDATE conversations SET userID = :userID WHERE userID = ''")
    fun fixEmptyUserIDs(userID: String)
}
