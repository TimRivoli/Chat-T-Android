package com.chatty.android.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PriceDao {
    @Query("SELECT * FROM prices_working_set ORDER BY ticker")
    fun getAll(): List<PriceEntity>

    @Query("SELECT MAX(latestEntry) FROM prices_working_set")
    fun getLastUpdated(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(entity: PriceEntity)

    @Query("DELETE FROM prices_working_set")
    fun clearAll()
}
