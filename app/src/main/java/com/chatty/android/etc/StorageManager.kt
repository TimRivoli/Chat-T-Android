package com.chatty.android.etc

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import java.util.UUID
import com.chatty.android.etc.DataClasses.*
import com.chatty.android.db.AppDatabase
import com.chatty.android.etc.DynamicContentGeneration.generateStarterChatModes
import java.util.Calendar

object StorageManager {
    private const val TAG = "StorageManager"
    var useGoogleAuth: Boolean = false
    @Volatile var settingsLoaded: Boolean = false
    var userID: String = ""
    private var deviceID: String = ""
    var subscriptionLevel: Int = 0
    var API_KEY = ""
    var encryptContent: Boolean = true
    var encryptionPending: Boolean = false
    var contentEncryptionKey = ""
    var webClientID = ""
    private var androidID: String = ""
    private var syncConversations: Boolean = false
    private var syncUsage: Boolean = true
    private var syncInProgress = false
    private var syncNeeded = true
    var lastSynced: Long = 0
    private const val syncCooldown = 83333  //milliseconds = 5 min
    private lateinit var localDb: LocalDatabase
    private lateinit var localStorage: SharedPreferences
    private lateinit var appContext: Context
    var pendingKeyTransferPublicKey: String = ""
    var pendingKeyTransferDeviceModel: String = ""
    private var respondedKeyTransferPublicKey: String = ""

    fun init(context: Context, passedAndroidID: String): StorageManager {
        Log.d(TAG, "Initializing Storage Manager...")
        if (passedAndroidID != "") {
            androidID = passedAndroidID
        }
        appContext = context.applicationContext
        localStorage = context.getSharedPreferences("Prefs", Context.MODE_PRIVATE)
        subscriptionLevel = localStorage.getInt("subscriptionLevel", 0)
        syncUsage = localStorage.getBoolean("syncUsage", true)
        useGoogleAuth = localStorage.getBoolean("useGoogleAuth", false) || FORCE_GOOGLE_AUTH
        syncConversations = localStorage.getBoolean("syncConversations", false) || useGoogleAuth
        deviceID = localStorage.getString("deviceID", "").toString()
        contentEncryptionKey = localStorage.getString("contentEncryptionKey", "").toString()
        webClientID = localStorage.getString("webClientID", "").toString()
        pendingKeyTransferPublicKey = localStorage.getString("pendingKeyTransferPublicKey", "").toString()
        pendingKeyTransferDeviceModel = localStorage.getString("pendingKeyTransferDeviceModel", "").toString()
        respondedKeyTransferPublicKey = localStorage.getString("respondedKeyTransferPublicKey", "").toString()
        if (deviceID == "") {
            deviceID = UUID.randomUUID().toString()
            subscriptionLevel = 1
            val editor = localStorage.edit()
            editor.putString("deviceID", deviceID)
            editor.apply()
        }
        if (!::localDb.isInitialized || !FirebaseManager.isInitialized) {
            Log.d(TAG, "Initializing Room and Firebase Managers...")
            localDb = LocalDatabase(AppDatabase.build(context))
            CoroutineScope(Dispatchers.IO).launch {
                FirebaseManager.initialize(androidID, deviceID, useGoogleAuth)
                if (FirebaseManager.isFunctional) {
                    FirebaseManager.getDeviceSettings()
                }
                settingsLoaded = true
                //generateDynamicContent()
                if (subscriptionLevel > 0) {
                    syncDatabases()
                }
            }
        }
        return StorageManager
    }

    fun postGoogleAuthInit() {
        CoroutineScope(Dispatchers.IO).launch {
            FirebaseManager.getDeviceSettings()
            generateDynamicContent()
            if (subscriptionLevel > 0) {
                syncDatabases()
            }
        }
    }

