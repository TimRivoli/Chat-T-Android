package com.chatty.android.etc
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.util.Date
import com.chatty.android.etc.DataClasses.*
import com.chatty.android.etc.DynamicContentGeneration.defaultNoteCategories

private class SQLiteHelper(
    context: Context,
    name: String = DATABASE_NAME,
    factory: SQLiteDatabase.CursorFactory? = null,
    version: Int = DATABASE_VERSION
) : SQLiteOpenHelper(context, name, factory, version) {
    companion object {
        const val DATABASE_VERSION = 3
        const val DATABASE_NAME = "ChatMessagesDB"
        private const val CREATE_CONVERSATION_TABLE = """
            CREATE TABLE IF NOT EXISTS  $ConversationTableName (
                conversationID LONG,
                dateCreated LONG,
                dateAccessed LONG,
                dateModified LONG,
                saved INTEGER,
                title TEXT,
                summary TEXT,
                userID TEXT,
                firebaseID STRING
            )
        """
        private const val CREATE_CONVERSATION_DELETED_TABLE = """ CREATE TABLE IF NOT EXISTS $ConversationDeletionsTableName ( conversationID LONG, timeStamp LONG  )        """

        private const val CREATE_MESSAGES_TABLE = """
            CREATE TABLE IF NOT EXISTS $MessageTableName (
                conversationID LONG,
                timeStamp LONG,
                role TEXT,
                content TEXT
            )
        """
        private const val CREATE_USAGE_TABLE = """
            CREATE TABLE IF NOT EXISTS $UsageTableName (
                conversationID LONG,
                timeStamp LONG,
                promptTokens INTEGER,
                completionTokens INTEGER,
                totalTokens INTEGER,
                userID TEXT,
                androidID TEXT
            )
        """
        private const val CREATE_SAMPLEPROMPTS_TABLE = """
            CREATE TABLE IF NOT EXISTS $SamplePromptTableName (
                activityName TEXT,
                prompt TEXT,
                used INTEGER,
                timeStamp LONG
            )
        """
        // value.activityName + "|" + value.prompt + "|" + value.conversational + "|" + value.clearConversationOnChange + "|" + value.showLanguageOptions + "|" + value.temperature
        private const val CREATE_CHATMODES_TABLE = """
            CREATE TABLE IF NOT EXISTS $ChatModesTableName (
                activityName TEXT,
                prompt TEXT,
                conversational INTEGER,
                clearConversationOnChange INTEGER,
                showLanguageOptions INTEGER,
                temperature FLOAT,
                sequence INTEGER,
                timeStamp LONG
            )
        """

        private const val CREATE_NOTES_TABLE = """
            CREATE TABLE IF NOT EXISTS $NotesTableName (
                noteID LONG,
                categoryID INTEGER,
                title TEXT,
                content TEXT,
                dateCreated LONG,
                dateModified LONG,
                dateAccessed LONG
            )
        """
        private const val CREATE_NOTESCATEGORIES_TABLE = """
            CREATE TABLE IF NOT EXISTS $NotesCategoryTableName (
                categoryID INTEGER,
                categoryName TEXT
            )
        """
        private const val CREATE_DEFAULT_NOTESCATEGORIES = """
            INSERT INTO ${NotesCategoryTableName} (categoryID, categoryName)
            SELECT 0, 'General'
            WHERE NOT EXISTS (SELECT 1 FROM ${NotesCategoryTableName} WHERE categoryID = 0);
        """
        private const val CREATE_NOTES_DELETED_TABLE = """ CREATE TABLE IF NOT EXISTS $NotesDeletionsTableName ( noteID LONG, timeStamp LONG  )        """

    }

    override fun onCreate(db: SQLiteDatabase) {
        Log.d("SQLiteHelper", "Creating database...")
        db.execSQL(CREATE_CONVERSATION_TABLE)
        db.execSQL(CREATE_CONVERSATION_DELETED_TABLE)
        db.execSQL(CREATE_MESSAGES_TABLE)
        db.execSQL(CREATE_USAGE_TABLE)
        db.execSQL(CREATE_SAMPLEPROMPTS_TABLE)
        db.execSQL(CREATE_CHATMODES_TABLE)
        db.execSQL(CREATE_NOTESCATEGORIES_TABLE)
        db.execSQL(CREATE_DEFAULT_NOTESCATEGORIES)
        db.execSQL(CREATE_NOTES_TABLE)
        db.execSQL(CREATE_NOTES_DELETED_TABLE)
    }

    fun recreateDatabase(db: SQLiteDatabase) {
        Log.d("SQLiteHelper", "Deleting and re-creating database...")
        db.execSQL("DROP TABLE IF EXISTS $ConversationTableName")
        db.execSQL("DROP TABLE IF EXISTS $ConversationDeletionsTableName")
        db.execSQL("DROP TABLE IF EXISTS $MessageTableName")
        db.execSQL("DROP TABLE IF EXISTS $UsageTableName")
        db.execSQL("DROP TABLE IF EXISTS $SamplePromptTableName")
        db.execSQL("DROP TABLE IF EXISTS $ChatModesTableName")
        db.execSQL("DROP TABLE IF EXISTS $NotesTableName")
        db.execSQL("DROP TABLE IF EXISTS $NotesCategoryTableName")
        db.execSQL("DROP TABLE IF EXISTS $NotesDeletionsTableName")
        onCreate(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.d("SQLiteHelper", "onUpgrade update database from ${oldVersion} to ${newVersion}")
        if (oldVersion < 2) {
            db.execSQL("DROP TABLE IF EXISTS $ConversationDeletionsTableName")
        }
        if (oldVersion < 3) {
            db.execSQL("DROP TABLE IF EXISTS $NotesTableName")
            db.execSQL("DROP TABLE IF EXISTS $NotesCategoryTableName")
            db.execSQL("DROP TABLE IF EXISTS $NotesDeletionsTableName")
        }
        onCreate(db)
    }
}

class SQLiteManager(context: Context) {
    //TODO: remove references to deviceID or androidID, this is purely local userID is blank or GoogleID
    private val TAG = "SQLiteDataSource"
    private val dbHelper: SQLiteHelper = SQLiteHelper(context)
    private val database: SQLiteDatabase = dbHelper.writableDatabase
    private var noteCategories = ArrayList<NoteCategory>()
    fun shutDown(){
        database.close()
        dbHelper.close()
    }

    fun clearDatabase(recreateDatabase: Boolean = false){
        if (recreateDatabase) {
            Log.d(TAG, "Dropping and recreating database...")
            dbHelper.recreateDatabase(database)
        } else {
            Log.d(TAG, "Deleting invalid conversations...")
            database.execSQL("DELETE FROM $ConversationTableName")
            database.execSQL("DELETE FROM $ConversationDeletionsTableName")
            database.execSQL("DELETE FROM $MessageTableName")
            database.execSQL("DELETE FROM $UsageTableName")
            database.execSQL("DELETE FROM $ChatModesTableName")
        }
    }

    //---------------------------------- Conversations  ------------------------------------------
    @SuppressLint("Range")
    private fun cursorToConversation(cursor: Cursor): Conversation {
        val conversationID = cursor.getLong(cursor.getColumnIndex("conversationID"))
        val title = cursor.getString(cursor.getColumnIndex("title"))
        val summary = cursor.getString(cursor.getColumnIndex("summary"))
        val saved = cursor.getInt(cursor.getColumnIndex("saved")) == 1
        val userID = cursor.getString(cursor.getColumnIndex("userID"))
        val dateCreated = Date(cursor.getLong(cursor.getColumnIndex("dateCreated")))
        val dateAccessed = Date(cursor.getLong(cursor.getColumnIndex("dateAccessed")))
        val dateModified = Date(cursor.getLong(cursor.getColumnIndex("dateModified")))
        return Conversation(conversationID, title, summary, saved, userID, dateCreated, dateAccessed, dateModified)
    }

    @SuppressLint("Range")
    fun getConversationsLastUpdated(userID: String):Long {
        val cursor = database.rawQuery("SELECT MAX(dateModified) AS maxTimeStamp FROM $ConversationTableName WHERE userID='$userID'", null)
        var maxTimeStamp: Long = 0
        if (cursor.moveToFirst()) {
            maxTimeStamp = cursor.getLong(cursor.getColumnIndex("maxTimeStamp"))
        }
        cursor.close()
        return maxTimeStamp
    }

    fun updateConversationDates(conversation: Conversation) {
        //values.put doesn't seem to work, so I'm using a SQL statement
        val SQL = "UPDATE $ConversationTableName SET dateCreated = ${conversation.dateCreated.time}, dateAccessed = ${conversation.dateAccessed.time},  dateModified = ${conversation.dateModified.time} WHERE  conversationID = ${conversation.conversationID};"
        Log.d(TAG, SQL)
        database.execSQL(SQL)
    }
    fun saveConversation(conversation: Conversation) {
        Log.d(TAG, "Appending conversation to SQL " + conversation.conversationID)
        val values = ContentValues()
        values.put("conversationID", conversation.conversationID)
        //values.put("dateCreated", conversation.dateCreated.time)  //This doesn't work, it comes back as long value = 2023
        //values.put("dateAccessed", conversation.dateAccessed.time)
        //values.put("dateModified", conversation.dateModified.time)
        values.put("title", conversation.title)
        values.put("summary", conversation.summary)
        values.put("saved", if (conversation.saved) 1 else 0)
        values.put("userID", conversation.userID)
        database.insert(ConversationTableName, null, values)
        updateConversationDates(conversation)
    }

    fun deleteConversation(conversationID: Long){
        database.execSQL("DELETE FROM $ConversationTableName WHERE conversationID=$conversationID")
        database.execSQL("DELETE FROM $MessageTableName WHERE conversationID=$conversationID")
        database.execSQL("INSERT INTO $ConversationDeletionsTableName (conversationID, timeStamp) values(${conversationID}, ${Date().time})")
    }

    @SuppressLint("Range")
    fun getDeletedConversations(userID:String): ArrayList<Long> {
        val result = ArrayList<Long>()
        val cursor: Cursor
        try {
            cursor = database.query(ConversationDeletionsTableName, null, null, null,null, null, null)
            cursor.moveToFirst()
            while (!cursor.isAfterLast) {
                val conversationID = cursor.getLong(cursor.getColumnIndex("conversationID"))
                result.add(conversationID)
                cursor.moveToNext()
            }
            cursor.close()
        } catch (e: SQLException) {
            Log.e(TAG, e.message.toString())
        }
        return result
    }

    fun getConversations(userID:String, searchString: String = "", limitResultsTo: Int=50): List<Conversation> {
        val conversations = ArrayList<Conversation>()
        val cursor: Cursor
        var sql = "SELECT * FROM $ConversationTableName"
        var whereClause = ""
        if (searchString !="") {
            whereClause = "(Title LIKE '%" + searchString + "%')"
        }
        if (whereClause != "") {   sql += " WHERE " + whereClause  }
        sql += " ORDER BY dateAccessed DESC"
        if (limitResultsTo > 0) {sql += " LIMIT ${limitResultsTo}"}
        Log.d(TAG, "SQL: ${sql}")
        try {
            cursor = database.rawQuery(sql, null)
            cursor.moveToFirst()
            while (!cursor.isAfterLast) {
                val conversation = cursorToConversation(cursor)
                conversations.add(conversation)
                cursor.moveToNext()
            }
            cursor.close()
        } catch (e: SQLException) {
            Log.e(TAG, e.message.toString())
        }
        return conversations
    }

    fun getConversation(conversationID: Long, userID:String): Conversation {
        var conversation = Conversation()
        val cursor: Cursor
        Log.d(TAG, "getConversation $conversationID userID: $userID")
        try {
            cursor = database.query(ConversationTableName,null, "conversationID=? AND userID=?",  arrayOf(conversationID.toString(), userID),null,null,null)
            cursor.moveToFirst()
            if (!cursor.isAfterLast) {
                conversation = cursorToConversation(cursor)
                conversation.saved = true
            }
            cursor.close()
        } catch (e: SQLException) {
            Log.e(TAG, e.message.toString())
        }
        if (conversation != null) {
            if (conversation.conversationID == conversationID){
                val SQL = "UPDATE $ConversationTableName SET dateAccessed=strftime('%s','now')*1000 WHERE conversationID=${conversation.conversationID}"
                Log.d(TAG, SQL)
                database.execSQL(SQL)
            }
        }
        return conversation
    }
    fun updateConversationModified(conversation: Conversation) {
        conversation.dateModified = Date()
        updateConversationDates(conversation)
    }

    //---------------------------------- Messages  ------------------------------------------
    @SuppressLint("Range")
    private fun cursorToChatMessage(cursor: Cursor): ChatMessageExtended {
        //TODO: Add error handling
        val conversationID = cursor.getLong(cursor.getColumnIndex("conversationID"))
        val role = cursor.getString(cursor.getColumnIndex("role"))
        val content = cursor.getString(cursor.getColumnIndex("content"))
        val timeStamp = cursor.getLong(cursor.getColumnIndex("timeStamp"))
        //Log.d(TAG, "cursorToChatMessage " + conversationID)
        return ChatMessageExtended(conversationID, role, content, timeStamp)
    }

    fun appendMessage(conversation: Conversation, chatMessage: ChatMessageExtended) {
        val values = ContentValues()
        values.put("conversationID", chatMessage.conversationID)
        values.put("role", chatMessage.role)
        values.put("content", chatMessage.content)
        values.put("timeStamp", chatMessage.timeStamp)
        Log.d(TAG, "insertChatMessage " + chatMessage.conversationID)
        database.insert(MessageTableName, null, values)
    }
    fun deleteMessage(message: ChatMessageExtended) {
        val query = "DELETE FROM $MessageTableName WHERE conversationID=? AND timeStamp=?"
        val args = arrayOf(message.conversationID.toString(), message.timeStamp.toString())
        try {
            database.execSQL(query, args)
            Log.d(TAG, "Message deletion succeeded")
        } catch (e: Exception) {
            Log.w(TAG, "Message deletion failed: " + e.message.toString())
        }
    }

    fun getMessages(conversationID: Long): List<ChatMessageExtended> {
        val chatMessages = ArrayList<ChatMessageExtended>()
        //Log.d(TAG, "getChatMessagesByConversationID : " + conversationID)
        val cursor: Cursor
        try {
            cursor = database.query(MessageTableName,null, "conversationID = ?", arrayOf(conversationID.toString()),null, null,"timeStamp")
            //Log.d(TAG, "getMessages found " + cursor.count)
            cursor.moveToFirst()
            while (!cursor.isAfterLast) {
                val chatMessage = cursorToChatMessage(cursor)
                chatMessages.add(chatMessage)
                cursor.moveToNext()
            }
            cursor.close()
        } catch (e: SQLException) {
            Log.w(TAG, e.message.toString())
        }
        return chatMessages
    }

    //---------------------------------- Usage Table ------------------------------------------
    @SuppressLint("Range")
    private fun cursorToUsage(cursor: Cursor): ChatUsage {
        //TODO: Add error handling
        val conversationID = cursor.getLong(cursor.getColumnIndex("conversationID"))
        val promptTokens = cursor.getInt(cursor.getColumnIndex("promptTokens"))
        val completionTokens = cursor.getInt(cursor.getColumnIndex("completionTokens"))
        val totalTokens = cursor.getInt(cursor.getColumnIndex("totalTokens"))
        val userID = cursor.getString(cursor.getColumnIndex("userID"))
        val androidID = cursor.getString(cursor.getColumnIndex("androidID"))
        val timeStamp = cursor.getLong(cursor.getColumnIndex("timeStamp"))
        return ChatUsage(conversationID,promptTokens, completionTokens, totalTokens, userID, androidID, timeStamp)
    }

    fun getConversationUsage(conversationID: Long): Int {
        var result: Int = 0
        val cursor: Cursor
        try {
            cursor = database.query(UsageTableName,null,"conversationID=?", arrayOf(conversationID.toString()), null, null, null)
            cursor.moveToFirst()
            if (!cursor.isAfterLast) {
                while (!cursor.isAfterLast) {
                    val usage = cursorToUsage(cursor)
                    result += usage.totalTokens
                    cursor.moveToNext()
                }
            }
            cursor.close()
        } catch (e: SQLException) {
            Log.e(TAG, e.message.toString())
        }
        return result
    }

    fun getUsage(userID:String, androidID: String): ArrayList<ChatUsage> {
        val result= ArrayList<ChatUsage>()
        val cursor: Cursor
        try {
            cursor = database.query(UsageTableName,null,"userID=? AND androidID=?", arrayOf(userID, androidID), null,null,"timeStamp")
            cursor.moveToFirst()
            if (!cursor.isAfterLast) {
                while (!cursor.isAfterLast) {
                    val usage = cursorToUsage(cursor)
                    result.add(usage)
                    cursor.moveToNext()
                }
            }
            cursor.close()
        } catch (e: SQLException) {
            Log.e(TAG, e.message.toString())
        }
        return result
    }

    @SuppressLint("Range")
    fun getUsageLastUpdated(userID:String, androidID: String): Long {
        val cursor = database.rawQuery("SELECT MAX(timeStamp) AS maxTimeStamp FROM $UsageTableName WHERE userID='$userID' AND androidID='$androidID'", null)
        var maxTimeStamp: Long = 0
        if (cursor.moveToFirst()) {
            maxTimeStamp = cursor.getLong(cursor.getColumnIndex("maxTimeStamp"))
        }
        cursor.close()
        return maxTimeStamp
    }

    fun appendUsage(usage: ChatUsage) {
        Log.d(TAG, "Appending chat usage to SQL ${usage.conversationID} userID: ${usage.userID}")
        val SQL = """
        INSERT INTO $UsageTableName 
        (conversationID, promptTokens, completionTokens, totalTokens, androidID, userID, timeStamp) 
        VALUES 
        (${usage.conversationID}, ${usage.promptTokens}, ${usage.completionTokens}, ${usage.totalTokens}, '${usage.androidID}', '${usage.userID}', ${usage.timeStamp});
        """.trimIndent()

        try {
            database.execSQL(SQL)
        } catch (e: Exception) {
            Log.e(TAG, "Error appending chat usage to SQL: ${e.message}")
        }
    }

    //-------------------------------------------  Notes --------------------------------------------------

    @SuppressLint("Range")
    private fun cursorToNoteEntry(cursor: Cursor): NoteEntry {
        val noteID = cursor.getLong(cursor.getColumnIndex("noteID"))
        val categoryID = cursor.getInt(cursor.getColumnIndex("categoryID"))
        val categoryName = getNoteCategoryName(categoryID)
        val title = cursor.getString(cursor.getColumnIndex("title"))
        val content = cursor.getString(cursor.getColumnIndex("content"))
        val dateCreated = Date(cursor.getLong(cursor.getColumnIndex("dateCreated")))
        val dateAccessed = Date(cursor.getLong(cursor.getColumnIndex("dateAccessed")))
        val dateModified = Date(cursor.getLong(cursor.getColumnIndex("dateModified")))
        return NoteEntry(noteID, categoryID, categoryName, title, content, dateCreated, dateAccessed, dateModified)
    }

    @SuppressLint("Range")
    private fun cursorToNoteCategory(cursor: Cursor): NoteCategory {
        val categoryID = cursor.getInt(cursor.getColumnIndex("categoryID"))
        val categoryName = cursor.getString(cursor.getColumnIndex("categoryName"))
        return NoteCategory(categoryID, categoryName)
    }

    private fun cacheNoteCategories() {
        noteCategories = ArrayList<NoteCategory>()
        var sql = "SELECT categoryID, categoryName FROM $NotesCategoryTableName ORDER BY categoryID"
        try {
            val cursor = database.rawQuery(sql, null)
            cursor.moveToFirst()
            while (!cursor.isAfterLast) {
                val cat = cursorToNoteCategory(cursor)
                noteCategories.add(cat)
                //Log.d(TAG, "NoteCategory: ${cat.categoryID}: ${cat.categoryName}")
                cursor.moveToNext()
            }
            cursor.close()
        } catch (e: SQLException) {
            Log.e(TAG, e.message.toString())
        }
        if (noteCategories.size < 2) {
            noteCategories = defaultNoteCategories()
            for (c in noteCategories) {createNoteCategory(c.categoryName,c.categoryID)}
        }
    }

    fun getNoteCategoryMaxID():Int {
        var result = 0
        if (noteCategories.size ==0) { cacheNoteCategories() }
        for (c in noteCategories) {
            if (c.categoryID > result) {result = c.categoryID}
        }
        return result
    }

    fun getNoteCategoryID(categoryName: String = ""):Int {
        var result:Int = -1
        if (noteCategories.size ==0) { cacheNoteCategories() }
        for (c in noteCategories) {
            if (c.categoryName == categoryName) { result = c.categoryID}
        }
        return result
    }

    fun getNoteCategoryName(categoryID: Int):String {
        var result = ""
        if (noteCategories.size ==0) { cacheNoteCategories() }
        for (c in noteCategories) {
            if (c.categoryID==categoryID) {result = c.categoryName}
        }
        return result
    }

    fun getNoteCategories(includeAnyOption: Boolean=false): ArrayList<String> {
        if (noteCategories.size ==0) {cacheNoteCategories()}
        val result = ArrayList<String>()
        if (includeAnyOption) { result.add(0, "Any")  }
        for (c in noteCategories) { result.add(c.categoryName) }
        return result
    }

    fun createNoteCategory(categoryName: String, categoryID: Int=-1) {
        var newCategoryID = categoryID
        if (newCategoryID <0) {newCategoryID = getNoteCategoryMaxID() + 1}
        val query = "INSERT INTO ${NotesCategoryTableName} (categoryID, categoryName) SELECT ${newCategoryID}, '${categoryName}' WHERE NOT EXISTS (SELECT 1 FROM ${NotesCategoryTableName} WHERE categoryName ='${categoryName}')"
        try {
            database.execSQL(query)
            Log.d(TAG, "Note category ${categoryName} created")
        } catch (e: Exception) {
            Log.w(TAG, "Note category ${categoryName} creation failed: " + e.message.toString())
        }
    }

    fun deleteNoteCategory(categoryID: Int, replacementCategoryID: Int=0) {
        val query1 = "UPDATE $NotesTableName SET CategoryID=${replacementCategoryID} WHERE categoryID=${categoryID}"
        val query2 = "DELETE FROM $NotesCategoryTableName WHERE categoryID=${categoryID}"
        try {
            database.execSQL(query1)
            database.execSQL(query2)
            Log.d(TAG, "Note category ${categoryID} deletion succeeded")
        } catch (e: Exception) {
            Log.w(TAG, "Note category ${categoryID} deletion failed: " + e.message.toString())
        }
    }

    fun getNotes(categoryID: Int=-1, searchString: String="", limitResultsTo:Int = 0, metaDataOnly:Boolean=false): ArrayList<NoteEntry> {
        val notes = ArrayList<NoteEntry>()
        var sql = "SELECT * FROM $NotesTableName"
        var whereClause = ""
        var searchClause = ""
        if (categoryID >= 0.toLong()) {  whereClause = "categoryID=${categoryID}" }
        if (searchString !="") {
            searchClause = "(Title LIKE '%" + searchString + "%' OR Content LIKE '%" + searchString + "%')"
            if (whereClause == "") {
                whereClause = searchClause
            } else {
                whereClause += " AND " + searchClause
            }
        }
        if (whereClause != "") {   sql += " WHERE " + whereClause  }
        sql += " ORDER BY dateAccessed DESC"
        if (limitResultsTo > 0) {sql += " LIMIT ${limitResultsTo}"}
        //Log.d(TAG, "SQL Search: ${sql}")
        try {
            val cursor = database.rawQuery(sql, null)
            cursor.moveToFirst()
            while (!cursor.isAfterLast) {
                val note = cursorToNoteEntry(cursor)
                if (metaDataOnly) {
                    note.title=""
                    note.content=""
                }
                notes.add(note)
                cursor.moveToNext()
            }
            cursor.close()
        } catch (e: SQLException) {
            Log.e(TAG, e.message.toString())
        }
        return notes
    }

    fun saveNote(note: NoteEntry) {
        Log.d(TAG, "Saving note to SQL " + note.noteID)
        if (note.categoryName == "General") {note.categoryID=0}
        val values = ContentValues()
        values.put("noteID", note.noteID)
        values.put("categoryid", note.categoryID)
        values.put("title", note.title)
        values.put("content", note.content)
        val whereClause = "noteID = ?"
        val whereArgs = arrayOf(note.noteID.toString())
        val rowsAffected = database.update("Notes", values, whereClause, whereArgs)
        if (rowsAffected > 0) {
            println("Note updated successfully")
        } else {
            println("Note update failed, inserting new row")
            database.insert(NotesTableName, null, values)
        }
        updateNoteDates(note)
    }

    fun updateNoteDates(note: NoteEntry) {
        //values.put doesn't seem to work, so I'm using a SQL statement
        val SQL = "UPDATE $NotesTableName SET dateCreated = ${note.dateCreated.time}, dateAccessed = ${note.dateAccessed.time},  dateModified = ${note.dateModified.time} WHERE noteID = ${note.noteID};"
        //Log.d(TAG, SQL)
        database.execSQL(SQL)
    }

    fun getNote(noteID: Long): NoteEntry {
        val cursor: Cursor
        Log.d(TAG, "getNote $noteID")
        var result = NoteEntry()
        try {
            cursor = database.query(NotesTableName, null, "noteID=?", arrayOf(noteID.toString()), null, null, null)
            cursor.moveToFirst()
            while (!cursor.isAfterLast) {
                result = cursorToNoteEntry(cursor)
                cursor.moveToNext()
            }
            cursor.close()
        } catch (e: SQLException) {
            Log.e(TAG, e.message.toString())
        }
        if (result.noteID == 0.toLong()) {result.noteID = Date().time}
        return result
    }

    fun deleteNote(noteID: Long) {
        try {
            database.execSQL("DELETE FROM $NotesTableName WHERE noteID=$noteID")
            database.execSQL("INSERT INTO $NotesDeletionsTableName (noteID, timeStamp) values(${noteID}, ${Date().time})")
            Log.d(TAG, "Note deletion succeeded")
        } catch (e: Exception) {
            Log.w(TAG, "Note deletion failed: " + e.message.toString())
        }
    }

    @SuppressLint("Range")
    fun getDeletedNotes(): ArrayList<Long> {
        val result = ArrayList<Long>()
        val cursor: Cursor
        try {
            cursor = database.query(NotesDeletionsTableName, null, null, null,null, null, null)
            cursor.moveToFirst()
            while (!cursor.isAfterLast) {
                val noteID = cursor.getLong(cursor.getColumnIndex("noteID"))
                result.add(noteID)
                cursor.moveToNext()
            }
            cursor.close()
        } catch (e: SQLException) {
            Log.e(TAG, e.message.toString())
        }
        return result
    }

    @SuppressLint("Range")
    fun getNotesLastUpdated():Long {
        var cursor = database.rawQuery("SELECT MAX(dateModified) AS maxTimeStamp FROM $NotesTableName", null)
        var maxTimeStamp: Long = 0
        if (cursor.moveToFirst()) {
            maxTimeStamp = cursor.getLong(cursor.getColumnIndex("maxTimeStamp"))
        }
        cursor.close()
        cursor = database.rawQuery("SELECT MAX(timeStamp) AS maxTimeStamp FROM $NotesDeletionsTableName", null)
        if (cursor.moveToFirst()) {
            var x = cursor.getLong(cursor.getColumnIndex("maxTimeStamp"))
            if (x> maxTimeStamp) {maxTimeStamp=x}
        }
        cursor.close()
        return maxTimeStamp
    }

    //---------------------------------- Sample Prompts Table ------------------------------------------
    @SuppressLint("Range")
    fun getSamplePrompt(activityName: String, unused: Boolean = false):SamplePrompt {
        var result = SamplePrompt()
        var sql = "SELECT activityName, prompt FROM $SamplePromptTableName WHERE activityName='$activityName' ORDER BY RANDOM() LIMIT 1"
        if (unused) {sql = "SELECT activityName, prompt FROM $SamplePromptTableName where activityName='$activityName' and used=0 ORDER BY RANDOM() LIMIT 1"}
        try {
            val cursor = database.rawQuery(sql, null)
            if (cursor.moveToFirst()) {
                val prompt = cursor.getString(cursor.getColumnIndex("prompt"))
                result = SamplePrompt(activityName, prompt)
            }
            cursor.close()
            database.execSQL("UPDATE $SamplePromptTableName SET used=1 WHERE activityName='$activityName' and prompt='${result.prompt.replace("'", "''")}'")
        }
        catch (exception :Exception) {
            Log.e(TAG, "Error getting sample prompts, regenerating table...")
            //dbHelper.applyChatUpdates(database)
        }
        return result
    }

    fun reuseSamplePrompts() {
        database.execSQL("UPDATE $SamplePromptTableName SET used=0")
    }

    @SuppressLint("Range")
    fun getSamplePrompts(cuttoff: Long, unused: Boolean = false):ArrayList<SamplePrompt> {
        var result = ArrayList<SamplePrompt>()
        var sql = "SELECT activityName, prompt FROM $SamplePromptTableName WHERE timeStamp>$cuttoff ORDER BY timeStamp"
        if (unused) {sql = "SELECT activityName, prompt FROM $SamplePromptTableName WHERE timeStamp>$cuttoff and used=0 ORDER BY timeStamp"}
        val cursor = database.rawQuery(sql, null)
        if (!cursor.isAfterLast) {
            while (cursor.moveToNext()) {
                val prompt = cursor.getString(cursor.getColumnIndex("prompt"))
                val activityName = cursor.getString(cursor.getColumnIndex("activityName"))
                result.add(SamplePrompt(activityName, prompt))
            }
        }
        cursor.close()
        return result
    }

    @SuppressLint("Range")
    fun cleanSamplePrompts() {
        var p1: String = ""
        var p2: String = ""
        var ts: Long = 0
        val cursor = database.rawQuery("SELECT prompt, timeStamp FROM $SamplePromptTableName ORDER BY prompt, timeStamp DESC", null)
        if (!cursor.isAfterLast) {
            if (cursor.moveToFirst()) {
                p1 = cursor.getString(cursor.getColumnIndex("prompt"))
                while (cursor.moveToNext()) {
                    p2 = cursor.getString(cursor.getColumnIndex("prompt"))
                    ts = cursor.getLong(cursor.getColumnIndex("timeStamp"))
                    if (p1 == p2) {
                        Log.d(TAG, "Deleting duplicate prompt $p2")
                        database.execSQL("DELETE FROM $SamplePromptTableName WHERE prompt='${p2.replace("'", "''")}' AND timeStamp=$ts")
                    }
                    p1 = p2
                }
            }
        }
        cursor.close()
    }

    fun appendSamplePrompt(timeStamp: Long, sample: SamplePrompt) {
        Log.d(TAG, "Added prompt to SQL... ")
        val prompt = sample.prompt.replace("'", "''")
        val SQL = """
        INSERT INTO $SamplePromptTableName (activityName, prompt, used, timeStamp) 
        VALUES ('${sample.activityName}','${prompt}', ${0}, ${timeStamp});
        """.trimIndent()

        try {
            database.execSQL(SQL)
            Log.d(TAG, "Added sample prompt to SQL: ${sample.activityName} : ${prompt}")
        } catch (e: Exception) {
            Log.e(TAG, "Error writing prompt to SQL: ${e.message}")
        }
    }

    @SuppressLint("Range")
    fun getSamplePromptsLastUpdated(): Long {
        val cursor = database.rawQuery("SELECT MAX(timeStamp) AS maxTimeStamp FROM $SamplePromptTableName", null)
        var maxTimeStamp: Long = 0
        if (cursor.moveToFirst()) {
            maxTimeStamp = cursor.getLong(cursor.getColumnIndex("maxTimeStamp"))
        }
        cursor.close()
        return maxTimeStamp
    }

    @SuppressLint("Range")
    fun getSamplePromptsCount(unused: Boolean = false): Long {
        var sql = "SELECT COUNT(prompt) AS promptCount FROM $SamplePromptTableName"
        if (unused) {sql = "SELECT COUNT(prompt) AS promptCount FROM $SamplePromptTableName WHERE used=0"}
        Log.d(TAG, sql)
        var result: Long = 0
        val cursor = database.rawQuery(sql, null)
        if (cursor.moveToFirst()) {
            result = cursor.getLong(cursor.getColumnIndex("promptCount"))
        }
        cursor.close()
        return result
    }

    //---------------------------------- Chat Modes Table ------------------------------------------
    @SuppressLint("Range")
    fun getChatModes():ArrayList<ChatActivityType> {
        var result = ArrayList<ChatActivityType>()
        var sql = "SELECT activityName, prompt, conversational, clearConversationOnChange, showLanguageOptions, temperature FROM $ChatModesTableName ORDER BY sequence"
        val cursor = database.rawQuery(sql, null)
        if (!cursor.isAfterLast) {
            while (cursor.moveToNext()) {
                val activityName = cursor.getString(cursor.getColumnIndex("activityName"))
                val prompt = cursor.getString(cursor.getColumnIndex("prompt"))
                val conversational = cursor.getInt(cursor.getColumnIndex("conversational")) != 0
                val clearConversationOnChange = cursor.getInt(cursor.getColumnIndex("clearConversationOnChange")) != 0
                val showLanguageOptions = cursor.getInt(cursor.getColumnIndex("showLanguageOptions")) != 0
                val temperature = cursor.getDouble(cursor.getColumnIndex("temperature"))
                result.add(ChatActivityType(activityName, prompt, conversational, clearConversationOnChange, showLanguageOptions, temperature))
            }
        }
        cursor.close()
        return result
    }

    fun clearChatModes() {
        Log.d(TAG, "Clearing $ChatModesTableName")
        database.execSQL("DELETE FROM $ChatModesTableName")
    }

    fun appendChatMode(cm: ChatActivityType, sequence: Int, timeStamp:Long) {
        Log.d(TAG, "Appending chat activitiy type to SQL... " + cm.prompt)
        val prompt = cm.prompt.replace("'", "''")
        val SQL = """
        INSERT INTO $ChatModesTableName (activityName, prompt, conversational,clearConversationOnChange,showLanguageOptions,temperature,sequence,timeStamp) 
        VALUES ('${cm.activityName}','${prompt}', ${if (cm.conversational) 1 else 0}, ${if (cm.clearConversationOnChange) 1 else 0},${if (cm.showLanguageOptions) 1 else 0},${cm.temperature},${sequence},${timeStamp} );
        """.trimIndent()
        try {
            database.execSQL(SQL)
            Log.d(TAG, "Added chat mode to SQL: ${cm.activityName}")
        } catch (e: Exception) {
            Log.e(TAG, "Error writing chat mode to SQL: ${e.message}")
        }
    }

    @SuppressLint("Range")
    fun getChatModesLastUpdated(): Long {
        val cursor = database.rawQuery("SELECT MAX(timeStamp) AS maxTimeStamp FROM $ChatModesTableName", null)
        var maxTimeStamp: Long = 0
        if (cursor.moveToFirst()) {
            maxTimeStamp = cursor.getLong(cursor.getColumnIndex("maxTimeStamp"))
        }
        cursor.close()
        return maxTimeStamp
    }

    //---------------------------------- Fixes and Maintenance ------------------------------------------

    fun applyChatDatabaseFixes(userID:String, androidID: String) {
        Log.d(TAG, "Appling chat database fixes...")
        //dbHelper.applyChatUpdates(database)
        //Log.d(TAG, "Empty userID, set to " + userID)
        database.execSQL("UPDATE $ConversationTableName SET userID='${userID}' WHERE userID=''")
        database.execSQL("UPDATE $UsageTableName SET userID='${userID}' WHERE userID=''")
        //cleanSamplePrompts()

//        try {
//            val cursor = database.query(MessageTableName,null,null, null, null,null,"timeStamp")
//            if (!cursor.isAfterLast) {
//                cursor.moveToFirst()
//                while (!cursor.isAfterLast) {
//                    val message = cursorToChatMessage(cursor)
//                    val q2 = ChatManager.formatPromptPrettyLike(message.content).replace("'", "''")
//                    if (q2 != message.content) {
//                        var SQL = "UPDATE " + MessageTableName + " SET content='" + q2 + "' WHERE conversationID=" + message.conversationID + " AND timeStamp=" + message.timeStamp
//                        database.execSQL(SQL)
//                        SQL = "UPDATE $ConversationTableName SET dateModified = ${Date().time} WHERE  conversationID = ${message.conversationID};"
//                        database.execSQL(SQL)
//                        //Log.i(TAG, SQL)
//                    }
//                    //Log.i(TAG, message.content)
//                    cursor.moveToNext()
//                }
//            }
//            cursor.close()
//        } catch (e: SQLException) {
//            Log.e(TAG, e.message.toString())
//        }
        if (androidID !=""){
            //Log.d(TAG, "Empty androidID, set to " + androidID)
            database.execSQL("UPDATE $UsageTableName SET androidID='${androidID}' WHERE androidID=''")
        }
        Log.d(TAG, "Appling database fixes complete")
    }

}