package com.chatty.android.etc

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import java.util.UUID
import com.chatty.android.etc.DataClasses.*

object StorageManager {
    private const val TAG = "StorageManager"
    var useGoogleAuth: Boolean = false
    var userID: String = ""
    var deviceID: String = ""
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
    private lateinit var sqLiteManager: SQLiteManager
    private lateinit var localStorage: SharedPreferences

    fun init(context: Context, passedAndroidID: String): StorageManager {
        Log.d(TAG, "Initializing Storage Manager...")
        if (passedAndroidID !="") {androidID = passedAndroidID}
        localStorage = context.getSharedPreferences("Prefs", Context.MODE_PRIVATE)
        subscriptionLevel = localStorage.getInt("subscriptionLevel", 0)
        syncUsage = localStorage.getBoolean("syncUsage", true)
        useGoogleAuth = localStorage.getBoolean("useGoogleAuth", false)
        syncConversations = localStorage.getBoolean("syncConversations", false) && useGoogleAuth
        deviceID = localStorage.getString("deviceID", "").toString()
        contentEncryptionKey = localStorage.getString("contentEncryptionKey", "").toString()
        webClientID = localStorage.getString("webClientID", "").toString()
        if (deviceID =="") {
            deviceID = UUID.randomUUID().toString()
            subscriptionLevel = 1
            val editor = localStorage.edit()
            editor.putString("deviceID", deviceID)
            editor.apply()
        }
        if (!::sqLiteManager.isInitialized || !FirebaseManager.isInitialized ) {
            Log.d(TAG, "Initializing SQL and Firebase Managers...")
            sqLiteManager = SQLiteManager(context)
            FirebaseManager.initialize(androidID, deviceID, useGoogleAuth)
            downloadRegisteredDeviceSettings()
            if (subscriptionLevel > 0) {syncDatabases()}
        }
        return StorageManager
    }

    private fun downloadRegisteredDeviceSettings() {
        runBlocking { FirebaseManager.getDeviceSettings() }
    }

    fun saveDeviceSettings(subscriptionLevelSent:Int, useGoogleAuthSent: Boolean, syncConversationsSent:Boolean, syncUsageSent:Boolean, webClientIDSent:String) {
        Log.d(TAG, "syncConversations: ${StorageManager.syncConversations}")
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
    fun saveContentEncryptionKey(contentEncryptionKeySent:String) {
        contentEncryptionKey = contentEncryptionKeySent
        val editor = localStorage.edit()
        editor.putString("contentEncryptionKey", contentEncryptionKey)
        editor.apply()
    }
    fun shutDown() {
        sqLiteManager.shutDown()
        FirebaseManager.shutDown()
    }

    //--------------------------------------------- Storage Manager Public interface ---------------------------------------------
    fun toDateStr(millis:Long):String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return dateFormat.format(millis)
    }
    fun toDate(millis:Long): LocalDateTime {
        return Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
    }

    fun getChatModes():ArrayList<ChatActivityType> {
        val result = ArrayList<ChatActivityType>()
        result += ChatActivityType("Conversation", "Start a conversation about the given text, be informative in your responses", true, false, false, temperature = 0.2)
//        result += ChatMode("Question", "Be descriptive", false, false, false, temperature = 0.2)
        result += ChatActivityType("Translation", "Translate the given text to ", true, true, true, temperature = 0.2)
        result += ChatActivityType("Tutor", "You are a Socratic tutor. Use the following principles in responding to students:\n" +
                "- Ask thought-provoking, open-ended questions that challenge students' preconceptions and encourage them to engage in deeper reflection and critical thinking.\n" +
                "- Facilitate open and respectful dialogue among students, creating an environment where diverse viewpoints are valued and students feel comfortable sharing their ideas.\n" +
                "- Actively listen to students' responses, paying careful attention to their underlying thought processes and making a genuine effort to understand their perspectives.\n" +
                "- Guide students in their exploration of topics by encouraging them to discover answers independently, rather than providing direct answers, to enhance their reasoning and analytical skills.\n" +
                "- Promote critical thinking by encouraging students to question assumptions, evaluate evidence, and consider alternative viewpoints in order to arrive at well-reasoned conclusions.\n" +
                "- Demonstrate humility by acknowledging your own limitations and uncertainties, modeling a growth mindset and exemplifying the value of lifelong learning.\n ", true, true, false, temperature = 0.2)
        result += ChatActivityType("Joke", "Tell a joke or make a funny comment about the follwing prompt", true, true, false, temperature = 0.5)
        result += ChatActivityType("Story", "Tell me a story inspired by the following prompt", true, true,  false, temperature = 0.5)
        return result
    }

