```markdown
# com.chatty.android.etc Package

This package contains classes related to Text-to-Speech functionality. It provides methods to convert text into speech.

## `TextToSpeechManager`

This class is responsible for managing Text-to-Speech functionality.

### Constants
- `TAG`: A constant string for logging purposes.
- `maxPauseTime`: The maximum time in milliseconds to pause before canceling speech.

### Properties
- `tts`: An instance of `TextToSpeech` for speech synthesis.
- `speechEnabled`: A boolean indicating if speech synthesis is enabled.
- `isSpeaking`: A boolean indicating if the system is currently speaking.
- `stopRequested`: A boolean indicating if a stop has been requested during speech.
- `pauseRequested`: A boolean indicating if a pause has been requested during speech.
- `currentText`: A string representing the current text to be spoken.
- `currentLanguage`: An enum representing the current language for speech synthesis.

### Methods
- `startup(context: Context)`: Initializes the Text-to-Speech engine.
- `onInit(p0: Int)`: Callback function called when the Text-to-Speech engine is initialized.
- `SetLanguage(lang: ChatLanguageOption)`: Sets the language for speech synthesis.
- `speak(text: String)`: Initiates speech synthesis with the provided text.
- `shutdown()`: Shuts down the Text-to-Speech engine.

#### Note
For initialization and shutdown, you can use the following code snippets:

```kotlin
private lateinit var ttsManager: TextToSpeechManager

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    ttsManager = TextToSpeechManager
    ttsManager.startup(this)
}

override fun onDestroy() {
    super.onDestroy()
    ttsManager.shutdown()
}
```

---

**Note**: This documentation provides an overview of the class and its properties/methods. For detailed information on method parameters, behavior, and usage, refer to the source code.
```