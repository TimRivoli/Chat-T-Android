# Chat-T-Android
Chat-T (Chatty) Android application - using ChatGPT API and SQLite/Firebase storage.

## 📚 Description
Implements:

 -Open AI chat interface with modes for chat, translation, socratic tutor, jokes, and story telling.
 
 -UI elements: recycler view with scroll buttons, swipe to delete messages or conversations.
 
 -Voice input and text to speech output of chat results.
 
 -SQLite database storage for chat conversations and token usage.
 
 -Efficient sync of chat conversations and token usage from SQLite to Firebase.
 
 -Device registration, feature activation, and dynamic content download from Firebase.

 -Secure distribution of API keys via PKI, encryption of content stored in the cloud with private device keys, and secure transfer of encryption keys between devices using PKI to allow encrypted content replication between devices.
 
 -Organized into re-usable modules: Data Classes, ChatManager, CryptoManager, FirebaseManager, StorageManager, and SQLLiteManager.
  
 -ChatManager: handles the active conversation and interaction with the completions API.

 -CryptoManager: handles all of the cryptography functions.
   
 -FirebaseManager: provides efficient Firebase cloud storage of conversation and token usage history, allowing these to be synchronized with my companion Chat-T (chatty) web application.

 -SQLiteManager: provides local SQLite storage for conversation and token usage history.
 
 -StorageManager: front-ends SQLManager and FirebaseManager and handles synchronization between the two.