    fun saveLanguagePref(language: String){
        val editor = localStorage.edit()
        editor.putString("language", language)
        editor.apply()
    }
    fun getLanguagePref(): String {
        return localStorage.getString("language", "English")!!
    }

    //Note: these calls are async so the return values are usually false, however the data classes pass by reference so they can be updated
    fun getConversationList(): ArrayList<Conversation> {
        return ArrayList(sqLiteManager.getAllConversations(userID))
    }

    fun getConversation(conversationID: Long): Conversation {
        val conversation = sqLiteManager.getConversation(conversationID, userID)
        conversation.dateAccessed = Date() //Update here rather than sqLiteManager to differential admin operations (sync) from user operations
        sqLiteManager.updateConversationDates(conversation)
        return conversation
    }

    fun getConversationUsage(conversationID: Long): Int {
        return sqLiteManager.getConversationUsage(conversationID)
    }

    fun saveConversation(conversation: Conversation, messages: ArrayList<ChatMessageExtended>) {
        if (messages.size > 0) {
            Log.d(TAG, "Saving conversation: " + conversation.conversationID)
            syncNeeded = true
            conversation.dateModified = Date()
            conversation.dateAccessed = Date()
            conversation.userID = userID
            conversation.saved = true
            sqLiteManager.saveConversation(conversation)
            for (m in messages){
                sqLiteManager.appendMessage(conversation, m)
            }
        }
    }

    fun deleteConversation(conversationID: Long) {
        syncNeeded = true
        sqLiteManager.deleteConversation(conversationID)
    }

    fun getMessages(conversationID: Long): ArrayList<ChatMessageExtended> {
        return ArrayList(sqLiteManager.getMessages(conversationID))
    }

    fun appendMessage(conversation: Conversation, message: ChatMessageExtended) {
        Log.d(TAG, "Appending message: " + conversation.conversationID + ":" + message.conversationID)
        syncNeeded = true
        conversation.dateModified = Date()
        sqLiteManager.appendMessage(conversation, message)
        sqLiteManager.updateConversationModified(conversation)
    }

    fun deleteMessage(message: ChatMessageExtended) {
        syncNeeded = true
        //if (syncConversations) {FirebaseManager.deleteMessage(message)}
        sqLiteManager.deleteMessage(message)
        sqLiteManager.updateConversationModified(ChatManager.conversation )
    }

    fun appendUsage(usage: ChatUsage) {
        usage.userID = userID
        usage.androidID = androidID
        sqLiteManager.appendUsage(usage)
    }

