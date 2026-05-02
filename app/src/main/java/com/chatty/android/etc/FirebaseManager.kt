package com.chatty.android.etc

import android.os.Build
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.chatty.android.etc.DataClasses.*

object FirebaseManager {
    private const val TAG = "FireBaseManager"
    private const val defaultDate = 946684800000    //1/1/2000
    private const val encryptionTestContent = "Today is a good day!"
    private lateinit var fsDatabase: FirebaseFirestore
    var isFunctional: Boolean = true //Should stay true unless something breaks
    var isInitialized: Boolean = false
    private var deviceID: String = ""
    private var androidID: String= ""
    private var userID: String= ""
    private var rootID: String = ""
    private var usageRootID: String = ""
    private var deviceRootID: String = ""
    private var keyTransferRequestedCertificate: String = ""
    private var keyTransferResponse = ""
    private var NotesCategoryCache = ArrayList<NoteCategory>() //Cache on TOC read, sync from there during sync

    suspend fun initialize(givenAndroidID:String,givenDeviceID:String,useGoogleAuth:Boolean){
        val auth=FirebaseAuth.getInstance()
        androidID=givenAndroidID
        deviceID=givenDeviceID
        try{
            var user=auth.currentUser
            if(user==null){
                Log.d(TAG,"Firebase: No cached user, signing in anonymously...")
                val result=auth.signInAnonymously().await()
                user=result.user
            }else{
                Log.d(TAG,"Firebase: Existing user detected: ${user.uid} anonymous: ${user.isAnonymous}")
            }
            if(user==null){
                Log.e(TAG,"Firebase: Authentication failed, user is null")
                isFunctional=false
                return
            }
            userID=user.uid
            StorageManager.userID=userID
            Log.d(TAG,"Firebase initialized with userID: $userID")
            rootID=userID
            usageRootID="$androidID:$userID"
            deviceRootID="$androidID:$deviceID"
            fsDatabase=FirebaseFirestore.getInstance()
            isFunctional=true
            isInitialized=true
        }catch(ex:Exception){
            Log.e(TAG,"Firebase initialization failed",ex)
            isFunctional=false
            isInitialized=false
        }
    }
    fun onGoogleAuthComplete(googleUserID: String) {
        userID = googleUserID
        StorageManager.userID = userID
        rootID = userID
        usageRootID = "$androidID:$userID"
        deviceRootID = "$androidID:$deviceID"
        if (!::fsDatabase.isInitialized) {
            fsDatabase = FirebaseFirestore.getInstance()
        }
        isFunctional = true
        Log.d(TAG, "Firebase updated with Google user: $userID")
    }

    fun shutDown() {
    }

    //-------------------------------------------------- Firebase essential functions  --------------------------------------------------
    private fun generateID(timeInMillis:Long = 0, dayPrecision:Boolean = false, baseID: String = rootID): String {
        val dateStamp = Calendar.getInstance()
        if (timeInMillis > 0) {dateStamp.timeInMillis = timeInMillis}
        if (dayPrecision) {
            dateStamp.set(Calendar.HOUR_OF_DAY, 0)
            dateStamp.set(Calendar.MINUTE, 0)
            dateStamp.set(Calendar.SECOND, 0)
            dateStamp.set(Calendar.MILLISECOND, 0)
        }
        var ts =  dateStamp.timeInMillis.toString()
        while (ts.length < 13) {ts = "0$ts"
        }
        //Log.d(TAG, timeInMillis.toString() + " -> " + ts + " -> " + toDate(ts.toLong()))
        return "$baseID:$ts"
    }

     private suspend fun getDocumentById(collectionName: String, documentId: String): DocumentSnapshot?
     //withContext(Dispatchers.IO)
    {
        val documentRef = fsDatabase.collection(collectionName).document(documentId)
        try {
            return documentRef.get().await()
        } catch (e: Exception) {
            return null
        }
    }
    private fun saveDocument(collectionName: String, documentID: String, data: MutableMap<String, Any>) {
        Log.d(TAG, "Saving document to $collectionName documentID $documentID")
        fsDatabase.collection(collectionName).document(documentID).set(data)
            .addOnSuccessListener {
                Log.d(TAG, "Saved to $collectionName documentID $documentID")
            }
            .addOnFailureListener { e ->
                Log.w(TAG,"Error saving to $collectionName documentID $documentID", e )
            }
    }
    private fun getDocumentItems(d: DocumentSnapshot, itemPrefix: String="item"):ArrayList<String> {
        val result = ArrayList<String>()
        val itemCount = d.getLong("${itemPrefix}Count") ?: 0
        for (i in 0 until itemCount ) {
            val x = d.getString("${itemPrefix}${i}") ?: ""
            if (x != "") { result.add(x) }
        }
        return result
    }
    private fun getDocumentItemsLong(d: DocumentSnapshot, itemPrefix: String="item"):ArrayList<Long> {
        val result = ArrayList<Long>()
        val itemCount = d.getLong("${itemPrefix}Count") ?: 0
        for (i in 0 until itemCount ) {
            val x = d.getLong("${itemPrefix}${i}") ?: 0
            if (x > -1) { result.add(x) }
        }
        return result
    }