    fun saveDeviceSettings(subscriptionLevelSent: Int, useGoogleAuthSent: Boolean, syncConversationsSent: Boolean, syncUsageSent: Boolean, webClientIDSent: String) {
        Log.d(TAG, "syncConversations: $syncConversations")
        subscriptionLevel = subscriptionLevelSent
        useGoogleAuth = useGoogleAuthSent
        syncConversations = syncConversationsSent
        syncUsage = syncUsageSent
        webClientID = webClientIDSent
        val editor = localStorage.edit()
        editor.putInt("subscriptionLevel", subscriptionLevel)
        editor.putBoolean("useGoogleAuth", useGoogleAuth)
        editor.putBoolean("syncConversations", syncConversations)
        editor.putBoolean("syncUsage", syncUsage)
        editor.putString("webClientID", webClientID)
        editor.apply()
    }

    fun saveContentEncryptionKey(contentEncryptionKeySent: String) {
        contentEncryptionKey = contentEncryptionKeySent
        val editor = localStorage.edit()
        editor.putString("contentEncryptionKey", contentEncryptionKey)
        editor.apply()
    }

    fun savePendingKeyTransferRequest(publicKey: String, deviceModel: String) {
        if (publicKey == respondedKeyTransferPublicKey) return
        if (publicKey == pendingKeyTransferPublicKey) return
        respondedKeyTransferPublicKey = ""
        pendingKeyTransferPublicKey = publicKey
        pendingKeyTransferDeviceModel = deviceModel
        val editor = localStorage.edit()
        editor.putString("pendingKeyTransferPublicKey", publicKey)
        editor.putString("pendingKeyTransferDeviceModel", deviceModel)
        editor.putString("respondedKeyTransferPublicKey", "")
        editor.apply()
        val intent = Intent().apply {
            setClassName(appContext, "com.chatty.android.KeyTransferApprovalActivity")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        appContext.startActivity(intent)
    }

    fun clearPendingKeyTransferRequest() {
        pendingKeyTransferPublicKey = ""
        pendingKeyTransferDeviceModel = ""
        val editor = localStorage.edit()
        editor.putString("pendingKeyTransferPublicKey", "")
        editor.putString("pendingKeyTransferDeviceModel", "")
        editor.apply()
    }

    fun markKeyTransferResponded() {
        respondedKeyTransferPublicKey = pendingKeyTransferPublicKey
        val editor = localStorage.edit()
        editor.putString("respondedKeyTransferPublicKey", respondedKeyTransferPublicKey)
        editor.apply()
    }

    fun shutDown() {
        localDb.shutDown()
        FirebaseManager.shutDown()
    }

    fun getActivationKey(): String = "$androidID:$deviceID"

    //--------------------------------------------- Storage Manager Public interface ---------------------------------------------
    fun toDateStr(millis: Long): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return dateFormat.format(millis)
    }

    fun toDate(millis: Long): LocalDateTime {
        return Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
    }

    fun getChatModes(): ArrayList<ChatActivityType> {
        var r = localDb.getChatModes()
        Log.d(TAG, "Loading chat modes from SQL ${r.size}")
        if (r.size == 0) {
            r = generateStarterChatModes()
            var counter = 0
            for (a in r) {
                localDb.appendChatMode(a, counter, 946684800000)
                counter += 1
            }
        }
        return r
    }

    fun saveLanguagePref(language: String) {
        val editor = localStorage.edit()
        editor.putString("language", language)
        editor.apply()
    }

    fun getLanguagePref(): String {
        return localStorage.getString("language", "English")!!
    }

    fun saveSpeakOutputPref(speakOutput: Boolean) {
        val editor = localStorage.edit()
        editor.putBoolean("speakOutput", speakOutput)
        editor.apply()
    }

    fun getSpeakOutputPref(): Boolean {
        return localStorage.getBoolean("speakOutput", false)
    }

    fun saveAutoSubmitPref(autoSubmit: Boolean) {
        val editor = localStorage.edit()
        editor.putBoolean("autoSubmit", autoSubmit)
        editor.apply()
    }

