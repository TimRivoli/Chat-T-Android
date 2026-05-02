package com.chatty.android.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface NoteCategoryDao {
    @Query("SELECT * FROM notes_categories ORDER BY categoryID")
    fun getAll(): List<NoteCategoryEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertIfNotExists(entity: NoteCategoryEntity)

    @Query("DELETE FROM notes_categories WHERE categoryID = :categoryID")
    fun delete(categoryID: Int)
}
