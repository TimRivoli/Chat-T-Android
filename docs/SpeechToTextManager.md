```markdown
# com.chatty.android.etc Package

This package contains classes related to Speech-to-Text functionality. It provides methods to convert spoken words into text.

## `SpeechToTextManager`

This class is responsible for managing Speech-to-Text functionality.

### Constants
- `TAG`: A constant string for logging purposes.
- `REQUEST_CODE`: A request code used for requesting permission.
- `tvTarget`: A `TextView` object to store the speech-to-text result.

### Methods
- `requestPermission(context: AppCompatActivity)`: Requests the necessary permission to use the Speech Recognizer.
- `isPermissionGranted(context: AppCompatActivity): Boolean`: Checks if the required permission is granted. If not, it requests permission and checks again.
- `startSpeechToText(context: AppCompatActivity, textView: TextView)`: Starts the Speech Recognizer to convert speech to text.

#### `startSpeechToText` Method Listeners
- `onReadyForSpeech(params: Bundle?)`: Called when the Speech Recognizer is ready to start listening.
- `onBeginningOfSpeech()`: Called when the user starts speaking.
- `onRmsChanged(rmsdB: Float)`: Called when the volume of the input audio changes.
- `onBufferReceived(buffer: ByteArray?)`: Called when sound is received and it is not speech.
- `onEndOfSpeech()`: Called when the user stops speaking.
- `onError(error: Int)`: Called in case of an error during recognition.
- `onResults(results: Bundle?)`: Called when speech recognition is successful, providing the recognized text.
- `onPartialResults(partialResults: Bundle?)`: Called when partial results are available.
- `onEvent(eventType: Int, params: Bundle?)`: Reserved for future use.

---

**Note**: This documentation provides an overview of the classes and their methods. For detailed information on method parameters, behavior, and usage, refer to the source code.
```