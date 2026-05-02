package com.chatty.android.etc

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.chatty.android.etc.DataClasses.ChatCompletion
import com.chatty.android.etc.DataClasses.ChatMessage
import com.chatty.android.etc.DataClasses.Choice
import com.chatty.android.etc.DataClasses.Usage
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URL
import java.util.Date
import java.util.concurrent.TimeUnit

object NetworkManager {
    private const val TAG = "NetworkManager"
    var isNetworkGood: Boolean = false
    var isNetworkWifi: Boolean = false
    private var networkstatusLastChecked: Long = 0
    private val networkCheckCooldown: Long = 15000

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun onCoolDown(): Boolean =
        (Date().time - networkstatusLastChecked) < networkCheckCooldown

    private fun isInternetAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork)
        return caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    private fun getNetworkType(context: Context): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork)
        caps?.let {
            if (it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return "Wi-Fi"
            if (it.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) return "Mobile Data"
        }
        return "Unknown"
    }

    fun checkNetworkStatus(context: Context) {
        if (!onCoolDown()) {
            Log.d(TAG, "Checking network status...")
            isNetworkWifi = getNetworkType(context) == "Wi-Fi"
            isNetworkGood = isInternetAvailable(context)
            Log.d(TAG, "isNetworkWifi: $isNetworkWifi  isInternetAvailable: $isNetworkGood")
            networkstatusLastChecked = Date().time
        }
    }

    private suspend fun isURLAccessible(urlAddress: String): Boolean {
        if (onCoolDown()) {
            Log.d(TAG, "Suppressing network check due to cooldown")
            return isNetworkGood
        }
        return try {
            Log.d(TAG, "Testing URL $urlAddress")
            val url = URL(urlAddress)
            val connection = url.openConnection()
            connection.connect()
            connection.getInputStream().close()
            isNetworkGood = true
            true
        } catch (e: Exception) {
            Log.e(TAG, e.toString())
            isNetworkGood = false
            false
        }.also { networkstatusLastChecked = Date().time }
    }

    private suspend fun isOpenAPIAccessible(): Boolean = isURLAccessible(OPENAI_API_ENDPOINT)

    suspend fun callChatCompletionAPI(
        messages: List<ChatMessage>,
        temperature: Double = 0.2,
        model: String = "gpt-3.5-turbo"
    ): ChatCompletion = withContext(Dispatchers.IO) {
        if (!isNetworkGood) {
            isNetworkGood = isOpenAPIAccessible()
            Log.d(TAG, "isOpenAPIAccessible: $isNetworkGood")
        }
        if (!isNetworkGood) {
            Log.e(TAG, "Network not functional, skipping OpenAI request.")
            return@withContext errorResponse("No Internet")
        }

        val gson = Gson()
        val payload = mapOf("model" to model, "messages" to messages, "temperature" to temperature)
        val requestBody = gson.toJson(payload).toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(OPENAI_COMPLETIONS_ENDPOINT)
            .header("Authorization", "Bearer ${StorageManager.API_KEY}")
            .post(requestBody)
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val chatCompletion = gson.fromJson(body, ChatCompletion::class.java)
                    Log.d(TAG, "Response code: ${response.code}  Finish reason: ${chatCompletion.choices.firstOrNull()?.finish_reason}")
                    chatCompletion
                } else {
                    Log.e(TAG, "API error ${response.code}: $body")
                    errorResponse("[${response.code}] $body")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to complete ChatCompletion request: $e")
            errorResponse(e.message ?: "Unknown error")
        }
    }

    private fun errorResponse(message: String) = ChatCompletion(
        choices = listOf(Choice(0, ChatMessage("Error", message), "Error")),
        usage = Usage(0, 0, 0)
    )
}
