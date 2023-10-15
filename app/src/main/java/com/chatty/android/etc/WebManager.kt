package com.chatty.android.etc
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import com.google.gson.Gson
import com.chatty.android.etc.DataClasses.*

object WebManager {
    private const val TAG = "WebManager"

    suspend fun callChatCompletionAPI(messages: List<ChatMessage>, temperature: Double=0.2, model: String="gpt-3.5-turbo")
    : ChatCompletion = withContext(Dispatchers.IO) {
        val headers = mapOf(
            "Content-Type" to "application/json",
            "Authorization" to "Bearer ${StorageManager.API_KEY}"
        )
        val data = mapOf(
            "model" to model,
            "messages" to messages,
            "temperature" to temperature
        )
        //can also include these optional parameters
        //functions: [name, description, parameters
        //function_call: auto, none
        //top_p, n, max_tokens, stop, presence_penalty, frequency_penalty, logit_bias
        val gson = Gson()
        val json_data_string = gson.toJson(data).toString()
        try {
            val url = URL(OPENAI_COMPLETIONS_ENDPOINT) //
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            for ((key, value) in headers) { connection.setRequestProperty(key, value)  }
            connection.doOutput = true
            val outputStream = OutputStreamWriter(connection.outputStream)
            outputStream.write(json_data_string)
            outputStream.flush()
            //Log.d(TAG, "Response code: ${connection.responseCode}")
            val inputStream = connection.inputStream
            val responseText = inputStream.bufferedReader().use { it.readText() }
            val chatCompletion = gson.fromJson(responseText, ChatCompletion::class.java)
            Log.d(TAG, "Finish Reason: ${chatCompletion.choices[0].finish_reason}")
            return@withContext chatCompletion
        } catch (e: Exception) {
            Log.e(TAG, "Unable to generate ChatCompletion response")
            Log.e(TAG, "Exception: $e" )
            val choice = Choice(0,message=ChatMessage("Error", e.message.toString()), finish_reason = "Error")
            return@withContext ChatCompletion(choices=listOf(choice), usage=Usage(0,0,0))
        }
    }

}