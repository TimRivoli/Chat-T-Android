package com.chatty.android.etc

import android.os.Build
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.DocumentSnapshot
import java.util.Calendar
import java.util.Date
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

    fun initialize(givenAndroidID: String, givenDeviceID: String, useGoogleAuth:Boolean) {
        val auth = FirebaseAuth.getInstance()
        androidID = givenAndroidID
        deviceID = givenDeviceID
        if (useGoogleAuth) { //Only set userID for non-anonymous
            userID = auth.currentUser?.uid ?: ""
            StorageManager.userID = userID
        }
        try {
            if (auth.currentUser != null) {
                Log.d(TAG, "Firebase was authenticated automatically. UserID: " + StorageManager.userID)
            } else {
                Log.d(TAG, "Firebase was not authenticated automatically...")
                if (firebaseUserID !="" && firebasePwd!=""){
                    Log.d(TAG,"Firebase: Logging in with email and password...")
                    auth.signInWithEmailAndPassword(firebaseUserID, firebasePwd)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                userID = auth.currentUser?.uid ?: ""
                                StorageManager.userID = userID
                                Log.d(TAG, "Firebase: Logged in with email and password as $userID")
                            }
                        }
                } else {
                    Log.d(TAG,"Firebase: Logging anonymously...")
                    auth.signInAnonymously()
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val x = auth.currentUser?.uid ?: ""
                                Log.d(TAG, "Firebase: Logged in anonymously userID: $x")
                            }
                        }
                }
            }
        } catch (ex: Exception) {
            Log.e(TAG, ex.message.toString())
            Log.e(TAG,"Firebase: Login failed.  Disabling firebase")
            isFunctional = false
        }
        rootID = androidID
        deviceRootID = "$androidID:$deviceID"
        usageRootID = androidID
        if (userID != ""){
            rootID = userID
            usageRootID += ":$userID"
        }

        if (isFunctional) { fsDatabase = FirebaseFirestore.getInstance()        }
        isInitialized = isFunctional
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
        //Log.d(TAG, "Saving document to $collectionName documentID $documentID")
        fsDatabase.collection(collectionName).document(documentID).set(data)
            .addOnSuccessListener {
                //Log.d(TAG, "Saved to $collectionName documentID $documentID")
            }
            .addOnFailureListener { e ->
                Log.w(TAG,"Error saving to $collectionName documentID $documentID", e )
            }
    }
    private fun getDocumentItems(d: DocumentSnapshot):ArrayList<String> {
        val result = ArrayList<String>()
        val itemCount = d.getLong("itemCount") ?: 0
        for (i in 0 until itemCount ) {
            val x = d.getString("item$i") ?: ""
            if (x != "") { result.add(x) }
        }
        return result
    }
    private fun getDocumentItemsLong(d: DocumentSnapshot):ArrayList<Long> {
        val result = ArrayList<Long>()
        val itemCount = d.getLong("itemCount") ?: 0
        for (i in 0 until itemCount ) {
            val x = d.getLong("item$i") ?: 0
            if (x > -1) { result.add(x) }
        }
        return result
    }

    //-------------------------------------------------- device setup functions  --------------------------------------------------
    private fun updateDeviceStatus() {
        //Log.d(TAG, "Updating device registration: $deviceRootID")
        val deviceModel = Build.MODEL
        val publicKeyString = CryptoManager.getPublicKeyString()
        val data = hashMapOf("deviceModel" to deviceModel,"userID" to userID, "publicKey" to publicKeyString, "timeStamp" to Date().time )
        fsDatabase.collection(RegistrationTableName).document(deviceRootID).set(data)
            .addOnSuccessListener {
                Log.d(TAG, "Updated device registration: $deviceRootID")
            }
            .addOnFailureListener { e ->
                Log.w(TAG,"Update device registration error: $deviceRootID", e )
            }
    }
    fun updateLastSynced() {
        fsDatabase.collection(RegistrationTableName).document(deviceRootID).update("lastSynced", StorageManager.lastSynced)
            .addOnSuccessListener {
                Log.d(TAG, "updateLastSynced successfull")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "updateLastSynced error: ", e)
            }

    }
    private fun activateTrialLicense() {
        Log.d(TAG, "Activating trial license for device: $deviceRootID")
        val data = hashMapOf("subscriptionLevel" to 1, "useGoogleAuth" to false, "syncUsage" to true, "timeStamp" to Date().time  )
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
            val x = document.getLong("subscriptionLevel") ?:0
            var subscriptionLevel  = x.toInt()
            val syncConversations = document.getBoolean("syncConversations") ?:false
            val syncUsage = document.getBoolean("syncUsage") ?:true
            var apiKey = document.getString("apiKey") ?:""
            var webClientID = document.getString("webClientID") ?:""
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
    //-------------------------------------------------- Firebase Conversation Management  --------------------------------------------------
    suspend fun getDeletedConversations(): ArrayList<Long> {
        val result = ArrayList<Long>()
        try {
            val document = fsDatabase.collection(DeletionsTableName).document(userID)
                .get()
                .await()
            if (document != null) {
                val items = getDocumentItemsLong(document)
                for (item in items) {result.add(item.toLong())}
            }
        } catch (exception:Exception) {
            Log.e(TAG, "Unable to read $DeletionsTableName collection, disabling Firebase.  Reason: " + exception.toString())
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
        saveDocument(DeletionsTableName, userID, data)
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
                        if (keyTransferRequestedCertificate != "") {
                            Log.d(TAG, "Sending response to: $keyTransferRequestedCertificate")
                            keyTransferResponse = CryptoManager.encryptStringRSA(StorageManager.contentEncryptionKey, keyTransferRequestedCertificate)
                            val documentData = document.data
                            if (documentData != null) {
                                Log.d(TAG, "My response: $keyTransferResponse")
                                documentData.set("transferResponse", keyTransferResponse)
                                saveDocument(ConversationTableName, documentID, documentData)
                                Log.d(TAG, "getConversationTOC, sent key.")
                            }
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