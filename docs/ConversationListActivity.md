# Chatty Android Package

This package provides functionality for managing conversations and displaying the list of conversations within the Chatty Android application.

## Class: ConversationListActivity

### Overview

The `ConversationListActivity` class is responsible for displaying the list of conversations, allowing the user to interact with them, and providing swipe gestures for certain actions.

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

#### `onItemSwiped(position: Int, conversationID: String)`
- Implements the `SwipeListener` interface to handle the event when a conversation is swiped.

#### `onItemClick(conversationID: Long)`
- Handles the event when a conversation item is clicked, initiating a transition to the `ChatActivity`.

### Properties

#### `TAG: String`
- A constant string used for logging.

#### `recyclerView: RecyclerView`
- An instance of `RecyclerView` for displaying the list of conversations.

#### `scrollUp: ImageButton`
- An instance of `ImageButton` for scrolling up the conversation list.

#### `scrollDown: ImageButton`
- An instance of `ImageButton` for scrolling down the conversation list.

#### `swipeRefreshLayout: SwipeRefreshLayout`
- An instance of `SwipeRefreshLayout` for enabling pull-to-refresh functionality.

#### `firstVisibleItemPosition: Int`
- Keeps track of the position of the first visible item in the conversation list.

#### `lastVisibleItemPosition: Int`
- Keeps track of the position of the last visible item in the conversation list.

#### `totalItemCount: Int`
- Keeps track of the total number of items in the conversation list.

### Usage

```kotlin
// Example usage of ConversationListActivity class
val conversationListActivity = ConversationListActivity()
conversationListActivity.onCreate(savedInstanceState)
```

## Dependencies

- [RecyclerView](https://developer.android.com/guide/topics/ui/layout/recyclerview)
- [SwipeRefreshLayout](https://developer.android.com/jetpack/androidx/releases/swiperefreshlayout)

## Note

- This class provides the UI and logic for managing the list of conversations. 

---

