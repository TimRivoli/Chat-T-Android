```markdown
# com.chatty.android.etc Package

This package contains the `StorageManager` class responsible for managing data storage and synchronization in the Chatty Android application.

## StorageManager

The `StorageManager` is a singleton class that provides methods for initializing, managing, and synchronizing data in the application.

### Properties

- `useGoogleAuth`: Boolean indicating if Google authentication is used.
- `userID`: User ID.
- `deviceID`: Device ID.
- `subscriptionLevel`: Subscription level.
- `androidID`: Android ID.
- `syncConversations`: Boolean indicating if conversations are synchronized.
- `syncUsage`: Boolean indicating if usage data is synchronized.
- `syncInProgress`: Boolean indicating if a synchronization process is in progress.
- `syncNeeded`: Boolean indicating if synchronization is needed.
- `lastSynced`: Timestamp of the last synchronization.
- `sqLiteManager`: SQLiteManager instance for local data management.
- `localStorage`: SharedPreferences for local storage.

### Methods

- `init(context: Context, passedAndroidID: String)`: Initializes the `StorageManager`.
- `downloadRegisteredDeviceSettings()`: Downloads registered device settings from Firebase.
- `shutDown()`: Shuts down the `StorageManager`.
- Various utility methods for date conversion and retrieval of chat modes.
- Methods for managing conversations, messages, usage, and sample prompts.
- `syncDatabases()`: Synchronizes local and remote databases.

## Usage Example

```kotlin
val context: Context = //... obtain context
val androidID: String = //... obtain android ID
val storageManager = StorageManager.init(context, androidID)
//... perform operations using storageManager
storageManager.shutDown()
```

---

**Note**: This documentation provides an overview of the `StorageManager` class and its methods. For detailed information on each method and their usage, refer to the source code.
```