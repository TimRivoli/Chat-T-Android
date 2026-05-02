package com.chatty.android.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SamplePromptDao {
    @Query("SELECT * FROM sample_prompts WHERE activityName = :activityName AND used = 0 ORDER BY RANDOM() LIMIT 1")
    fun getUnused(activityName: String): SamplePromptEntity?

    @Query("SELECT * FROM sample_prompts WHERE activityName = :activityName ORDER BY RANDOM() LIMIT 1")
    fun getRandom(activityName: String): SamplePromptEntity?

    @Query("SELECT * FROM sample_prompts WHERE timeStamp > :cutoff ORDER BY timeStamp")
    fun getSince(cutoff: Long): List<SamplePromptEntity>

    @Query("SELECT * FROM sample_prompts WHERE timeStamp > :cutoff AND used = 0 ORDER BY timeStamp")
    fun getSinceUnused(cutoff: Long): List<SamplePromptEntity>

    @Query("SELECT MAX(timeStamp) FROM sample_prompts")
    fun getLastUpdated(): Long?

    @Query("SELECT COUNT(*) FROM sample_prompts")
    fun count(): Long

    @Query("SELECT COUNT(*) FROM sample_prompts WHERE used = 0")
    fun countUnused(): Long

    @Insert
    fun insert(entity: SamplePromptEntity)

    @Query("UPDATE sample_prompts SET used = 0")
    fun resetUsed()

    @Query("UPDATE sample_prompts SET used = 1 WHERE activityName = :activityName AND prompt = :prompt")
    fun markUsed(activityName: String, prompt: String)

    @Query("DELETE FROM sample_prompts WHERE id NOT IN (SELECT MIN(id) FROM sample_prompts GROUP BY prompt)")
    fun removeDuplicates()
}
