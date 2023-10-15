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

private class SQLiteHelper(
    context: Context,
    name: String = DATABASE_NAME,
    factory: SQLiteDatabase.CursorFactory? = null,
    version: Int = DATABASE_VERSION
) : SQLiteOpenHelper(context, name, factory, version) {
    companion object {
        const val DATABASE_VERSION = 1
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
        private const val CREATE_CONVERSATION_DELETED_TABLE = """ CREATE TABLE IF NOT EXISTS $DeletionsTableName ( conversationID LONG  )        """

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

    }

    override fun onCreate(db: SQLiteDatabase) {
        Log.d("SQLiteHelper", "Creating database...")
        db.execSQL(CREATE_CONVERSATION_TABLE)
        db.execSQL(CREATE_CONVERSATION_DELETED_TABLE)
        db.execSQL(CREATE_MESSAGES_TABLE)
        db.execSQL(CREATE_USAGE_TABLE)
        db.execSQL(CREATE_SAMPLEPROMPTS_TABLE)
    }

    fun recreateDatabase(db: SQLiteDatabase) {
        Log.d("SQLiteHelper", "Deleting and re-creating database...")
        db.execSQL("DROP TABLE IF EXISTS $ConversationTableName")
        db.execSQL("DROP TABLE IF EXISTS $DeletionsTableName")
        db.execSQL("DROP TABLE IF EXISTS $MessageTableName")
        db.execSQL("DROP TABLE IF EXISTS $UsageTableName")
        db.execSQL("DROP TABLE IF EXISTS $SamplePromptTableName")
        onCreate(db)
    }

    fun applyUpdates (db: SQLiteDatabase) {
        Log.d("SQLiteHelper", "Updating database...")
        //db.execSQL(CREATE_CONVERSATION_DELETED_TABLE)
        db.execSQL("DROP TABLE IF EXISTS $SamplePromptTableName")
        db.execSQL(CREATE_SAMPLEPROMPTS_TABLE)
//        db.execSQL(CREATE_USAGE_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        recreateDatabase(db)
    }
}

class SQLiteManager(context: Context) {
    //TODO: remove references to deviceID or androidID, this is purely local userID is blank or GoogleID
    private val TAG = "SQLiteDataSource"
    private val dbHelper: SQLiteHelper = SQLiteHelper(context)
    private val database: SQLiteDatabase = dbHelper.writableDatabase

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
            database.execSQL("DELETE FROM $DeletionsTableName")
            database.execSQL("DELETE FROM $MessageTableName")
            database.execSQL("DELETE FROM $UsageTableName")
        }
    }

    //---------------------------------- Conversations  ------------------------------------------
    @SuppressLint("Range")
    private fun cursorToConversation(cursor: Cursor): Conversation {
        //TODO: Add error handling, so far hasn't been necessary
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
        database.execSQL("INSERT INTO $DeletionsTableName (conversationID) values($conversationID)")
    }

    @SuppressLint("Range")
    fun getDeletedConversations(userID:String): ArrayList<Long> {
        val result = ArrayList<Long>()
        val cursor: Cursor
        try {
            cursor = database.query(DeletionsTableName, null, null, null,null, null, null)
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
    fun getAllConversations(userID:String): List<Conversation> {
        val conversations = ArrayList<Conversation>()
        val cursor: Cursor
        try {
            cursor = database.query(ConversationTableName, null, "userID=?", arrayOf(userID), null, null, "dateModified DESC")
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
            database.execSQL("UPDATE $SamplePromptTableName SET used=1 WHERE activityName='$activityName' and prompt='${result}'")
        }
        catch (exception :Exception) {
            Log.e(TAG, "Error getting sample prompts, regenerating table...")
            dbHelper.applyUpdates(database)
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
            if (cursor.moveToFirst()) {
                while (cursor.moveToNext()) {
                    val prompt = cursor.getString(cursor.getColumnIndex("prompt"))
                    val activityName = cursor.getString(cursor.getColumnIndex("activityName"))
                    result.add(SamplePrompt(activityName, prompt))
                }
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
                        database.execSQL("DELETE FROM $SamplePromptTableName WHERE prompt='${p2}' AND timeStamp=$ts")
                    }
                    p1 = p2
                }
            }
        }
        cursor.close()
    }

    fun appendSamplePrompt(timeStamp: Long, prompt: SamplePrompt) {
        Log.d(TAG, "Appending prompt to SQL... " + prompt.prompt)
        val SQL = """
        INSERT INTO $SamplePromptTableName (activityName, prompt, used, timeStamp) 
        VALUES ('${prompt.activityName}','${prompt.prompt}', ${0}, ${timeStamp});
        """.trimIndent()

        try {
            database.execSQL(SQL)
            Log.d(TAG, "Added sample prompt to SQL: ${prompt.prompt}")
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

    //---------------------------------- Fixes and Maintenance ------------------------------------------

    fun applyFixes(userID:String, androidID: String) {
        Log.d(TAG, "Appling database fixes...")
        dbHelper.applyUpdates(database)
        //Log.d(TAG, "Empty userID, set to " + userID)
        database.execSQL("UPDATE $ConversationTableName SET userID='${userID}' WHERE userID=''")
        database.execSQL("UPDATE $UsageTableName SET userID='${userID}' WHERE userID=''")
        cleanSamplePrompts()
        try {
            val cursor = database.query(MessageTableName,null,null, null, null,null,"timeStamp")
            if (!cursor.isAfterLast) {
                cursor.moveToFirst()
                while (!cursor.isAfterLast) {
                    val message = cursorToChatMessage(cursor)
                    val q2 = ChatManager.formatPromptPrettyLike(message.content)
                    if (q2 != message.content) {
                        var SQL = "UPDATE " + MessageTableName + " SET content='" + q2 + "' WHERE conversationID=" + message.conversationID + " AND timeStamp=" + message.timeStamp
                        database.execSQL(SQL)
                        SQL = "UPDATE $ConversationTableName SET dateModified = ${Date().time} WHERE  conversationID = ${message.conversationID};"
                        database.execSQL(SQL)
                        Log.i(TAG, SQL)
                    }
                    Log.i(TAG, message.content)
                    cursor.moveToNext()
                }
            }
            cursor.close()
        } catch (e: SQLException) {
            Log.e(TAG, e.message.toString())
        }

        try {
            val cursor = database.query(MessageTableName,null,"role=?", arrayOf("user"), null,null,"timeStamp")
            cursor.moveToFirst()
            if (!cursor.isAfterLast) {
                while (!cursor.isAfterLast) {
                    val message = cursorToChatMessage(cursor)
                    val q2 = ChatManager.formatPromptPrettyLike(message.content)
                    if (q2 != message.content) {
                        var SQL = "UPDATE " + MessageTableName + " SET content='" + q2 + "' WHERE conversationID=" + message.conversationID + " AND timeStamp=" + message.timeStamp
                        database.execSQL(SQL)
                        SQL = "UPDATE $ConversationTableName SET dateModified = ${Date().time} WHERE  conversationID = ${message.conversationID};"
                        database.execSQL(SQL)
                        Log.i(TAG, SQL)
                    }
                    Log.i(TAG, message.content)
                    cursor.moveToNext()
                }
            }
            cursor.close()
        } catch (e: SQLException) {
            Log.e(TAG, e.message.toString())
        }

        if (androidID !=""){
            //Log.d(TAG, "Empty androidID, set to " + androidID)
            database.execSQL("UPDATE $UsageTableName SET androidID='${androidID}' WHERE androidID=''")
        }
        Log.d(TAG, "Appling database fixes complete")
    }
}