package com.chatty.android.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface UsageDao {
    @Query("SELECT * FROM usage WHERE conversationID = :conversationID")
    fun getByConversation(conversationID: Long): List<UsageEntity>

    @Query("SELECT * FROM usage WHERE userID = :userID AND androidID = :androidID ORDER BY timeStamp")
    fun getByUser(userID: String, androidID: String): List<UsageEntity>

    @Query("SELECT MAX(timeStamp) FROM usage WHERE userID = :userID AND androidID = :androidID")
    fun getLastUpdated(userID: String, androidID: String): Long?

    @Insert
    fun insert(entity: UsageEntity)

    @Query("DELETE FROM usage")
    fun deleteAll()

    @Query("UPDATE usage SET userID = :userID WHERE userID = ''")
    fun fixEmptyUserIDs(userID: String)

    @Query("UPDATE usage SET androidID = :androidID WHERE androidID = ''")
    fun fixEmptyAndroidIDs(androidID: String)
}
