package com.chatty.android.etc

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.text.Editable
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale

@SuppressLint("StaticFieldLeak")
object SpeechToTextManager {
    private const val TAG = "SpeechToTextManager"
    private const val REQUEST_CODE = 100
    private lateinit var tvTarget: TextView

    private fun requestPermission(context: AppCompatActivity) {
        Log.d(TAG, "Speech recognizer requesting permission")
        ActivityCompat.requestPermissions( context,   arrayOf(Manifest.permission.RECORD_AUDIO),  REQUEST_CODE  )
    }

    fun isPermissionGranted(context: AppCompatActivity): Boolean {
        Log.d(TAG, "Speech recognizer checking permission")
        var r = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO ) == PackageManager.PERMISSION_GRANTED
        if (!r) {
            requestPermission(context)
            var r = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO ) == PackageManager.PERMISSION_GRANTED
        }
        return r
    }

    fun startSpeechToText(context: AppCompatActivity, textView: TextView) {
        Log.d(TAG, "Speech recognizer startSpeechToText")
        tvTarget = textView
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra( RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM  )
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE,
            Locale.getDefault()
        )
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
        val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "Speech recognizer is ready to start listening.")
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "Speech recognizer user has started to speak.")
            }

            override fun onRmsChanged(rmsdB: Float) {
                //                Log.d(TAG, "Speech recognizer RMS changes (volume of the input audio).") //very prolific
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                Log.d(TAG, "Speech recognizer sound has been received and it is not speech.")
            }

            override fun onEndOfSpeech() {
                Log.d(TAG, "Speech recognizer user has stopped speaking.")
            }

            override fun onError(error: Int) {
                Log.e(TAG, "Speech recognizer error: ${error.toString()}")
            }

            override fun onResults(results: Bundle?) {
                Log.d(TAG, "Speech recognizer onResults.")
                var speechText: String = ""
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val speechText = matches[0]
                    val cursorPosition = tvTarget.selectionStart // Get the current cursor position
                    val editable = tvTarget.text as Editable
                    editable.insert(cursorPosition, speechText)
                    Log.d(TAG, "Speech recognizer txtUserInput value set.")
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                Log.d(TAG, "Speech recognizer parital results.")
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                // Reserved for future use.
            }
        })

        speechRecognizer.startListening(intent)
    }

}