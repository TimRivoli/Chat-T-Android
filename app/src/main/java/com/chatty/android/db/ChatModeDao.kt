package com.chatty.android.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ChatModeDao {
    @Query("SELECT * FROM chat_modes ORDER BY sequence")
    fun getAll(): List<ChatModeEntity>

    @Query("SELECT MAX(timeStamp) FROM chat_modes")
    fun getLastUpdated(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: ChatModeEntity)

    @Query("DELETE FROM chat_modes")
    fun clearAll()
}
