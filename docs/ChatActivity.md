# Chatty Android Package

This package provides classes and utilities for managing conversations and user interactions within the Chatty Android application.

## Class: ChatActivity

### Overview

The `ChatActivity` class is responsible for facilitating conversations between the user and the chatbot, as well as providing a user interface for interaction.

### Methods

#### `onCreate(savedInstanceState: Bundle?)`
- Overrides the `onCreate` method to perform necessary setup operations when the activity is created.

#### `onStart()`
- Overrides the `onStart` method to perform operations when the activity starts.

#### `onResume()`
- Overrides the `onResume` method to perform operations when the activity resumes.

#### `onPause()`
- Overrides the `onPause` method to perform operations when the activity is about to be paused.

#### `onStop()`
- Overrides the `onStop` method to perform operations when the activity is about to be stopped.

#### `onDestroy()`
- Overrides the `onDestroy` method to perform operations when the activity is about to be destroyed.

#### `onBackPressed()`
- Overrides the `onBackPressed` method to handle back button presses, either finishing the activity or clearing the conversation.

#### `onItemSwiped(position: Int)`
- Implements the `SwipeListener` interface to handle the event when a message is swiped.

#### `onSaveInstanceState(outState: Bundle)`
- Overrides the `onSaveInstanceState` method to save the instance state.

#### `startProcessing(start: Boolean)`
- Helper method to toggle processing state, disabling/enabling UI components during processing.

#### `notifyDataChanged()`
- Notifies the adapter that the dataset has changed.

#### `executeChatRequest()`
- Executes a chat request based on the user's input.

#### `clearConversation()`
- Clears the conversation history.

#### `setSaveButtonState()`
- Sets the state and icon of the save button based on conversation save status.

#### `loadConversation(conversationID: Long)`
- Loads a conversation by its ID.

#### `saveConversation()`
- Saves the current conversation.

#### `toggleChatEngine()`
- Toggles the chat engine (model) if the user has a subscription level greater than 1.

#### `toggleSpeakingEnabled()`
- Toggles the speaking feature on/off.

#### `toggleSpeakingPause()`
- Toggles the speaking pause feature.

#### `speakText()`
- Initiates the text-to-speech process if speaking is enabled and there's text to be spoken.

#### `stopSpeaking()`
- Stops the current text-to-speech process.

#### `onChatItemClick(text: String, isUser: Boolean)`
- Handles item clicks in the chat, allowing users to select prompts or interact with responses.

#### `scrollToTop()`
- Scrolls the chat view to the top.

#### `scrollToBottom()`
- Scrolls the chat view to the bottom.

#### `notifyUser(message: String)`
- Displays a short toast message to notify the user.

#### `startConversationListActivity()`
- Starts the `ConversationListActivity` to display the list of conversations.

### Properties

#### `TAG: String`
- A constant string used for logging.

#### Other Properties
- Various properties representing UI elements, flags, and state information used within the activity.

### Usage

```kotlin
// Example usage of ChatActivity class
val chatActivity = ChatActivity()
chatActivity.onCreate(savedInstanceState)
```

## Dependencies

- [RecyclerView](https://developer.android.com/guide/topics/ui/layout/recyclerview)
- [SpeechToTextManager](#) (Assuming this is an internal utility)

## Note

- This class provides the core functionality for handling user input, processing it with the chatbot, and displaying the conversation history. It also manages various UI components and user interactions.

---

