package com.chatty.android.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY dateAccessed DESC")
    fun getAll(): List<NoteEntity>

    @Query("SELECT * FROM notes ORDER BY dateAccessed DESC LIMIT :limit")
    fun getAllLimited(limit: Int): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE categoryID = :categoryID ORDER BY dateAccessed DESC")
    fun getByCategory(categoryID: Int): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE categoryID = :categoryID ORDER BY dateAccessed DESC LIMIT :limit")
    fun getByCategoryLimited(categoryID: Int, limit: Int): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE (title LIKE :search OR content LIKE :search) ORDER BY dateAccessed DESC")
    fun search(search: String): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE (title LIKE :search OR content LIKE :search) ORDER BY dateAccessed DESC LIMIT :limit")
    fun searchLimited(search: String, limit: Int): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE categoryID = :categoryID AND (title LIKE :search OR content LIKE :search) ORDER BY dateAccessed DESC")
    fun searchInCategory(categoryID: Int, search: String): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE categoryID = :categoryID AND (title LIKE :search OR content LIKE :search) ORDER BY dateAccessed DESC LIMIT :limit")
    fun searchInCategoryLimited(categoryID: Int, search: String, limit: Int): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE noteID = :noteID LIMIT 1")
    fun get(noteID: Long): NoteEntity?

    @Query("SELECT MAX(dateModified) FROM notes")
    fun getLastModified(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(entity: NoteEntity)

    @Query("UPDATE notes SET dateCreated = :created, dateAccessed = :accessed, dateModified = :modified WHERE noteID = :noteID")
    fun updateDates(noteID: Long, created: Long, accessed: Long, modified: Long)

    @Query("DELETE FROM notes WHERE noteID = :noteID")
    fun delete(noteID: Long)

    @Query("UPDATE notes SET categoryID = :newID WHERE categoryID = :oldID")
    fun reassignCategory(oldID: Int, newID: Int)
}