    fun getAutoSubmitPref(): Boolean {
        return localStorage.getBoolean("autoSubmit", false)
    }

    //Note: these calls are async so the return values are usually false, however the data classes pass by reference so they can be updated
    fun getConversationList(filterString: String = "", limitResultsTo:Int = 0): ArrayList<Conversation> {
        return ArrayList(localDb.getConversations(userID, filterString, limitResultsTo))
    }

    fun getConversation(conversationID: Long): Conversation {
        val conversation = localDb.getConversation(conversationID, userID)
        conversation.dateAccessed = Date() //Update here rather than sqLiteManager to differential admin operations (sync) from user operations
        localDb.updateConversationDates(conversation)
        return conversation
    }

    fun getConversationUsage(conversationID: Long): Int {
        return localDb.getConversationUsage(conversationID)
    }

    fun saveConversation(conversation: Conversation, messages: ArrayList<ChatMessageExtended>) {
        if (messages.size > 0) {
            Log.d(TAG, "Saving conversation: " + conversation.conversationID)
            syncNeeded = true
            conversation.dateModified = Date()
            conversation.dateAccessed = Date()
            conversation.userID = userID
            conversation.saved = true
            localDb.saveConversation(conversation)
            for (m in messages) {
                localDb.appendMessage(conversation, m)
            }
        }
    }

    fun saveConversationAsNote(conversation: Conversation, messages: ArrayList<ChatMessageExtended>) {
        if (messages.size > 0) {
            Log.d(TAG, "Saving conversation as a Note: " + conversation.conversationID)
            syncNeeded = true
            conversation.userID = userID
            localDb.saveConversation(conversation)
            val title = conversation.summary.take(50)
            var content = ""
            for (m in messages) {
                content += m.role + ": " + m.content + "/n"
            }
            val n = NoteEntry(0,0,"",title, content)
            saveNote(n)
        }
    }

    fun deleteConversation(conversationID: Long) {
        syncNeeded = true
        localDb.deleteConversation(conversationID)
    }

    fun getMessages(conversationID: Long): ArrayList<ChatMessageExtended> {
        return ArrayList(localDb.getMessages(conversationID))
    }

    fun appendMessage(conversation: Conversation, message: ChatMessageExtended) {
        Log.d(TAG, "Appending message: " + conversation.conversationID + ":" + message.conversationID)
        syncNeeded = true
        conversation.dateModified = Date()
        localDb.appendMessage(conversation, message)
        localDb.updateConversationModified(conversation)
    }

    fun deleteMessage(message: ChatMessageExtended) {
        syncNeeded = true
        //if (syncConversations) {FirebaseManager.deleteMessage(message)}
        localDb.deleteMessage(message)
        localDb.updateConversationModified(ChatManager.conversation)
    }

    //-------------------------------------------  Notes --------------------------------------------------

    fun getNotes(categoryID: Int = -1, searchString: String = "", limitResultsTo:Int = 0, metaDataOnly:Boolean=false): ArrayList<NoteEntry> {
        return localDb.getNotes(categoryID, searchString, limitResultsTo, metaDataOnly)
    }

    fun getNote(noteID: Long): NoteEntry {
        val note: NoteEntry = localDb.getNote(noteID)
        note.dateAccessed = Date() //Update here rather than sqLiteManager to differential admin operations (sync) from user operations
        localDb.updateNoteDates(note)
        return note
    }

