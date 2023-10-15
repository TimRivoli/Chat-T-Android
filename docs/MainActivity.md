# Chatty Android Package

This package provides functionality for user authentication and navigation within the Chatty Android application.

## Class: MainActivity

### Overview

The `MainActivity` class is responsible for handling user authentication and determining the flow of the application based on the authentication status.

### Methods

#### `onCreate(savedInstanceState: Bundle?)`
- Overrides the `onCreate` method to perform necessary setup operations when the activity is created.

#### `onStart()`
- Overrides the `onStart` method to handle login logic when the activity starts.

#### `startActivity(intent: Intent?, options: Bundle?)`
- Overrides the `startActivity` method to add custom logging functionality.

#### `onRestart()`
- Overrides the `onRestart` method, which is called when the activity is about to be restarted.

#### `onResume()`
- Overrides the `onResume` method, which is called when the activity is about to resume from a paused state.

#### `onPause()`
- Overrides the `onPause` method, which is called when the activity is about to be paused.

#### `onStop()`
- Overrides the `onStop` method, which is called when the activity is about to be stopped.

#### `isFinishing(): Boolean`
- Overrides the `isFinishing` method to provide custom logic when checking if the activity is finishing.

#### `finish()`
- Overrides the `finish` method to provide custom logic when finishing the activity.

#### `onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)`
- Overrides the `onActivityResult` method to handle the result of Google authentication.

#### `firebaseAuthWithGoogle(idToken: String)`
- Initiates Firebase authentication with a Google ID token.

#### `login()`
- Handles the login process, checking if Google authentication is enabled and initiating the appropriate sign-in flow.

#### `showAlert(message: String)`
- Displays an alert dialog with a given message.

#### `startChat()`
- Starts the ChatActivity based on the user's authentication status and subscription level.

### Properties

#### `TAG: String`
- A constant string used for logging.

#### `RC_SIGN_IN: Int`
- A constant integer used as the request code for Google sign-in.

#### `auth: FirebaseAuth`
- An instance of FirebaseAuth for user authentication.

#### `gso: GoogleSignInOptions`
- Google Sign-In options for configuring the sign-in process.

#### `mGoogleSignInClient: GoogleSignInClient`
- An instance of GoogleSignInClient for managing the Google Sign-In process.

### Usage

```kotlin
// Example usage of MainActivity class
val mainActivity = MainActivity()
mainActivity.login()
```

## Dependencies

- [GoogleSignInOptions](https://developers.google.com/identity/sign-in/android/sign-in)
- [FirebaseAuth](https://firebase.google.com/docs/auth)

## Note

- This class contains both UI and authentication logic. It's recommended to separate these concerns for better maintainability and testability.

---