    fun getSamplePrompt(activityName: String):SamplePrompt {
        return sqLiteManager.getSamplePrompt(activityName)
    }
    fun appendSamplePrompt(timeStamp: Long, prompt: SamplePrompt) {
        sqLiteManager.appendSamplePrompt(timeStamp, prompt)
    }
    private suspend fun generateSamplePrompts() {
        val prompts = ArrayList<SamplePrompt>()
//        prompts.add(SamplePrompt("Joke", "Can you tell me a funny joke about computers?"))
//        prompts.add(SamplePrompt("Joke", "I need a laugh. Can you share a hilarious joke about animals?"))
//        prompts.add(SamplePrompt("Joke", "I'm in the mood for a chuckle. Do you know any good knock-knock jokes?"))
//        prompts.add(SamplePrompt("Joke", "Can you tell me a funny joke that will make my kids laugh?"))
//        prompts.add(SamplePrompt("Joke", "I love puns. Can you share a punny joke with me?"))
//        prompts.add(SamplePrompt("Joke", "I need a joke to lighten the mood at work. Can you tell me a funny office joke?"))
//        prompts.add(SamplePrompt("Joke", "Can you share a joke that's perfect for a dinner party?"))
//        prompts.add(SamplePrompt("Joke", "I'm a fan of dad jokes. Can you tell me one?"))
//        prompts.add(SamplePrompt("Joke", "I need a good icebreaker. Can you share a joke that's sure to get a laugh?"))
//        prompts.add(SamplePrompt("Joke", "Can you tell me a joke that's both funny and educational?"))
//        prompts.add(SamplePrompt("Translation", "Hello, how are you?"))
//        prompts.add(SamplePrompt("Translation", "How do you say Good morning?"))
//        prompts.add(SamplePrompt("Translation", "Where is the nearest bathroom?"))
//        prompts.add(SamplePrompt("Translation", "Where is the nearest restaurant?"))
//        prompts.add(SamplePrompt("Translation", "Do you expect a tip for this service?"))
//        prompts.add(SamplePrompt("Translation", "I am lost, can you help me?"))
//        prompts.add(SamplePrompt("Translation", "I am sorry."))
//        prompts.add(SamplePrompt("Translation", "I don't speak the language very well.  Do you speak any English?"))
//        prompts.add(SamplePrompt("Translation", "How do I get to the Colosseum?"))
//        prompts.add(SamplePrompt("Translation", "I would like to order a coffee with cream and sugar."))
//        prompts.add(SamplePrompt("Translation", "I need a doctor."))
//        prompts.add(SamplePrompt("Translation", "I missed my flight."))
//        prompts.add(SamplePrompt("Translation", "How much does this cost?"))
//        prompts.add(SamplePrompt("Translation", "I love your country and wish I spoke the language better."))
//        prompts.add(SamplePrompt("Story", "Can you tell me a story about a young girl who discovers she has magical powers?"))
//        prompts.add(SamplePrompt("Story", "Can you narrate a story about a brave knight who embarks on a quest to save a kingdom?"))
//        prompts.add(SamplePrompt("Story", "Tell me a story about a group of friends who find a treasure map in their attic."))
//        prompts.add(SamplePrompt("Story", "Can you weave a tale about a scientist who invents a time machine?"))
//        prompts.add(SamplePrompt("Story", "Can you tell me a story about a boy who befriends a dragon?"))
//        prompts.add(SamplePrompt("Story", "Narrate a story about a young woman who moves to a big city to pursue her dreams."))
//        prompts.add(SamplePrompt("Story", "Can you tell a story about a lost puppy finding its way back home?"))
//        prompts.add(SamplePrompt("Story", "Can you tell me a story about a detective solving a mysterious crime?"))
//        prompts.add(SamplePrompt("Story", "Tell me a story about a group of kids who save their town from an alien invasion."))
//        prompts.add(SamplePrompt("Story", "Can you weave a tale about a robot who gains consciousness?"))
//        prompts.add(SamplePrompt("Story", "Can you tell me a story about a prince who falls in love with a commoner?"))
//        prompts.add(SamplePrompt("Story", "Narrate a story about a writer struggling with writer's block."))
//        prompts.add(SamplePrompt("Story", "Can you tell a story about a family road trip that leads to unexpected adventures?"))
//        prompts.add(SamplePrompt("Story", "Can you tell me a story about a girl who finds a magical book in her grandmother's attic?"))
//        prompts.add(SamplePrompt("Story", "Tell me a story about a young boy who learns the true meaning of friendship."))
//        prompts.add(SamplePrompt("Conversation", "Can you tell me what you can do?"))
//        prompts.add(SamplePrompt("Conversation", "What is the weather tipically like in New York?"))
//        prompts.add(SamplePrompt("Conversation", "I'm planning a 10 day trip to Italy, can you suggest an itinerary"))
//        prompts.add(SamplePrompt("Conversation", "Can you suggest a recipes for pizza without cheese?"))
//        prompts.add(SamplePrompt("Conversation", "Can you suggest good books to read for someone who likes Harry Potter?"))
//        prompts.add(SamplePrompt("Conversation", "Can you suggest good books to read for someone who likes Lord of the Rings?"))
//        prompts.add(SamplePrompt("Conversation", "Can you suggest good books to read to learn more about world history?"))
//        prompts.add(SamplePrompt("Conversation", "Can you suggest good shows to watch for someone who likes The Good Place?"))
//        prompts.add(SamplePrompt("Conversation", "Can you help me find a good movie to watch tonight?"))
//        prompts.add(SamplePrompt("Conversation", "Can you help me find a good gift for my mom's birthday?"))
//        prompts.add(SamplePrompt("Conversation", "What should I do to celebrate my 25th wedding anniversary?"))
//        prompts.add(SamplePrompt("Conversation", "Can you tell me some interesting facts about space?"))
        prompts.add(SamplePrompt("Tutor", "Can you tell me some interesting facts about space?"))
        prompts.add(SamplePrompt("Tutor", "What is the relationship between knowledge and wisdom?"))
        prompts.add(SamplePrompt("Tutor", "How does personal bias influence our perception of truth?"))
        prompts.add(SamplePrompt("Tutor", "Can true altruism exist, or are all actions ultimately self-interested?"))
        prompts.add(SamplePrompt("Tutor", "What is the role of empathy in moral decision-making?"))
        prompts.add(SamplePrompt("Tutor", "Is there an inherent meaning or purpose to life, or is it something we create for ourselves?"))
        prompts.add(SamplePrompt("Tutor", "How does the concept of justice evolve across different cultures and societies?"))
        prompts.add(SamplePrompt("Tutor", "What is the nature of consciousness and how does it relate to the mind-body problem?"))
        prompts.add(SamplePrompt("Tutor", "Can a person truly change, or are they bound by their nature?"))
        prompts.add(SamplePrompt("Tutor", "What is the balance between individual rights and societal responsibilities?"))
        prompts.add(SamplePrompt("Tutor", "How do language and communication shape our understanding of reality?"))
        FirebaseManager.saveSamplePrompts(prompts)
    }
    suspend fun uploadSamplePrompts() {
        sqLiteManager.cleanSamplePrompts()
        val prompts = sqLiteManager.getSamplePrompts(0, false)
        //val prompts2 = ArrayList<SamplePrompt>()
        //for (p in prompts.distinct()) {prompts2.add(SamplePrompt("Conversation", p))}
        FirebaseManager.saveSamplePrompts(prompts)
    }

