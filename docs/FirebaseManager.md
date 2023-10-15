# Package `com.chatty.android.etc`

## Overview
This package contains the `FirebaseManager` object, which serves as the interface for managing interactions with Firebase services. The functionalities provided by this object include authentication, data retrieval and storage, and essential functions for managing conversations and sample prompts.

## Classes and Methods

### `FirebaseManager` object

#### Properties

- `TAG`: String - A constant representing the tag used for logging.
- `defaultDate`: Long - A constant representing the default date (1/1/2000).
- `isFunctional`: Boolean - Indicates whether Firebase is functional.
- `isInitialized`: Boolean - Indicates whether Firebase has been initialized.
- `fsDatabase`: FirebaseFirestore - The instance of the Firestore database.
- `deviceID`: String - The device ID.
- `androidID`: String - The Android ID.
- `userID`: String - The user ID.
- `rootID`: String - The root ID.
- `usageRootID`: String - The usage root ID.
- `deviceRootID`: String - The device root ID.

#### Methods

- `initialize(givenAndroidID: String, givenDeviceID: String, useGoogleAuth: Boolean)`: Initializes Firebase with the provided Android ID, Device ID, and whether to use Google authentication.
- `shutDown()`: Shuts down Firebase.
- `generateID(timeInMillis: Long = 0, dayPrecision: Boolean = false, baseID: String = rootID)`: Generates an ID based on provided parameters.
- `getDocumentById(collectionName: String, documentId: String): DocumentSnapshot?`: Retrieves a document from Firestore by ID.
- `saveDocument(collectionName: String, documentID: String, data: HashMap<String, Any>)`: Saves a document to Firestore.
- `getDocumentItems(d: DocumentSnapshot): ArrayList<String>`: Retrieves items from a Firestore document.
- `getDocumentItemsLong(d: DocumentSnapshot): ArrayList<Long>`: Retrieves long items from a Firestore document.
- `updateDeviceStatus()`: Updates the device registration.
- `updateLastSynced()`: Updates the last synced timestamp.
- `activateTrialLicense()`: Activates a trial license for the device.
- `getDeviceSettings(): DeviceSettings`: Retrieves device settings.
- `getSamplePromptsLastUpdated(): Long`: Retrieves the last updated timestamp for sample prompts.
- `getSamplePrompts(cutoff: Long): ArrayList<SamplePrompt>`: Retrieves sample prompts after a given cutoff timestamp.
- `saveSamplePrompts(prompts: ArrayList<SamplePrompt>): Int`: Saves sample prompts to Firestore.
- `getDeletedConversations(): ArrayList<Long>`: Retrieves deleted conversations.
- `saveDeletedConversations(ids: ArrayList<Long>)`: Saves deleted conversations.
- `saveConversation(conversation: Conversation, messages: ArrayList<ChatMessageExtended>)`: Saves a conversation and its messages to Firestore.
- `getConversation(conversationID: Long): Pair<Conversation, ArrayList<ChatMessageExtended>>`: Retrieves a conversation and its messages from Firestore.
- `makeConversationTOC(conversations: ArrayList<Conversation>)`: Creates a Table of Contents for conversations in Firestore.
- `getConversationTOCLastUpdated(): Long`: Retrieves the last updated timestamp for the Table of Contents.
- `getConversationTOC(): ArrayList<Conversation>`: Retrieves the Table of Contents for conversations.
- `deleteConversation(conversationID: Long)`: Deletes a conversation from Firestore.
- `getUsageLastUpdated(): Long`: Retrieves the last updated timestamp for usage data.
- `saveUsage(usage: ArrayList<ChatUsage>): Int`: Saves usage data to Firestore.
- `clearDatabase(deleteAll: Boolean = false)`: Clears conversations and usage data from Firestore.

## Usage
To use the `FirebaseManager`, you need to initialize it with the appropriate IDs and set whether to use Google authentication. After initialization, you can use the provided methods to interact with Firebase services for various functionalities, including managing conversations, sample prompts, and usage data.