    //-------------------------------------------------- device setup functions  --------------------------------------------------
    private fun updateDeviceStatus() {
        //Log.d(TAG, "Updating device registration: $deviceRootID")
        val now = Date()
        val readableTimestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.getDefault()).format(now)
        val publicKeyString = CryptoManager.getPublicKeyString()
        val data = hashMapOf(
            "deviceManufacturer" to Build.MANUFACTURER,
            "deviceModel" to "${Build.MANUFACTURER} ${Build.MODEL}",
            "androidVersion" to "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            "appVersion" to com.chatty.android.BuildConfig.VERSION_NAME,
            "userID" to userID,
            "publicKey" to publicKeyString,
            "timeStamp" to now.time,
            "lastCheckinReadable" to readableTimestamp
        )
        fsDatabase.collection(RegistrationTableName).document(deviceRootID).set(data)
            .addOnSuccessListener {
                Log.d(TAG, "Updated device registration: $deviceRootID")
            }
            .addOnFailureListener { e ->
                Log.w(TAG,"Update device registration error: $deviceRootID", e )
            }
    }
    fun updateLastSynced() {
        val readableLastSynced = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.getDefault()).format(Date(StorageManager.lastSynced))
        fsDatabase.collection(RegistrationTableName).document(deviceRootID).update(
            "lastSynced", StorageManager.lastSynced,
            "lastSyncedReadable", readableLastSynced
        )
            .addOnSuccessListener {
                Log.d(TAG, "updateLastSynced successfull")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "updateLastSynced error: ", e)
            }

    }
    private fun activateTrialLicense() {
        Log.d(TAG, "Activating trial license for device: $deviceRootID")
        val data = hashMapOf("subscriptionLevel" to 1, "useGoogleAuth" to false,  "syncConversations" to false, "syncUsage" to true, "timeStamp" to Date().time  )
        fsDatabase.collection(ActivatedDevicesTableName).document(deviceRootID).set(data)
            .addOnSuccessListener {
                Log.d(TAG, "Device activated for trial")
            }
            .addOnFailureListener { e ->
                Log.w(TAG,"Error activating device", e )
            }
    }
