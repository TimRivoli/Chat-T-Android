package com.chatty.android

import android.app.Application
import android.provider.Settings
import android.util.Log
import com.chatty.android.etc.ChatManager
import com.chatty.android.etc.CryptoManager
import com.chatty.android.etc.StorageManager
import com.chatty.android.etc.TextToSpeechManager
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import java.security.KeyStore

class ChattYApplication : Application() {
//creating references here keeps the singleton classes from getting constantly recycled
    private val TAG = "MainApplication"
    private lateinit var cm: ChatManager
    private lateinit var sm: StorageManager

    override fun onCreate() {
        Log.d(TAG, "onCreate")
        super.onCreate()
        val androidID = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            CryptoManager.initialize(keyStore)
            Log.d(TAG, "KeyStore loaded")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "KeyStore load failure: " + e.toString())
        }
        val appCheck = FirebaseAppCheck.getInstance()
        if (BuildConfig.DEBUG) {
            appCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
        } else {
            appCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
        }
        sm = StorageManager.init(applicationContext, androidID)
        cm = ChatManager()
        TextToSpeechManager.startup(applicationContext)
    }

    override fun onTerminate() {
        Log.d(TAG, "onCreate")
        super.onTerminate()
        TextToSpeechManager.shutdown()
        sm.shutDown()
    }
}