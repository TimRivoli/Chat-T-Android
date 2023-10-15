package com.chatty.android.etc
import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale
import com.chatty.android.etc.DataClasses.*


object TextToSpeechManager : TextToSpeech.OnInitListener {
    private val TAG = "TextToSpeechManager"
    private const val maxPauseTime = 200000 //Miliseconds on pause before canceling
    private lateinit var tts: TextToSpeech
    var speechEnabled: Boolean = false
    var isSpeaking: Boolean = false
    var stopRequested: Boolean = false
    var pauseRequested: Boolean = false
    var currentText : String =""
    lateinit var currentLanguage: ChatLanguageOption

    fun startup(context: Context){
        if (!::tts.isInitialized){
            Log.d(TAG, "TTS initializing... ")
            tts = TextToSpeech(context, this)
        }
    }

    override fun onInit(p0: Int) {
        speechEnabled = false
        Log.d(TAG, "TTS starting onInit... ")
        if (p0 == TextToSpeech.SUCCESS) {
            Log.d(TAG, "TTS setting locale...")
            val locale = Locale.getDefault()
            val result = tts.setLanguage(locale)
            speechEnabled = true
            TextToSpeech.SUCCESS
            Log.d(TAG, "TTS Startup Result:" + result)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                speechEnabled = false
            }
        } else {
            speechEnabled = false
        }
        speechEnabled = true
        if (speechEnabled){
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    Log.d(TAG, "TTS started speaking: " + utteranceId)
                    isSpeaking = true
                }

                override fun onDone(utteranceId: String?) {
                    Log.d(TAG, "TTS complete speaking: " + utteranceId)
                    isSpeaking = false
                }

                override fun onError(utteranceId: String?) {
                    Log.d(TAG, "TTS error speaking: " + utteranceId)
                    isSpeaking = false
                }
            })
        }
    }

    private fun _speak(text: String, utteranceId: Int) {
        isSpeaking = true   //Need to set this right away or the others will queue up before it start
        Log.d(TAG,"TTS Speaking: " + text)
        tts.speak(text, TextToSpeech.QUEUE_ADD, null, utteranceId.toString())
    }
    fun SetLanguage(lang: ChatLanguageOption){
        when (lang) {
            ChatLanguageOption.English -> {
                tts.setLanguage(Locale.ENGLISH)
            }
            ChatLanguageOption.French -> {
                tts.setLanguage(Locale.FRENCH)
            }
            ChatLanguageOption.German -> {
                tts.setLanguage(Locale.GERMAN)
            }
            ChatLanguageOption.Chinese -> {
                tts.setLanguage(Locale.SIMPLIFIED_CHINESE)
            }
            ChatLanguageOption.Italian -> {
                tts.setLanguage(Locale.ITALIAN)
            }
            ChatLanguageOption.Japanese -> {
                tts.setLanguage(Locale.JAPAN)
            }
            else -> {
                tts.setLanguage(Locale.ENGLISH)
            }
        }
    }

    fun speak(text: String) {
        var timeOnPause: Long = 0
        if (speechEnabled) {
            var newRequest :Boolean = true
            if (isSpeaking){
                stopRequested = true
                pauseRequested = false
                while (isSpeaking) {
                    Log.d(TAG, "TTS: stopping speech for new request...")
                    Thread.sleep(100)
                }
                stopRequested = false
            }
            if (newRequest) {
                stopRequested = false
                currentText = text
                if (text.length < 120) {
                    _speak(text,0)
                } else {
                    val thread = Thread {
                        val parts = text.split(Regex("[.!?]+\\s*"))
                        var i =0
                        for (part  in parts) {
                            if (!stopRequested && part !="") {
                                _speak(part, i)
                                i+=1
                                while ((isSpeaking || pauseRequested) && !stopRequested) {
                                    Thread.sleep(100)
                                    timeOnPause +=100
                                    if (timeOnPause > maxPauseTime) {
                                        stopRequested = true
                                        pauseRequested = false
                                    }
                                }
                                timeOnPause = 0
                            }
                        }
                        stopRequested = false
                        pauseRequested = false
                    }
                    thread.start()
                }
            }
        } else {
            Log.d(TAG, "TTS: Speech is not enabled.")
        }
    }

    fun shutdown() {
        tts.shutdown()
    }
}

//For initialization
//private lateinit var ttsManager: TTSManager
//override fun onCreate(savedInstanceState: Bundle?) {
//    super.onCreate(savedInstanceState)
//    setContentView(R.layout.activity_main)
//    ttsManager = TTSManager {
//    }
//}
//override fun onDestroy() {
//    super.onDestroy()
//    ttsManager.shutdown()
//}