//data class DeviceSettings(val deviceModel: String = "", var subscriptionLevel:Int = -100, val useGoogleAuth:Boolean=false, val syncConversations: Boolean=false, val syncUsage: Boolean = true, var apiKey: String = "")
    suspend fun getDeviceSettings() {
        Log.d(TAG, "Reading device settings for $deviceRootID")
        val documentRef = fsDatabase.collection(ActivatedDevicesTableName).document(deviceRootID)
        try {
            val document = documentRef.get().await()
            val useGoogleAuth = document.getBoolean("useGoogleAuth") ?:false
            val x = document.getLong("subscriptionLevel") ?:-100
            var subscriptionLevel  = x.toInt()
            val syncConversations = document.getBoolean("syncConversations") ?:false
            val syncUsage = document.getBoolean("syncUsage") ?:true
            var apiKey = document.getString("apiKey") ?:""
            var webClientID = document.getString("webClientID") ?:""
            var defaultModel = document.getString("defaultModel") ?:""
            var enhancedModel = document.getString("enhancedModel") ?:""
            if (defaultModel !="") {
                ChatManager.defaultModel = defaultModel
            }
            if (enhancedModel !="") {
                ChatManager.enhancedModel = enhancedModel
            }
            if (apiKey !="") {apiKey = CryptoManager.decryptStringRSA(apiKey)}
            if (webClientID !="") {webClientID = CryptoManager.decryptStringRSA(webClientID)}
            StorageManager.API_KEY = apiKey
            if (apiKey !="") {Log.d(TAG, "Read API key from encrypted device settings")}
            StorageManager.webClientID = webClientID
            if (subscriptionLevel.toInt() ==-100) {
                activateTrialLicense()
                subscriptionLevel = 1
            }
            StorageManager.saveDeviceSettings(subscriptionLevel, useGoogleAuth, syncConversations, syncUsage, webClientID)
        } catch (exception:Exception) {
            Log.e(TAG, "Unable to read device settings for $deviceRootID")
            StorageManager.saveDeviceSettings(1, false, false, false, "")
            activateTrialLicense()
        }
        updateDeviceStatus()
    }

    //-------------------------------------------------- Firebase Sample Prompt Management  --------------------------------------------------
    suspend fun getSamplePromptsLastUpdated():Long
    {
        var maxTimeStamp:Long = defaultDate
        try {
            val snap =  fsDatabase.collection(SamplePromptTableName)
                .orderBy("timeStamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
            if (snap != null) {
                if (!snap.isEmpty) {
                    if (snap.documents.isNotEmpty()) {
                        maxTimeStamp = snap.documents[0].getLong("timeStamp") ?: 0
                    }
                }
            }
            Log.d(TAG, "getSamplePromptsLastUpdated result $maxTimeStamp (${StorageManager.toDateStr(maxTimeStamp)})")
        } catch (exception:Exception) {
            Log.e(TAG, "Unable to read $SamplePromptTableName collection", exception)
        }
        return maxTimeStamp
    }

    suspend fun getSamplePrompts(cutoff: Long): ArrayList<SamplePrompt> {
        val result = ArrayList<SamplePrompt>()
        try {
            val snap = fsDatabase.collection(SamplePromptTableName)
                .orderBy("timeStamp")
                .whereGreaterThan("timeStamp", cutoff)
                .get()
                .await()
            if (snap != null) {
                if (!snap.isEmpty) {
                    for (document in snap.documents) {
                        val timeStamp = document.getLong("timeStamp") ?: 0
                        val items = getDocumentItems(document)
                        for (item in items) {
                            val v = item.split("|")
                            val prompt = v[0]
                            var activityType = "Conversation"
                            if (v.size > 1){ activityType = v[1]}
                            result.add(SamplePrompt(activityType, prompt, timeStamp))
                        }
                    }
                }
            }
        } catch (exception:Exception) {
            Log.e(TAG, "Unable to read $SamplePromptTableName collection", exception)
        }
        return result
    }

    suspend fun saveSamplePrompts(prompts: ArrayList<SamplePrompt>):Int{
        val baseID = "System"
        var updateCount = 0
        //prompts.sortByDescending  { it.timeStamp  }
        val timeStamp = Date().time
        if (prompts.size > 0) {
            val lastUpdated = getSamplePromptsLastUpdated()
            var itemCount = 0
            val stopID = generateID(lastUpdated, true, baseID)
            Log.d(TAG, "Prompt sync $stopID ${prompts.size} stop at ${StorageManager.toDateStr(lastUpdated)}")
            var currentID = generateID(timeStamp, true, baseID)
            var data = HashMap<String, Any>()
            val iterator = prompts.iterator()
            while (iterator.hasNext() && currentID >= stopID) {
                val id = generateID(timeStamp, true, baseID)
                val value = iterator.next()
                updateCount += 1
                if (id != currentID) {
                    if (itemCount > 0) {
                        data["timeStamp"] = timeStamp
                        data["itemCount"] = itemCount
                        saveDocument(SamplePromptTableName, currentID, data)
                        data = HashMap()
                        itemCount = 0
                    }
                    currentID = id
                } else {
                    data["item$itemCount"] = value.prompt + "|" + value.activityName
                    itemCount+=1
                }
            }
            if (itemCount > 0) {
                data["timeStamp"] = timeStamp
                data["itemCount"] = itemCount
                saveDocument(SamplePromptTableName, currentID, data)
            }
        }
        return updateCount
    }

    //-------------------------------------------------- Firebase Chat Activity Type Management  --------------------------------------------------
    suspend fun getChatModesLastUpdated():Long
    {
        var maxTimeStamp:Long = defaultDate
        try {
            val snap =  fsDatabase.collection(ChatModesTableName)
                .orderBy("timeStamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
            if (snap != null) {
                if (!snap.isEmpty) {
                    if (snap.documents.isNotEmpty()) {
                        maxTimeStamp = snap.documents[0].getLong("timeStamp") ?: 0
                    }
                }
            }
            Log.d(TAG, "getActivityTypesLastUpdated result $maxTimeStamp (${StorageManager.toDateStr(maxTimeStamp)})")
        } catch (exception:Exception) {
            Log.e(TAG, "Unable to read $ChatModesTableName collection", exception)
        }
        return maxTimeStamp
    }

    suspend fun getChatModes(): ArrayList<ChatActivityType> {
        val result = ArrayList<ChatActivityType>()
        try {
            val snap = fsDatabase.collection(ChatModesTableName)
                .orderBy("timeStamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
            if (snap != null) {
                if (!snap.isEmpty) {
                    for (document in snap.documents) {
                        val timeStamp = document.getLong("timeStamp") ?: 0
                        val items = getDocumentItems(document)
                        for (item in items) {
                            val v = item.split("|")
                            // value.activityName + "|" + value.prompt + "|" + value.conversational + "|" + value.clearConversationOnChange + "|" + value.showLanguageOptions + "|" + value.temperature
                            val a = ChatActivityType(v[0], v[1], v[2].toBooleanStrict(), v[3].toBooleanStrict(), v[4].toBooleanStrict(), v[5].toDouble())
                            result.add(a)
                        }
                    }
                }
            }
        } catch (exception:Exception) {
            Log.e(TAG, "Unable to read $ChatModesTableName collection", exception)
        }
        return result
    }

    suspend fun saveChatModes(activities: ArrayList<ChatActivityType>){
        Log.d(TAG, "Saving chat modes to firebase")
        val baseID = "System"
        val timeStamp = Date().time
        if (activities.size > 0) {
            var itemCount = 0
            var id = generateID(timeStamp, true, baseID)
            var data = HashMap<String, Any>()
            val iterator = activities.iterator()
            while (iterator.hasNext()) {
                val value = iterator.next()
                data["item$itemCount"] = value.activityName + "|" + value.prompt + "|" + value.conversational + "|" + value.clearConversationOnChange + "|" + value.showLanguageOptions + "|" + value.temperature
                itemCount+=1
            }
            data["timeStamp"] = timeStamp
            data["itemCount"] = itemCount
            saveDocument(ChatModesTableName, id, data)
        }
    }

    //-------------------------------------------------- Firebase Conversation Management  --------------------------------------------------
    suspend fun getDeletedConversations(): ArrayList<Long> {
        val result = ArrayList<Long>()
        try {
            val document = fsDatabase.collection(ConversationDeletionsTableName).document(userID)
                .get()
                .await()
            if (document != null) {
                val items = getDocumentItemsLong(document)
                for (item in items) {result.add(item.toLong())}
            }
        } catch (exception:Exception) {
            Log.e(TAG, "Unable to read $ConversationDeletionsTableName collection, disabling Firebase.  Reason: " + exception.toString())
            isFunctional = false
        }
        return result
    }
    fun saveDeletedConversations(ids: ArrayList<Long>){
        val timeStamp = Date().time
        val data = HashMap<String, Any>()
        data["timeStamp"] = timeStamp
        data["itemCount"] = ids.size
        var i = 0
        for (id in ids){
            data["item$i"] = id
            i += 1
        }
        saveDocument(ConversationDeletionsTableName, userID, data)
    }

     fun saveConversation(conversation: Conversation, messages: ArrayList<ChatMessageExtended>) {
        val documentID = generateID(conversation.conversationID, false)
        val data =HashMap<String, Any>()
        data["conversationID"] = conversation.conversationID
        data["dateAccessed"] = conversation.dateAccessed
        data["dateCreated"] = conversation.dateCreated
        data["dateModified"] = conversation.dateModified
        if (StorageManager.encryptContent) {
            data["title"] =  CryptoManager.encryptStringAES(conversation.title, StorageManager.contentEncryptionKey)
            data["summary"] = CryptoManager.encryptStringAES(conversation.summary, StorageManager.contentEncryptionKey)
        } else {
           data["title"] =  conversation.title
           data["summary"] =  conversation.summary
        }
        var itemCount = 0
        messages.sortBy{it.timeStamp}
        for (m in messages) {
            m.content.replace("|", "")
            var v = m.timeStamp.toString() + "|" + m.role + "|" + m.content
            if (StorageManager.encryptContent) {v = CryptoManager.encryptStringAES(v, StorageManager.contentEncryptionKey)}
            data["item$itemCount"] = v
            itemCount += 1
        }
        data["itemCount"] = itemCount
        saveDocument(ConversationTableName, documentID, data)
        conversation.saved = true
    }

    suspend fun getConversation(conversationID: Long): Pair<Conversation, ArrayList<ChatMessageExtended>>  {
        val conversation = Conversation()
        val messages = ArrayList<ChatMessageExtended> ()
        val documentID = generateID(conversationID, false)
        try {
            val document = fsDatabase.collection(ConversationTableName).document(documentID)
                .get()
                .await()
            if (document != null) {
                conversation.conversationID = document.getLong("conversationID") ?:conversationID
                conversation.dateAccessed = document.getTimestamp("dateAccessed")?.toDate() ?: Date(defaultDate)
                conversation.dateCreated = document.getTimestamp("dateCreated")?.toDate() ?: Date(defaultDate)
                conversation.dateModified = document.getTimestamp("dateModified")?.toDate() ?: Date(defaultDate)
                conversation.title = document.getString("title") ?:""
                conversation.summary = document.getString("summary") ?:""
                if (StorageManager.encryptContent) {
                    conversation.title = CryptoManager.decryptStringAES(conversation.title, StorageManager.contentEncryptionKey)
                    conversation.summary = CryptoManager.decryptStringAES(conversation.summary, StorageManager.contentEncryptionKey)
                }
                val items = getDocumentItems(document)
                for (item in items) {
                    var v = item.split("|")
                    if (StorageManager.encryptContent) { v = CryptoManager.decryptStringAES(item, StorageManager.contentEncryptionKey).split("|") }
                    if (v.size==3) {
                        val m = ChatMessageExtended(conversationID, v[1], v[2], v[0].toLong())
                        messages.add(m)
                    } else {
                        Log.e(TAG, "Malformed message in $conversationID segments: ${v.size} content: ${item.take(50)}")
                    }
                }
            }
        } catch (exception:Exception) {
            Log.e(TAG, "Unable to read conversation conversationID: $conversationID, disabling Firebase", exception)
            isFunctional = false
        }
        return Pair(conversation, messages)
    }

    fun makeConversationTOC(conversations: ArrayList<Conversation>)  {
        //TOC Entry is conversationID:dateModified:title.take25.encrypted
        var lastUpdated = defaultDate
        for (c in conversations) {
            if (c.dateModified.time > lastUpdated) { lastUpdated = c.dateModified.time}
        }
        val documentID = "$rootID:TOC"
        val data =HashMap<String, Any>()
        data["dateCreated"] = lastUpdated
        data["title"] =  "TOC"
        data["rootID"] =  rootID
        if (StorageManager.encryptContent){
            data["key"] = CryptoManager.encryptStringAES(encryptionTestContent, StorageManager.contentEncryptionKey)
            if (keyTransferRequestedCertificate!="") {data.set("transferRequest", keyTransferRequestedCertificate)}
            if (keyTransferResponse !="") {data.set("transferResponse", keyTransferResponse)}

        }
        conversations.sortBy{it.conversationID}
        var itemCount = 0
        for (c in conversations) {
            //This is going to be incomplete, but it is more efficient storage on the TOC, updates are done off the full item download not TOC
            var title = c.title.take(25).replace("|", "")
            if (StorageManager.encryptContent)  { title = CryptoManager.encryptStringAES(title, StorageManager.contentEncryptionKey) }
            data["item$itemCount"] = c.conversationID.toString() +  "|" + c.dateModified.time.toString() + "|" + title
            itemCount += 1
        }
        data["itemCount"] = itemCount
        Log.d(TAG, "Creating new Conversation TOC, entries: ${itemCount}, latestEntry: ${lastUpdated} TOCID: ${documentID}")
        saveDocument(ConversationTableName, documentID, data)
    }

    suspend fun getConversationTOCLastUpdated():Long
    {
        val documentID = "$rootID:TOC"
        var maxTimeStamp:Long = 0
        try {
            val document = fsDatabase.collection(ConversationTableName).document(documentID)
                .get()
                .await()
            if (document != null) {
                maxTimeStamp = document.getLong("dateCreated")?: 0
            }
        } catch (exception:Exception) {
            Log.e(TAG, "Unable to read latest dateCreated from $ConversationTableName TOC", exception)
        }
        return maxTimeStamp
    }

    suspend fun getConversationTOC(): ArrayList<Conversation> {
        Log.d(TAG, "getConversationTOC")
        val documentID = "$rootID:TOC"
        val conversations = ArrayList<Conversation>()
        var document: DocumentSnapshot? = null
        try {
            document = fsDatabase.collection(ConversationTableName).document(documentID)
                .get()
                .await()
        } catch (exception:Exception) {
            Log.e(TAG, "Unable to read TOC", exception)
            //If the TOC fails to read, this will cause an upload of the full conversation list and re-creation of the TOC, due to the structure of the IDs items won't be duplicated
        }
        if (document != null) {
            var encryptionStateIsGood = !StorageManager.encryptContent
            val encryptionTestData = document.getString("key") ?: ""
            keyTransferRequestedCertificate = document.getString("transferRequest") ?: ""
            if (StorageManager.encryptContent) {
                Log.d(TAG, "Encryption is enabled.  Checking status...")
                Log.d(TAG, "Key: " + StorageManager.contentEncryptionKey)
                if (encryptionTestData == "") {
                    Log.d(TAG, "Content not yet encrypted, setting encryptionPending.")
                    StorageManager.encryptionPending = true
                    if (StorageManager.contentEncryptionKey == "") {
                        Log.d(TAG, "Generating new AES encryption key...")
                        StorageManager.saveContentEncryptionKey(CryptoManager.exportNewAESKey())
                    }
                } else {
                    if (StorageManager.contentEncryptionKey != "") {
                        Log.d(TAG, "Content is encrypted.  Testing if my key matches.")
                        val test = CryptoManager.decryptStringAES(encryptionTestData, StorageManager.contentEncryptionKey)
                        if (test == encryptionTestContent) {
                            Log.d(TAG, "AES Encryption state is good.  Clear to proceed.")
                            encryptionStateIsGood = true
                        } else {
                            Log.e(TAG, "AES Encryption test failed with my key.")
                        }
                    }
                    if (!encryptionStateIsGood) {
                        keyTransferResponse = document.getString("transferResponse") ?: ""
                        if (keyTransferResponse != "" && keyTransferRequestedCertificate == CryptoManager.getPublicKeyString()) {
                            Log.d(TAG, "Content is encrypted, checking the status of my key request.")
                            val testKey = CryptoManager.decryptStringRSA(keyTransferResponse)
                            val test = CryptoManager.decryptStringAES(encryptionTestData, testKey)
                            if (test == encryptionTestContent) {
                                Log.d(TAG, "Encryption state is good.  Clear to proceed.")
                                StorageManager.saveContentEncryptionKey(testKey)
                                encryptionStateIsGood = true
                                clearKeyTransferFieldsFromTOC(documentID)
                            } else {
                                Log.e(TAG, "Encryption test failed on received key.")
                            }
                        } else {
                            Log.d(TAG, "Content is encrypted and I don't have a valid key.  Requesting key transfer.")
                            val documentData = document.data
                            if (documentData != null) {
                                documentData.set("transferRequest", CryptoManager.getPublicKeyString())
                                saveDocument(ConversationTableName, documentID, documentData)
                                isFunctional = false   //If the encryption key doesn't match then we aren't functional
                                Log.e(TAG, "getConversationTOC, requested key transfer.")
                            }
                        }
                    } else {
                        Log.d(TAG, "My encryption state is good.  Checking for transfer requests...")
                        if (keyTransferRequestedCertificate != "" && keyTransferRequestedCertificate != CryptoManager.getPublicKeyString()) {
                            Log.d(TAG, "Key transfer requested, storing for user approval")
                            val deviceModel = getRequestingDeviceModel(keyTransferRequestedCertificate)
                            StorageManager.savePendingKeyTransferRequest(keyTransferRequestedCertificate, deviceModel)
                        }
                    }
                }
            }
            else {
                if (encryptionTestData !="") {  //Need to decrypt Firebase conent
                    encryptionStateIsGood = false
                    StorageManager.encryptionPending = true
                }
            }
            if (encryptionStateIsGood){
                val items = getDocumentItems(document)
                for (item in items) {
                    val v = item.split("|")
                    if (v.size==3 || (v.size==4 && StorageManager.encryptContent)) {
                        val conversationID = v[0].toLong()
                        val dateModified = v[1].toLong()
                        var titlePartial = v[2]
                        if (StorageManager.encryptContent) {
                            titlePartial = v[2] + "|" + v[3]
                            titlePartial = CryptoManager.decryptStringAES(titlePartial, StorageManager.contentEncryptionKey)
                        }
                        val c = Conversation(conversationID, titlePartial ,"",true, userID, Date(dateModified), Date(dateModified), Date(dateModified))
                        conversations.add(c)
                    } else {
                        Log.e(TAG, "Malformed conversation from TOC size: " + v.size + " content " + item)
                    }
                }
            }
        }
        return conversations
    }

    private suspend fun getRequestingDeviceModel(publicKey: String): String {
        return try {
            val snap = fsDatabase.collection(RegistrationTableName)
                .whereEqualTo("publicKey", publicKey)
                .limit(1)
                .get()
                .await()
            if (!snap.isEmpty) snap.documents[0].getString("deviceModel") ?: "Unknown device"
            else "Unknown device"
        } catch (e: Exception) {
            Log.e(TAG, "getRequestingDeviceModel failed", e)
            "Unknown device"
        }
    }

    suspend fun sendKeyTransferResponse() {
        val requesterPublicKey = StorageManager.pendingKeyTransferPublicKey
        if (requesterPublicKey.isEmpty() || StorageManager.contentEncryptionKey.isEmpty()) {
            Log.e(TAG, "sendKeyTransferResponse: missing key data")
            StorageManager.clearPendingKeyTransferRequest()
            return
        }
        val documentID = "$rootID:TOC"
        try {
            val document = fsDatabase.collection(ConversationTableName).document(documentID).get().await()
            val documentData = document.data
            if (documentData != null) {
                documentData["transferResponse"] = CryptoManager.encryptStringRSA(StorageManager.contentEncryptionKey, requesterPublicKey)
                saveDocument(ConversationTableName, documentID, documentData)
                Log.d(TAG, "sendKeyTransferResponse: sent encrypted key to requester")
            }
        } catch (e: Exception) {
            Log.e(TAG, "sendKeyTransferResponse failed", e)
        }
        StorageManager.markKeyTransferResponded()
        StorageManager.clearPendingKeyTransferRequest()
    }

    suspend fun clearKeyTransferRequest() {
        val documentID = "$rootID:TOC"
        try {
            fsDatabase.collection(ConversationTableName).document(documentID)
                .update("transferRequest", FieldValue.delete(), "transferResponse", FieldValue.delete())
                .await()
            Log.d(TAG, "clearKeyTransferRequest: cleared transfer fields")
        } catch (e: Exception) {
            Log.e(TAG, "clearKeyTransferRequest failed", e)
        }
        StorageManager.clearPendingKeyTransferRequest()
    }

    private fun clearKeyTransferFieldsFromTOC(documentID: String) {
        fsDatabase.collection(ConversationTableName).document(documentID)
            .update("transferRequest", FieldValue.delete(), "transferResponse", FieldValue.delete())
            .addOnSuccessListener { Log.d(TAG, "Cleared key transfer fields from TOC") }
            .addOnFailureListener { e -> Log.e(TAG, "Failed to clear key transfer fields", e) }
    }

    fun deleteConversation(conversationID: Long) {
        val documentID = generateID(conversationID, false)
        val docRef = fsDatabase.collection(ConversationTableName).document(documentID)
        docRef
            .delete()
            .addOnSuccessListener {
                println("Conversation $conversationID successfully deleted!")
            }
            .addOnFailureListener { e ->
                println("Error deleting conversation $conversationID: $e")
            }
    }
    //-------------------------------------------------- Firebase Notes Management  --------------------------------------------------
    fun saveNote(note: NoteEntry) {
        val documentID = generateID(note.noteID, false)
        val data =HashMap<String, Any>()
        data["noteID"] = note.noteID
        data["categoryID"] = note.categoryID
        data["dateAccessed"] = note.dateAccessed
        data["dateCreated"] = note.dateCreated
        data["dateModified"] = note.dateModified
        if (StorageManager.encryptContent) {
            data["title"] =  CryptoManager.encryptStringAES(note.title, StorageManager.contentEncryptionKey)
            data["content"] = CryptoManager.encryptStringAES(note.content, StorageManager.contentEncryptionKey)
        } else {
            data["title"] =  note.title
            data["content"] =  note.content
        }
        saveDocument(NotesTableName, documentID, data)
        //note.saved = true
    }

    suspend fun getNote(noteID: Long): NoteEntry  {
        val note = NoteEntry()
        val documentID = generateID(noteID, false)
        try {
            val document = fsDatabase.collection(NotesTableName).document(documentID)
                .get()
                .await()
            if (document != null) {
                note.noteID = document.getLong("noteID") ?:noteID
                note.categoryID = (document.getLong("categoryID") ?:0).toInt()
                note.dateAccessed = document.getTimestamp("dateAccessed")?.toDate() ?: Date(defaultDate)
                note.dateCreated = document.getTimestamp("dateCreated")?.toDate() ?: Date(defaultDate)
                note.dateModified = document.getTimestamp("dateModified")?.toDate() ?: Date(defaultDate)
                note.title = document.getString("title") ?:""
                note.content = document.getString("content") ?:""
                if (StorageManager.encryptContent) {
                    note.title = CryptoManager.decryptStringAES(note.title, StorageManager.contentEncryptionKey)
                    note.content = CryptoManager.decryptStringAES(note.content, StorageManager.contentEncryptionKey)
                }
            }
        } catch (exception:Exception) {
            Log.e(TAG, "Unable to read note noteID: $noteID, disabling Firebase", exception)
            isFunctional = false
        }
        return note
    }

    fun makeNotesTOC(notes: ArrayList<NoteEntry>)  {
        //TOC Entry is notesID:dateModified:title.take25.encrypted
        var lastUpdated = defaultDate
        for (c in notes) {
            if (c.dateModified.time > lastUpdated) { lastUpdated = c.dateModified.time}
        }
        val documentID = "$rootID:TOC"
        val data =HashMap<String, Any>()
        data["dateCreated"] = lastUpdated
        data["title"] =  "TOC"
        data["rootID"] = rootID
        notes.sortBy{it.noteID}
        var itemCount = 0
        for (c in notes) {
            //This is going to be incomplete, but it is more efficient storage on the TOC, updates are done off the full item download not TOC
            var title = c.title.take(25).replace("|", "")
            if (StorageManager.encryptContent)  { title = CryptoManager.encryptStringAES(title, StorageManager.contentEncryptionKey) }
            data["item$itemCount"] = c.noteID.toString() +  "|" + c.dateModified.time.toString() + "|" + title
            itemCount += 1
        }
        data["itemCount"] = itemCount
        Log.d(TAG, "Creating new Notes TOC, entries: ${itemCount}, latestEntry: ${lastUpdated} TOCID: ${documentID}")
        //Note categories, put on the TOC in plain text
        itemCount = 0
        for (c in NotesCategoryCache) {
            data["category${itemCount}"] = c.categoryID.toString() +  "|" + c.categoryName
            itemCount += 1
        }
        data["categoryCount"] = itemCount
        saveDocument(NotesTableName, documentID, data)
    }

    suspend fun getNotesTOCLastUpdated():Long
    {
        val documentID = "$rootID:TOC"
        var maxTimeStamp:Long = 0
        try {
            val document = fsDatabase.collection(NotesTableName).document(documentID)
                .get()
                .await()
            if (document != null) {
                maxTimeStamp = document.getLong("dateCreated")?: 0
            }
        } catch (exception:Exception) {
            Log.e(TAG, "Unable to read latest dateCreated from $NotesTableName TOC", exception)
        }
        return maxTimeStamp
    }

    suspend fun getNotesTOC(): ArrayList<NoteEntry> {
        Log.d(TAG, "getNoteEntryTOC")
        val documentID = "$rootID:TOC"
        val notes = ArrayList<NoteEntry>()
        var document: DocumentSnapshot? = null
        try {
            document = fsDatabase.collection(NotesTableName).document(documentID)
                .get()
                .await()
        } catch (exception:Exception) {
            Log.e(TAG, "Unable to read TOC", exception)
            //If the TOC fails to read, this will cause an upload of the full note list and re-creation of the TOC, due to the structure of the IDs items won't be duplicated
        }
        if (document != null) {
            var encryptionStateIsGood = true
            if (encryptionStateIsGood){
                var items = getDocumentItems(document)
                for (item in items) {
                    val v = item.split("|")
                    if (v.size==3 || (v.size==4 && StorageManager.encryptContent)) {
                        val noteID = v[0].toLong()
                        val dateModified = v[1].toLong()
                        var titlePartial = v[2]
                        if (StorageManager.encryptContent) {
                            titlePartial = v[2] + "|" + v[3]
                            titlePartial = CryptoManager.decryptStringAES(titlePartial, StorageManager.contentEncryptionKey)
                        }
                        val c = NoteEntry(noteID, 0, "", titlePartial, "", Date(dateModified), Date(dateModified), Date(dateModified))
                        notes.add(c)
                    } else {
                        Log.e(TAG, "Malformed note from TOC size: " + v.size + " content " + item)
                    }
                }
                //Note categories, put on the TOC in plain text
                NotesCategoryCache = ArrayList<NoteCategory>()
                items = getDocumentItems(document,"category")
                for (item in items) {
                    val v = item.split("|")
                    if (v.size==2 ) {
                        val categoryID = v[0].toInt()
                        val categoryName = v[1]
                        val c = NoteCategory(categoryID,categoryName)
                        NotesCategoryCache.add(c)
                    } else {
                        Log.e(TAG, "Malformed note category from TOC size: " + v.size + " content " + item)
                    }
                }
            }
        }
        return notes
    }

    fun deleteNote(noteID: Long) {
        val documentID = generateID(noteID, false)
        val docRef = fsDatabase.collection(NotesTableName).document(documentID)
        docRef
            .delete()
            .addOnSuccessListener {
                println("NoteEntry $noteID successfully deleted!")
            }
            .addOnFailureListener { e ->
                println("Error deleting note $noteID: $e")
            }
    }

    suspend fun getDeletedNotes(): ArrayList<Long> {
        val result = ArrayList<Long>()
        try {
            val document = fsDatabase.collection(NotesDeletionsTableName).document(userID)
                .get()
                .await()
            if (document != null) {
                val items = getDocumentItemsLong(document)
                for (item in items) {result.add(item.toLong())}
            }
        } catch (exception:Exception) {
            Log.e(TAG, "Unable to read $NotesDeletionsTableName collection, disabling Firebase.  Reason: " + exception.toString())
            isFunctional = false
        }
        return result
    }
    fun saveDeletedNotes(ids: ArrayList<Long>){
        val timeStamp = Date().time
        val data = HashMap<String, Any>()
        data["timeStamp"] = timeStamp
        data["itemCount"] = ids.size
        var i = 0
        for (id in ids){
            data["item$i"] = id
            i += 1
        }
        saveDocument(NotesDeletionsTableName, userID, data)
    }


    //-------------------------------------------------- Firebase ChatUsage Management  --------------------------------------------------
    private suspend fun getUsageLastUpdated():Long
    {
        var maxTimeStamp:Long = defaultDate
        try {
            val snap =  fsDatabase.collection(UsageTableName)
                .whereGreaterThanOrEqualTo("id", usageRootID)
                .whereLessThan("id", usageRootID + "Z")
                .orderBy("id", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
            if (snap != null) {
                if (!snap.isEmpty) {
                    if (snap.documents.isNotEmpty()) {
                        val id = snap.documents[0].id.replace("$usageRootID:", "")
                        val x = id.split(":")
                        if (x.size ==2 ) {
                            maxTimeStamp = x[1].toLong()
                        } else {
                            maxTimeStamp = x[0].toLong()
                        }
                        Log.d(TAG, "Max usage read as " + StorageManager.toDate(maxTimeStamp))
                    }
                }
            }
            Log.d(TAG, "getUsageLastUpdated result " + StorageManager.toDate(maxTimeStamp))
        } catch (exception:Exception) {
            Log.e(TAG, "getUsageLastUpdated, unable to read $UsageTableName collection, disabling Firebase", exception)
            isFunctional = false
        }
        return maxTimeStamp
    }

    suspend fun saveUsage(usage: ArrayList<ChatUsage>):Int{
        var updateCount = 0
        usage.sortByDescending  { it.timeStamp  }
        if (usage.size > 0) {
            val lastUpdated = getUsageLastUpdated()
            var itemCount = 0
            val stopID = generateID(lastUpdated, true, usageRootID)
            Log.d(TAG, "Usage sync $stopID ${usage.size} stop at ${StorageManager.toDate(lastUpdated)}")
            var currentID = generateID(usage[0].timeStamp, true, usageRootID)
            var data = HashMap<String, Any>()
            val iterator = usage.iterator()
            while (iterator.hasNext() && currentID >= stopID) {
                val c = iterator.next()
                val id = generateID(c.timeStamp, true, usageRootID)
                //Log.d(TAG, "${c.timeStamp} = " + toDate(c.timeStamp))
                val value = "${c.promptTokens}:${c.completionTokens}:${c.totalTokens}:${c.conversationID}:${c.timeStamp}"
                updateCount += 1
                if (id != currentID) {
                    if (itemCount > 0) {
                        data["id"] = currentID
                        data["itemCount"] = itemCount
                        saveDocument(UsageTableName, currentID, data)
                        data = HashMap()
                        itemCount = 0
                    }
                    currentID = id
                } else {
                    data["item$itemCount"] = value
                    itemCount+=1
                }
            }
            if (itemCount > 0) {
                data["id"] = currentID
                data["itemCount"] = itemCount
                saveDocument(UsageTableName, currentID, data)
            }
        }
        return updateCount
    }

    //-------------------------------------------------- Prices Working Set Management  --------------------------------------------------
    suspend fun getPricesWorkingSetTOCLastUpdated(): Long {
        val documentID = "$rootID:PRICETOC"
        var maxTimeStamp: Long = 0
        try {
            val document = fsDatabase.collection(PricesWorkingSetTableName).document(documentID)
                .get()
                .await()
            if (document != null) {
                // Parse item timestamps from TOC entries ("TICKER|timestamp" format)
                val items = getDocumentItems(document)
                for (item in items) {
                    val parts = item.split("|")
                    if (parts.size >= 2) {
                        val ts = parts[1].toLongOrNull() ?: 0L
                        if (ts > maxTimeStamp) maxTimeStamp = ts
                    }
                }
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Unable to read $PricesWorkingSetTableName TOC", exception)
        }
        Log.d(TAG, "getPricesWorkingSetTOCLastUpdated: $maxTimeStamp")
        return maxTimeStamp
    }

    suspend fun getPricesWorkingSet(): ArrayList<PriceWorkingSetEntry> {
        val result = ArrayList<PriceWorkingSetEntry>()
        try {
            val snap = fsDatabase.collection(PricesWorkingSetTableName)
                .whereGreaterThanOrEqualTo(FieldPath.documentId(), "$rootID:")
                .whereLessThan(FieldPath.documentId(), "$rootID;")
                .get()
                .await()
            if (snap != null && !snap.isEmpty) {
                for (document in snap.documents) {
                    if (document.id.endsWith(":PRICETOC")) continue
                    val entry = PriceWorkingSetEntry()
                    entry.ticker = document.getString("Ticker") ?: document.id.substringAfterLast(":")
                    entry.companyName = document.getString("CompanyName") ?: ""
                    entry.sector = document.getString("Sector") ?: ""
                    entry.sp500Listed = document.getBoolean("SP500Listed") ?: false
                    entry.currentPrice = document.getDouble("CurrentPrice") ?: 0.0
                    entry.average5Day = document.getDouble("Average_5Day") ?: 0.0
                    entry.average2Day = document.getDouble("Average_2Day") ?: 0.0
                    entry.pc2Year = document.getDouble("PC_2Year") ?: 0.0
                    entry.pc1Year = document.getDouble("PC_1Year") ?: 0.0
                    entry.pc6Month = document.getDouble("PC_6Month") ?: 0.0
                    entry.pc3Month = document.getDouble("PC_3Month") ?: 0.0
                    entry.pc2Month = document.getDouble("PC_2Month") ?: 0.0
                    entry.pc1Month = document.getDouble("PC_1Month") ?: 0.0
                    entry.pc1Day = document.getDouble("PC_1Day") ?: 0.0
                    entry.gainMonthly = document.getDouble("Gain_Monthly") ?: 0.0
                    entry.lossStd1Year = document.getDouble("LossStd_1Year") ?: 0.0
                    entry.pointValue = (document.getLong("Point_Value") ?: 0).toInt()
                    entry.targetHoldings = document.getDouble("TargetHoldings") ?: 0.0
                    entry.revenue = document.getDouble("Revenue") ?: 0.0
                    entry.netIncome = document.getDouble("NetIncome") ?: 0.0
                    entry.companySize = (document.getLong("CompanySize") ?: 0).toInt()
                    entry.marketCap = document.getDouble("MarketCap") ?: 0.0
                    entry.operatingExpense = document.getDouble("OperatingExpense") ?: 0.0
                    entry.netProfitMargin = document.getDouble("NetProfitMargin") ?: 0.0
                    entry.earningsPerShare = document.getDouble("EarningsPerShare") ?: 0.0
                    entry.cashShortTermInvestments = document.getDouble("CashShortTermInvestments") ?: 0.0
                    entry.totalAssets = document.getDouble("TotalAssets") ?: 0.0
                    entry.totalLiabilities = document.getDouble("TotalLiabilities") ?: 0.0
                    entry.netWorth = document.getDouble("NetWorth") ?: 0.0
                    entry.totalEquity = document.getDouble("TotalEquity") ?: 0.0
                    entry.sharesOutstanding = document.getDouble("SharesOutstanding") ?: 0.0
                    entry.priceToBook = document.getDouble("PriceToBook") ?: 0.0
                    entry.returnOnAssets = document.getDouble("ReturnOnAssetts") ?: 0.0
                    entry.returnOnCapital = document.getDouble("ReturnOnCapital") ?: 0.0
                    entry.cashFromOperations = document.getDouble("CashFromOperations") ?: 0.0
                    entry.cashFromInvesting = document.getDouble("CashFromInvesting") ?: 0.0
                    entry.cashFromFinancing = document.getDouble("CashFromFinancing") ?: 0.0
                    entry.netChangeInCash = document.getDouble("NetChangeInCash") ?: 0.0
                    entry.freeCashFlow = document.getDouble("FreeCashFlow") ?: 0.0
                    var comments = document.getString("Comments") ?: ""
                    if (StorageManager.encryptContent && comments.isNotEmpty()) {
                        comments = CryptoManager.decryptStringAES(comments, StorageManager.contentEncryptionKey)
                    }
                    entry.comments = comments
                    entry.latestEntry = document.getLong("LatestEntry")?.let { Date(it) }
                        ?: document.getTimestamp("LatestEntry")?.toDate()
                        ?: Date()
                    result.add(entry)
                }
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Unable to read $PricesWorkingSetTableName collection", exception)
        }
        return result
    }

    //-------------------------------------------------- Firebase Database Management  --------------------------------------------------
    suspend fun clearDatabase(deleteAll:Boolean = false) {
        val batch = fsDatabase.batch()
        val snap = fsDatabase.collection(ConversationTableName)
            .get()
            .await()
        if (snap != null) {
            if (!snap.isEmpty) {
                for (document in snap.documents) {
                    val docRef = document.reference
                    if (deleteAll) {
                        batch.delete(docRef)
                    } else {
                        val conversation = document.toObject(Conversation::class.java)
                        if (conversation == null) {
                            batch.delete(docRef)
                        } else {
                            if (conversation.conversationID < 1) {
                                batch.delete(docRef)
                            }
                        }
                    }
                }
            }
        }
        val snap3 = fsDatabase.collection(UsageTableName)
            .get()
            .await()
        if (snap3 != null) {
            if (!snap3.isEmpty) {
                for (document in snap3.documents) {
                    val docRef = document.reference
                    if (deleteAll) {
                        batch.delete(docRef)
                    }
                }
            }
        }
        batch.commit()
            .addOnSuccessListener {
                Log.d(TAG, "Batch conversation cleanup successful")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Batch conversation cleanup failed", e)
            }

    }
}