    fun addSecondsToDate(date: Date, seconds: Int=1): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.add(Calendar.SECOND, 1)
        return calendar.time
    }

    fun saveNote(note: NoteEntry, minorChange: Boolean = false) {
        Log.d(TAG, "Saving note: " + note.noteID)
        syncNeeded = true
        if (minorChange) {
            note.dateModified = addSecondsToDate(note.dateModified, 1)
            Log.d(TAG, "Minor change to note, adding once second to ${note.dateModified}")
        } else {
            note.dateModified = Date()
        }
        note.dateAccessed = Date()
        localDb.saveNote(note)
    }

    fun deleteNote(noteID: Long) {
        syncNeeded = true
        localDb.deleteNote(noteID)
    }

    fun getNoteCategories(includeAnyOption: Boolean=false):ArrayList<String> {
        return localDb.getNoteCategories(includeAnyOption)
    }

    fun getNoteCategoryID(categoryName: String = ""):Int {
        return localDb.getNoteCategoryID(categoryName)
    }

    fun getNoteCategoryName(categoryID: Int):String {
        return localDb.getNoteCategoryName(categoryID)
    }

    fun createNoteCategory(categoryName: String, categoryID:Int=-1) {
        syncNeeded = true
        localDb.createNoteCategory(categoryName, categoryID)
    }

    fun deleteNoteCategory(categoryID: Int) {
        syncNeeded = true
        localDb.deleteNoteCategory(categoryID)
    }

    //-------------------------------------------  Sample Prompts  --------------------------------------------------
    fun getSamplePrompt(activityName: String): SamplePrompt {
        return localDb.getSamplePrompt(activityName)
    }

    fun appendSamplePrompt(timeStamp: Long, prompt: SamplePrompt) {
        localDb.appendSamplePrompt(timeStamp, prompt)
    }

    suspend fun uploadSamplePrompts() {
        localDb.cleanSamplePrompts()
        val prompts = localDb.getSamplePrompts(0, false)
        Log.d(TAG, "Uploading ${prompts.size} sample prompts to firebase")
        FirebaseManager.saveSamplePrompts(prompts)
    }

    //-------------------------------------------  Usage --------------------------------------------------
    fun appendUsage(usage: ChatUsage) {
        usage.userID = userID
        usage.androidID = androidID
        localDb.appendUsage(usage)
    }

    private suspend fun syncNotes() {
        var updates = 0
        var deletions = 0
        var additions = 0
        var foundNote: NoteEntry
        val fbNoteLastUpdated = FirebaseManager.getNotesTOCLastUpdated()
        val sqlNotesLastUpdated = localDb.getNotesLastUpdated()
        Log.d(TAG, "Comparing notes table dates local $sqlNotesLastUpdated (${toDate(sqlNotesLastUpdated)}), remote $fbNoteLastUpdated (${toDate(fbNoteLastUpdated)})")
        val fbNotes: ArrayList<NoteEntry> = FirebaseManager.getNotesTOC()
        if (sqlNotesLastUpdated != fbNoteLastUpdated) {
            var sqlNotes: ArrayList<NoteEntry> = localDb.getNotes()
            Log.d(TAG, "Sync Note Deletions...")
            var deletedItems = FirebaseManager.getDeletedNotes()
            for (n in sqlNotes) {
                if (n.noteID in deletedItems) {
                    localDb.deleteNote(n.noteID)
                    n.noteID = -1
                    deletions += 1
                }
            }
            deletedItems = localDb.getDeletedNotes()
            for (n in fbNotes) {
                if (n.noteID in deletedItems) {
                    FirebaseManager.deleteNote(n.noteID)
                    n.noteID = -1
                    deletions += 1
                }
            }
            FirebaseManager.saveDeletedNotes(deletedItems)

            Log.d(TAG, "Sync Note matches...")
            var noteIDs = ArrayList<Long>()
            for (n in fbNotes) {
                noteIDs.add(n.noteID)
            }
            for (n in sqlNotes) {
                if (n.noteID > 0 && (n.noteID in noteIDs)) {
                    for (nn in fbNotes) {
                        if (n.noteID == nn.noteID) {
                            if (nn.dateModified > n.dateModified) {
                                foundNote = FirebaseManager.getNote(n.noteID) //get actual not, not the TOC shorthand
                                Log.d(TAG, "Sync Note updating SQL version ${foundNote.noteID} ${foundNote.title}")
                                localDb.saveNote(foundNote)
                                updates += 1
                            } else if (nn.dateModified < n.dateModified) {
                                Log.d(TAG, "Sync Note updating Firebase version ${n.noteID} ${n.title}")
                                FirebaseManager.saveNote(n)
                                updates += 1
                            }
                        }
                    }
                }
            }

            Log.d(TAG, "Sync Note missing to Firebase...")
            for (n in sqlNotes) {
                if (n.noteID > 0 && !(n.noteID in noteIDs)) {
                    FirebaseManager.saveNote(n)
                    additions += 1
                }
            }

            Log.d(TAG, "Sync Note missing to SQL...")
            noteIDs = ArrayList<Long>()
            for (n in sqlNotes) {
                noteIDs.add(n.noteID)
            }
            for (n in fbNotes) {
                if (n.noteID > 0 && n.noteID !in noteIDs) {
                    foundNote = FirebaseManager.getNote(n.noteID) //get actual not, not the TOC shorthand
                    localDb.saveNote(foundNote)
                    additions += 1
                }
            }
            if ((additions + updates > 0) && FirebaseManager.isFunctional) {
                sqlNotes = localDb.getNotes()
                Log.d(TAG, "Updating Firebase Notes TOC")
                FirebaseManager.makeNotesTOC(sqlNotes)
            }
        }
        if (fbNoteLastUpdated == 0L && FirebaseManager.isFunctional) {
            Log.d(TAG, "Notes TOC not found, creating...")
            FirebaseManager.makeNotesTOC(localDb.getNotes())
        }
        Log.d(TAG, "Sync Notes Completed. Additions: $additions Updates: $updates  Deletions: $deletions")
    }


    suspend fun syncConversations() {
        val syncConversationsDown = syncConversations
        val syncConversationsUp = syncConversations
        var TOCRefreshNeeded = false
        var found: Boolean
        var conversationUpdates = 0
        var messageUpdates = 0
        var deletions = 0

        val fbConversationsLastUpdated = FirebaseManager.getConversationTOCLastUpdated()
        if (syncConversations && FirebaseManager.isFunctional) {
            val sqlConversationsLastUpdated = localDb.getConversationsLastUpdated(userID)
            Log.d(TAG, "Comparing conversation table dates local $sqlConversationsLastUpdated (${toDate(sqlConversationsLastUpdated)}), remote $fbConversationsLastUpdated (${toDate(fbConversationsLastUpdated)})")
            var fbConversations: ArrayList<Conversation> = FirebaseManager.getConversationTOC()
            if (sqlConversationsLastUpdated != fbConversationsLastUpdated || encryptContent) {
                if (encryptionPending) {
                    fbConversations = ArrayList<Conversation>()
                    if (encryptContent) {
                        Log.d(TAG, "Encryption is pending.  Setting fbConversations to empty list to trigger re-write.")
                    } else {
                        Log.d(TAG, "Decryption is pending.  Setting fbConversations to empty list to trigger re-write.")
                    }
                }
                var sqlConversations: ArrayList<Conversation> = ArrayList(localDb.getConversations(userID))
                var messagesFB: ArrayList<ChatMessageExtended>
                var messagesSQL: ArrayList<ChatMessageExtended>
                Log.d(TAG, "Firebase conversations: " + fbConversations.size)
                Log.d(TAG, "SQL conversations: " + sqlConversations.size)

                //------------------------------ Part I deletions  -----------------------------------------
                if (FirebaseManager.isFunctional) {
                    val fbDeletedConversations = FirebaseManager.getDeletedConversations()
                    val sqlDeletedConversations = localDb.getDeletedConversations(userID)
                    Log.d(TAG, "Begin clearing deleted conversations from Firebase...${sqlDeletedConversations.size}")
                    var iterator = fbConversations.iterator()
                    while (iterator.hasNext()) {
                        val c = iterator.next()
                        if (c.conversationID in sqlDeletedConversations) {
                            Log.d(TAG, "Deleting ${c.conversationID} from Firebase")
                            FirebaseManager.deleteConversation(c.conversationID)
                            iterator.remove()
                            deletions += 1
                        }
                    }
                    if (deletions > 0) {
                        FirebaseManager.saveDeletedConversations(sqlDeletedConversations)
                        Log.d(TAG, "Deleted $deletions FireBase conversations")
                    }
                    Log.d(TAG, "Begin clearing deleted conversations from SQL...")
                    deletions = 0
                    iterator = sqlConversations.iterator()
                    while (iterator.hasNext()) {
                        val c = iterator.next()
                        if (c.conversationID in fbDeletedConversations) {
                            Log.d(TAG, "Deleting ${c.conversationID} from SQL")
                            localDb.deleteConversation(c.conversationID)
                            iterator.remove()
                            deletions += 1
                        }
                    }
                    if (deletions > 0) {
                        Log.d(TAG, "Deleted $deletions SQL conversations")
                    }
                }

                //------------------------------ Part II Sync changes up to Firebase  -----------------------------------------
                if (syncConversationsUp && FirebaseManager.isFunctional) {
                    Log.d(TAG, "Begin conversation sync up to Firebase...")
                    conversationUpdates = 0
                    for (c in sqlConversations) {
                        found = false
                        for (cc in fbConversations) {
                            if (c.conversationID == cc.conversationID) {
                                found = true
                                //Log.d(TAG,"Conversation match: " + c.conversationID + " SQL: " + c.dateModified + " FB: " + cc.dateModified                                        )
                                if (c.dateModified != cc.dateModified) {
                                    //messagesFB = FirebaseManager.getMessages(c.conversationID)
                                    val t = FirebaseManager.getConversation(c.conversationID)
                                    val fbConv = t.first    //More complete conversation than the TOC provides, use instead off cc
                                    messagesFB = t.second
                                    if (messagesFB.size == 0) {
                                        Log.w(TAG, "Zero messages returned from getMessagesFirebase") //retrieval may have just failed so don't do anything
                                    } else {
                                        messagesSQL = ArrayList(localDb.getMessages(c.conversationID))
                                        messagesSQL.sortBy { it.timeStamp }
                                        messagesFB.sortBy { it.timeStamp }
                                        val sqlSet = messagesSQL.map { it.timeStamp }.toSet()
                                        val fbSet = messagesFB.map { it.timeStamp }.toSet()
                                        if (c.dateModified > cc.dateModified) {
                                            Log.d(TAG, "Conversation found but SQL is newer, update Firebase version...")
                                            Log.d(TAG, "conversationID: ${c.conversationID} SQL: ${c.dateModified} FB: ${cc.dateModified} ")
                                            FirebaseManager.saveConversation(c, messagesSQL)
                                            conversationUpdates += 1
                                        } else {
                                            Log.d(TAG, "Conversation found but Firebase is newer, update SQL version...")
                                            val iterator = messagesSQL.iterator()
                                            while (iterator.hasNext()) {
                                                val message = iterator.next()
                                                if (message.timeStamp !in fbSet) {
                                                    localDb.deleteMessage(message)
                                                    iterator.remove()
                                                    messageUpdates += 1
                                                }
                                            }
                                            for (message in messagesFB) {
                                                if (message.timeStamp !in sqlSet) {
                                                    localDb.appendMessage(fbConv, message)
                                                    messagesSQL.add(message)
                                                    messageUpdates += 1
                                                }
                                            }
                                            localDb.updateConversationDates(fbConv)
                                            conversationUpdates += 1
                                        }
                                    }
                                }
                            }
                        }
                        if (!found) {
                            conversationUpdates += 1
                            messagesSQL = localDb.getMessages(c.conversationID) as ArrayList<ChatMessageExtended>
                            if (messagesSQL.size == 0) {
                                Log.d(TAG, "Deleting empty conversation from SQL...")
                                localDb.deleteConversation(c.conversationID)
                            } else {
                                Log.d(TAG, "Appending missing conversation ${c.conversationID} to Firebase...")
                                c.saved = true
                                c.userID = userID
                                FirebaseManager.saveConversation(c, messagesSQL)
                            }
                        }
                    }
                    Log.d(TAG, "SyncUp updates: $conversationUpdates: $messageUpdates")
                    if (conversationUpdates > 0) {
                        TOCRefreshNeeded = true
                    }
                }
                //------------------------------ Part III Sync changes down from Firebase  -----------------------------------------
                if (syncConversationsDown && FirebaseManager.isFunctional) {
                    Log.d(TAG, "Begin database sync down from Firebase...")
                    conversationUpdates = 0
                    messageUpdates = 0
                    localDb.applyChatDatabaseFixes(userID, androidID)
                    for (c in fbConversations) {
                        found = false
                        for (cc in sqlConversations) {
                            if (c.conversationID == cc.conversationID) {
                                found = true
                            }
                        }
                        if (!found) {
                            //messagesFB = FirebaseManager.getMessages(c.conversationID)
                            val t = FirebaseManager.getConversation(c.conversationID)
                            val fbConv = t.first    //More complete conversation than the TOC provides, use instead off cc
                            messagesFB = t.second
                            if (messagesFB.size == 0) {
                                Log.d(TAG, "Empty conversation found in Firebase...${c.conversationID}")
//                                        Log.d(TAG, "Deleting empty conversation from Firebase...")
//                                        deleteConversationFirebase(c.conversationID)
                            } else {
                                conversationUpdates += 1
                                Log.d(TAG, "Appending missing conversation ${c.conversationID} to SQL...")
                                fbConv.saved = true
                                fbConv.userID = userID
                                sqlConversations.add(fbConv)
                                localDb.saveConversation(fbConv)
                                for (m in messagesFB) {
                                    localDb.appendMessage(fbConv, m)
                                }
                            }
                        }
                    }
                    Log.d(TAG, "SyncDown updates: $conversationUpdates: $messageUpdates")
                    if (conversationUpdates > 0) {
                        TOCRefreshNeeded = true
                    }
                }
                if (TOCRefreshNeeded && FirebaseManager.isFunctional) {
                    sqlConversations = ArrayList(localDb.getConversations(userID))
                    Log.d(TAG, "Updating Firebase TOC")
                    FirebaseManager.makeConversationTOC(sqlConversations)
                }
            }
        }
        Log.d(TAG, "Sync Conversations Completed. ConversationUpdates: $conversationUpdates MessageUpdates: $messageUpdates  Deletions: $deletions")
    }

    //-------------------------------------------  Prices Working Set  --------------------------------------------------
    fun getPricesWorkingSet(): ArrayList<PriceWorkingSetEntry> {
        return localDb.getPricesWorkingSet()
    }

    private suspend fun syncPrices() {
        val fbLastUpdated = FirebaseManager.getPricesWorkingSetTOCLastUpdated()
        val sqlLastUpdated = localDb.getPricesLastUpdated()
        Log.d(TAG, "Comparing prices dates local $sqlLastUpdated (${toDate(sqlLastUpdated)}), remote $fbLastUpdated (${toDate(fbLastUpdated)})")
        val noLocalData = sqlLastUpdated == 0L
        if (noLocalData || fbLastUpdated > sqlLastUpdated) {
            val prices = FirebaseManager.getPricesWorkingSet()
            if (prices.isNotEmpty()) {
                localDb.clearPrices()
                for (entry in prices) {
                    localDb.savePriceEntry(entry)
                }
                Log.d(TAG, "Sync Prices Completed. Downloaded ${prices.size} entries")
            } else {
                Log.w(TAG, "Sync Prices: Firestore returned no entries")
            }
        } else {
            Log.d(TAG, "Prices are up to date, no sync needed")
        }
    }

    private suspend fun syncUsage() {
        var usageUpdates = 0
        Log.d(TAG, "Uploading usage to Firebase...")
        val usages = localDb.getUsage(userID, androidID)
        usageUpdates = FirebaseManager.saveUsage(usages)
        Log.d(TAG, "SyncUp usage updates: $usageUpdates")
    }

    private suspend fun syncSamplePrompts() {
        if (localDb.getSamplePromptsCount(true) < 5) {
            val t1 = localDb.getSamplePromptsLastUpdated()
            val t2 = FirebaseManager.getSamplePromptsLastUpdated()
            Log.d(TAG, "Checking sample prompts local $t1 (${toDate(t1)}) vs remote $t2 (${toDate(t2)})")
            if (t2 > t1) {
                localDb.applyChatDatabaseFixes(userID, androidID)
                val prompts = FirebaseManager.getSamplePrompts(t1)
                Log.d(TAG, "Checking sample prompts.... found ${prompts.size}")
                for (p in prompts) {
                    localDb.appendSamplePrompt(t2, p)
                }
                localDb.cleanSamplePrompts()
            }
        }
    }

    private suspend fun syncChatModes() {
        val t1 = localDb.getChatModesLastUpdated()
        val t2 = FirebaseManager.getChatModesLastUpdated()
        Log.d(TAG, "Checking chat modes local $t1 (${toDate(t1)}) vs remote $t2 (${toDate(t2)})")
        if (t2 <= 1717273220660) {
            Log.d(TAG, "Uploading starter chat modes to FireBase...")
            FirebaseManager.saveChatModes(generateStarterChatModes())
        } else if (t2 > t1) {
            localDb.applyChatDatabaseFixes(userID, androidID)
            val chatModes = FirebaseManager.getChatModes()
            if (chatModes.size > 0) {
                Log.d(TAG, "Updating new chat modes from FireBase... very exciting!")
                localDb.clearChatModes()
                var counter = 0
                for (m in chatModes) {
                    Log.d(TAG, "Mode: ${m.activityName}")
                    localDb.appendChatMode(m, counter, t2)
                    counter += 1
                }
            }
        }
    }

    private suspend fun generateDynamicContent() {
        DynamicContentGeneration.generateSamplePrompts()
        //uploadSamplePrompts()
    }

    fun syncDatabases() {
        val timeSinceLastSync = Date().time - lastSynced
        Log.d(TAG, "syncDatbase - isNetworkGood: ${NetworkManager.isNetworkGood} firebase isFunctional: ${FirebaseManager.isFunctional} syncInProgress: ${syncInProgress}")
        if (NetworkManager.isNetworkGood && FirebaseManager.isFunctional && !syncInProgress) {
            if (!syncNeeded || timeSinceLastSync <= syncCooldown) {
                Log.d(TAG, "Sync is not needed or on cooldown.  Needed: $syncNeeded  Cooldown: $timeSinceLastSync")
                lastSynced = Date().time
                FirebaseManager.updateLastSynced()
                syncNeeded = false
                syncInProgress = false
            } else {
                syncInProgress = true   //Prevent concurrent syncs
                CoroutineScope(Dispatchers.IO).launch {
                    syncSamplePrompts()
                    syncChatModes()
                    if (syncUsage) { syncUsage() }
                    if (syncConversations && FirebaseManager.isFunctional) { syncConversations() }
                    if (syncConversations && FirebaseManager.isFunctional && !encryptionPending) { syncNotes() }
                    if (syncConversations && FirebaseManager.isFunctional) { syncPrices() }
                    Log.d(TAG, "Database sync completed")
                    lastSynced = Date().time
                    FirebaseManager.updateLastSynced()
                    syncNeeded = false
                    syncInProgress = false
                }
            }
        }
    }
}
