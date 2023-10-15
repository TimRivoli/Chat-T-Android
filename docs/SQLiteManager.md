# com.chatty.android.etc Package

This package contains classes related to managing chat data, including conversations, messages, usage statistics, and sample prompts. It interfaces with an SQLite database for local storage.

## `SQLiteHelper`

This class provides a SQLiteOpenHelper for managing the database. It handles database creation and version management.

### Constants
- `DATABASE_VERSION`: The version number of the database.
- `DATABASE_NAME`: The name of the database.
- `CREATE_CONVERSATION_TABLE`: SQL statement to create the conversation table.
- `CREATE_CONVERSATION_DELETED_TABLE`: SQL statement to create the table for deleted conversations.
- `CREATE_MESSAGES_TABLE`: SQL statement to create the messages table.
- `CREATE_USAGE_TABLE`: SQL statement to create the usage table.
- `CREATE_SAMPLEPROMPTS_TABLE`: SQL statement to create the sample prompts table.

### Methods
- `onCreate(db: SQLiteDatabase)`: Called when the database is created.
- `onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int)`: Called when the database needs to be upgraded.
- `recreateDatabase(db: SQLiteDatabase)`: Drops and re-creates all tables.
- `applyUpdates(db: SQLiteDatabase)`: Applies updates to the database schema.
  
## `SQLiteManager`

This class is responsible for managing interactions with the SQLite database, including CRUD (Create, Read, Update, Delete) operations for conversations, messages, usage, and sample prompts.

### Methods
- `shutDown()`: Closes the database connection.
- `clearDatabase(recreateDatabase: Boolean = false)`: Clears the database, optionally recreating it.
- `getConversationsLastUpdated(userID: String): Long`: Retrieves the timestamp of the last modified conversation.
- `updateConversationDates(conversation: Conversation)`: Updates conversation date information.
- `saveConversation(conversation: Conversation)`: Saves a conversation to the database.
- `deleteConversation(conversationID: Long)`: Deletes a conversation and associated messages.
- `getDeletedConversations(userID: String): ArrayList<Long>`: Retrieves a list of deleted conversation IDs.
- `getAllConversations(userID: String): List<Conversation>`: Retrieves a list of all conversations for a given user.
- `getConversation(conversationID: Long, userID: String): Conversation`: Retrieves a specific conversation for a given user.
- `updateConversationModified(conversation: Conversation)`: Updates the modified date of a conversation.
- `appendMessage(conversation: Conversation, chatMessage: ChatMessageExtended)`: Appends a message to the messages table.
- `deleteMessage(message: ChatMessageExtended)`: Deletes a message.
- `getMessages(conversationID: Long): List<ChatMessageExtended>`: Retrieves a list of messages for a conversation.
- `getConversationUsage(conversationID: Long): Int`: Retrieves the total token count for a conversation.
- `getUsage(userID: String, androidID: String): ArrayList<ChatUsage>`: Retrieves usage statistics for a user.
- `getUsageLastUpdated(userID: String, androidID: String): Long`: Retrieves the timestamp of the last usage update.
- `appendUsage(usage: ChatUsage)`: Appends usage statistics to the database.
- `getSamplePrompt(activityName: String, unused: Boolean = false): SamplePrompt`: Retrieves a sample prompt.
- `reuseSamplePrompts()`: Resets sample prompts to unused status.
- `getSamplePrompts(cuttoff: Long, unused: Boolean = false): ArrayList<SamplePrompt>`: Retrieves a list of sample prompts.
- `cleanSamplePrompts()`: Removes duplicate sample prompts.
- `appendSamplePrompt(timeStamp: Long, prompt: SamplePrompt)`: Appends a sample prompt to the database.
- `getSamplePromptsLastUpdated(): Long`: Retrieves the timestamp of the last sample prompt update.
- `getSamplePromptsCount(unused: Boolean = false): Long`: Retrieves the count of sample prompts.
- `applyFixes(userID: String, androidID: String)`: Applies fixes and maintenance tasks to the database.

---

**Note**: This documentation provides an overview of the classes and their methods. For detailed information on method parameters, behavior, and usage, refer to the source code.
