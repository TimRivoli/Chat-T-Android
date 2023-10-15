```markdown
# com.chatty.android.etc Package

This package contains various data classes used in the Chatty Android application.

## Data Classes

### `ChatActivityType`
- Properties:
  - `activityName`: Name of the activity.
  - `prompt`: Prompt for the activity.
  - `conversational`: Boolean indicating if the activity is conversational.
  - `clearConversationOnChange`: Boolean indicating if the conversation should be cleared on change.
  - `showLanguageOptions`: Boolean indicating if language options should be displayed.
  - `temperature`: Temperature value (Double) for the activity.

### Enum `ChatLanguageOption`
- Represents different language options available.
- Values:
  - English
  - French
  - German
  - Spanish
  - Italian
  - Chinese
  - Japanese
  - Python
  - Java
  - Kotlin
- Methods:
  - `toString()`: Returns the string representation of the language.
  - `companion object`: Provides `fromString(value: String)` method to convert a string to a `ChatLanguageOption`.

### `ChatUsage`
- Properties:
  - `conversationID`: ID of the conversation.
  - `promptTokens`: Number of tokens in the prompt.
  - `completionTokens`: Number of completion tokens.
  - `totalTokens`: Total number of tokens.
  - `userID`: User ID.
  - `androidID`: Android ID.
  - `timeStamp`: Timestamp of the usage.

### `ChatMessage`
- Properties:
  - `role`: Role of the message (e.g., "sender", "receiver").
  - `content`: Content of the message.

### `ChatMessageExtended`
- Properties:
  - `conversationID`: ID of the conversation.
  - `role`: Role of the message.
  - `content`: Content of the message.
  - `timeStamp`: Timestamp of the message.

### `ChatCompletion`
- Properties:
  - `id`: ID of the completion.
  - `object`: Object of the completion.
  - `created`: Timestamp of creation.
  - `model`: Model information.
  - `choices`: List of choices (each containing an index, message, and finish reason).
  - `usage`: Usage information.

### `Choice`
- Properties:
  - `index`: Index of the choice.
  - `message`: Chat message associated with the choice.
  - `finish_reason`: Reason for finishing.

### `Conversation`
- Properties:
  - `conversationID`: ID of the conversation.
  - `title`: Title of the conversation.
  - `summary`: Summary of the conversation.
  - `saved`: Boolean indicating if the conversation is saved.
  - `userID`: User ID.
  - `dateCreated`: Date of creation.
  - `dateAccessed`: Date of last access.
  - `dateModified`: Date of last modification.

### `DeviceSettings`
- Properties:
  - `deviceModel`: Model of the device.
  - `subscriptionLevel`: Subscription level.
  - `useGoogleAuth`: Boolean indicating if Google authentication is used.
  - `syncConversations`: Boolean indicating if conversations are synced.
  - `syncUsage`: Boolean indicating if usage data is synced.

### `SamplePrompt`
- Properties:
  - `activityName`: Name of the activity.
  - `prompt`: Prompt for the activity.
  - `timeStamp`: Timestamp.

---

**Note**: This documentation provides an overview of the data classes and their properties. For detailed information on each class and their usage, refer to the source code.
```