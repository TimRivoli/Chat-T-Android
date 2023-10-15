package com.chatty.android.etc

import java.util.Date

class DataClasses {
    data class ChatActivityType(val activityName: String="", val prompt: String="", val conversational: Boolean=false, val clearConversationOnChange: Boolean=true, val showLanguageOptions:Boolean=false, val temperature:Double=0.2)
    enum class ChatLanguageOption {
        English, French, German, Spanish, Italian, Chinese, Japanese, Python, Java, Kotlin;

        override fun toString(): String {
            return when (this) {
                English -> "English"
                French -> "French"
                German -> "German"
                Spanish -> "Spanish"
                Italian -> "Italian"
                Chinese -> "Chinese"
                Japanese -> "Japanese"
                Python -> "Python"
                Java -> "Java"
                Kotlin -> "Kotlin"
            }
        }

        companion object {
            fun fromString(value: String): ChatLanguageOption? {
                return when (value) {
                    "English" -> English
                    "French" -> French
                    "German" -> German
                    "Spanish" -> Spanish
                    "Italian" -> Italian
                    "Chinese" -> Chinese
                    "Japanese" -> Japanese
                    "Python" -> Python
                    "Java" -> Java
                    "Kotlin" -> Kotlin
                    else -> null
                }
            }
        }
    }
    data class ChatUsage(var conversationID: Long=0, val promptTokens:Int=0, val completionTokens:Int=0, val totalTokens:Int=0, var userID: String="", var androidID: String = "", val timeStamp: Long = Date().time)
    data class ChatMessage(val role: String, val content: String)
    data class ChatMessageExtended(val conversationID: Long=0, var role: String="", var content: String="", var timeStamp: Long= Date().time)
    data class ChatCompletion(val id: String="", val `object`: String="", val created: Long=0,val model: String="", val choices: List<Choice>,val usage: Usage)
    data class Choice(val index: Int,val message: ChatMessage,val finish_reason: String)
    data class Conversation(var conversationID: Long= Date().time, var title: String="", var summary: String="", var saved: Boolean = false, var userID: String="", var dateCreated: Date = Date(), var dateAccessed: Date = Date(), var dateModified: Date = Date())
    data class SamplePrompt(var activityName: String ="Conversation", var prompt: String="", var timeStamp: Long = 0 )
    data class Usage(val prompt_tokens: Int,val completion_tokens: Int,val total_tokens: Int)
}