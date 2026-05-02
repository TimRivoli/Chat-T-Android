package com.chatty.android.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface NoteDeletionDao {
    @Query("SELECT * FROM notes_deleted")
    fun getAll(): List<NoteDeletionEntity>

    @Query("SELECT MAX(timeStamp) FROM notes_deleted")
    fun getLastUpdated(): Long?

    @Insert
    fun insert(entity: NoteDeletionEntity)
}