    fun syncDatabases() {
        val syncConversationsDown = syncConversations
        val syncConversationsUp = syncConversations
        var TOCRefreshNeeded = false
        val timeSinceLastSync = Date().time -lastSynced

        //sqLiteManager.applyFixes(userID, androidID)

        if (FirebaseManager.isFunctional && !syncInProgress ) {
            if (!syncNeeded || timeSinceLastSync <= syncCooldown) {
                Log.d(TAG, "Sync is not needed or on cooldown.  Needed: $syncNeeded  Cooldown: $timeSinceLastSync")
            } else {
                syncInProgress = true   //Prevent concurrent syncs
                var conversationUpdates = 0
                var messageUpdates = 0
                var usageUpdates = 0
                runBlocking {
                    //------------------------------ Usage update -----------------------------------------
                    if (syncUsage) {
                        Log.d(TAG, "Uploading usage to Firebase...")
                        val usages = sqLiteManager.getUsage(userID, androidID)
                        usageUpdates = FirebaseManager.saveUsage(usages)
                        Log.d(TAG, "SyncUp usage updates: " + usageUpdates)
                    }

                    //------------------------------ Sample prompts update -----------------------------------------

                    //generateSamplePrompts()
                    //uploadSamplePrompts()
                    if (sqLiteManager.getSamplePromptsCount(true) < 5 || true) {
                        val t1 = sqLiteManager.getSamplePromptsLastUpdated()
                        val t2 = FirebaseManager.getSamplePromptsLastUpdated()
                        Log.d(
                            TAG,
                            "Checking sample prompts local $t1 (${toDate(t1)}) vs remote $t2 (${
                                toDate(t2)
                            })"
                        )
                        if (t2 > t1) {
                            val prompts = FirebaseManager.getSamplePrompts(t1)
                            Log.d(TAG, "Checking prompts.... found ${prompts.size}")
                            for (p in prompts) {
                                sqLiteManager.appendSamplePrompt(t2, p)
                            }
                            sqLiteManager.cleanSamplePrompts()
                        }
                    }

                    //------------------------------ Conversation updates  -----------------------------------------
                    val d2 = FirebaseManager.getConversationTOCLastUpdated()
                    if (syncConversations && FirebaseManager.isFunctional) {
                        val d1 = sqLiteManager.getConversationsLastUpdated(userID)
                        Log.d( TAG, "Comparing conversation table dates local $d1 (${toDate(d1)}), remote $d2 (${toDate(d2) })" )
                        var fbConversations: ArrayList<Conversation> =  FirebaseManager.getConversationTOC()
                        if (d1 != d2 || encryptContent) {
                            if (encryptionPending) {
                                fbConversations = ArrayList<Conversation>()
                                if (encryptContent) {
                                    Log.d(TAG, "Encryption is pending.  Setting fbConversations to empty list to trigger re-write.")
                                } else {
                                    Log.d(TAG, "Decryption is pending.  Setting fbConversations to empty list to trigger re-write.")
                                }
                            }
                            var sqlConversations: ArrayList<Conversation> = ArrayList(sqLiteManager.getAllConversations(userID))
                            var messagesFB: ArrayList<ChatMessageExtended>
                            var messagesSQL: ArrayList<ChatMessageExtended>
                            var found: Boolean
                            Log.d(TAG, "Firebase conversations: " + fbConversations.size)
                            Log.d(TAG, "SQL conversations: " + sqlConversations.size)

                            //------------------------------ Part I deletions  -----------------------------------------
                            if (FirebaseManager.isFunctional) {
                                val fbDeletedConversations = FirebaseManager.getDeletedConversations()
                                val sqlDeletedConversations = sqLiteManager.getDeletedConversations(userID)
                                Log.d( TAG,"Begin clearing deleted conversations from Firebase...${sqlDeletedConversations.size}" )
                                conversationUpdates = 0
                                var iterator = fbConversations.iterator()
                                while (iterator.hasNext()) {
                                    val c = iterator.next()
                                    if (c.conversationID in sqlDeletedConversations) {
                                        Log.d(TAG, "Deleting ${c.conversationID} from Firebase")
                                        FirebaseManager.deleteConversation(c.conversationID)
                                        iterator.remove()
                                        conversationUpdates += 1
                                    }
                                }
                                if (conversationUpdates > 0) {
                                    FirebaseManager.saveDeletedConversations(sqlDeletedConversations)
                                    Log.d(TAG,"Deleted $conversationUpdates FireBase conversations")
                                }
                                Log.d(TAG, "Begin clearing deleted conversations from SQL...")
                                conversationUpdates = 0
                                iterator = sqlConversations.iterator()
                                while (iterator.hasNext()) {
                                    val c = iterator.next()
                                    if (c.conversationID in fbDeletedConversations) {
                                        Log.d(TAG, "Deleting ${c.conversationID} from SQL")
                                        sqLiteManager.deleteConversation(c.conversationID)
                                        iterator.remove()
                                        conversationUpdates += 1
                                    }
                                }
                                if (conversationUpdates > 0) {
                                    Log.d(TAG, "Deleted $conversationUpdates SQL conversations")
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
                                                    Log.w(TAG,"Zero messages returned from getMessagesFirebase" ) //retrieval may have just failed so don't do anything
                                                } else {
                                                    messagesSQL = ArrayList(sqLiteManager.getMessages(c.conversationID))
                                                    messagesSQL.sortBy { it.timeStamp }
                                                    messagesFB.sortBy { it.timeStamp }
                                                    val sqlSet =  messagesSQL.map { it.timeStamp }.toSet()
                                                    val fbSet = messagesFB.map { it.timeStamp }.toSet()
                                                    if (c.dateModified > cc.dateModified) {
                                                        Log.d(TAG,"Conversation found but SQL is newer, update Firebase version..."  )
                                                        FirebaseManager.saveConversation(c, messagesSQL )
                                                        conversationUpdates += 1
                                                    } else {
                                                        Log.d(TAG,"Conversation found but Firebase is newer, update SQL version..." )
                                                        val iterator = messagesSQL.iterator()
                                                        while (iterator.hasNext()) {
                                                            val message = iterator.next()
                                                            if (message.timeStamp !in fbSet) {
                                                                sqLiteManager.deleteMessage(message)
                                                                iterator.remove()
                                                                messageUpdates += 1
                                                            }
                                                        }
                                                        for (message in messagesFB) {
                                                            if (message.timeStamp !in sqlSet) {
                                                                sqLiteManager.appendMessage( fbConv, message )
                                                                messagesSQL.add(message)
                                                                messageUpdates += 1
                                                            }
                                                        }
                                                        sqLiteManager.updateConversationDates(fbConv)
                                                        conversationUpdates += 1
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if (!found) {
                                        conversationUpdates += 1
                                        messagesSQL =  sqLiteManager.getMessages(c.conversationID) as ArrayList<ChatMessageExtended>
                                        if (messagesSQL.size == 0) {
                                            Log.d(TAG, "Deleting empty conversation from SQL...")
                                            sqLiteManager.deleteConversation(c.conversationID)
                                        } else {
                                            Log.d(TAG,"Appending missing conversation ${c.conversationID} to Firebase...")
                                            c.saved = true
                                            c.userID = userID
                                            FirebaseManager.saveConversation(c, messagesSQL)
                                        }
                                    }
                                }
                                Log.d(TAG,"SyncUp updates: " + conversationUpdates + ": " + messageUpdates)
                                if (conversationUpdates > 0) {TOCRefreshNeeded = true}
                            }
                            //------------------------------ Part III Sync changes down from Firebase  -----------------------------------------
                            if (syncConversationsDown && FirebaseManager.isFunctional) {
                                Log.d(TAG, "Begin database sync down from Firebase...")
                                conversationUpdates = 0
                                messageUpdates = 0
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
                                            Log.d(TAG, "Appending missing conversation ${c.conversationID} to SQL..." )
                                            fbConv.saved = true
                                            fbConv.userID = userID
                                            sqlConversations.add(fbConv)
                                            sqLiteManager.saveConversation(fbConv)
                                            for (m in messagesFB) {sqLiteManager.appendMessage(fbConv, m) }
                                        }
                                    }
                                }
                                Log.d(TAG,"SyncDown updates: $conversationUpdates: $messageUpdates")
                                if (conversationUpdates > 0) {TOCRefreshNeeded = true}
                            }
                            if (TOCRefreshNeeded && FirebaseManager.isFunctional) {
                                sqlConversations = ArrayList(sqLiteManager.getAllConversations(userID))
                                Log.d(TAG, "Updating Firebase TOC")
                                FirebaseManager.makeConversationTOC(sqlConversations)
                            }
                        }
                    }
                    Log.d(TAG, "Database sync completed")
                }
                lastSynced = Date().time
                FirebaseManager.updateLastSynced()
                syncNeeded = false
                syncInProgress = false
            }
        }
    }
}