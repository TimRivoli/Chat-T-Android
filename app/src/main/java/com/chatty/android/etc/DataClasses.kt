package com.chatty.android.etc

import java.util.Date

class DataClasses {
    data class ChatActivityType(val activityName: String="", val prompt: String="", val conversational: Boolean=false, val clearConversationOnChange: Boolean=true, val showLanguageOptions:Boolean=false, val temperature:Double=0.2)
    enum class ChatLanguageOption {
        English, French, German, Spanish, Italian, Chinese, Japanese, Nepalese, Python, Java, Kotlin;
        override fun toString(): String {
            return when (this) {
                English -> "English"
                French -> "French"
                German -> "German"
                Spanish -> "Spanish"
                Italian -> "Italian"
                Chinese -> "Chinese"
                Japanese -> "Japanese"
                Nepalese -> "Nepalese"
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
                    "Nepalese" -> Nepalese
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
    data class NoteEntry(var noteID: Long=0, var categoryID: Int=0, var categoryName: String="", var title: String="", var content: String="", var dateCreated: Date = Date(), var dateAccessed: Date = Date(), var dateModified: Date = Date())
    data class NoteCategory(var categoryID: Int=0, var categoryName: String="")
    data class PriceWorkingSetEntry(
        var ticker: String = "",
        var companyName: String = "",
        var sector: String = "",
        var sp500Listed: Boolean = false,
        var currentPrice: Double = 0.0,
        var average5Day: Double = 0.0,
        var average2Day: Double = 0.0,
        var pc2Year: Double = 0.0,
        var pc1Year: Double = 0.0,
        var pc6Month: Double = 0.0,
        var pc3Month: Double = 0.0,
        var pc2Month: Double = 0.0,
        var pc1Month: Double = 0.0,
        var pc1Day: Double = 0.0,
        var gainMonthly: Double = 0.0,
        var lossStd1Year: Double = 0.0,
        var pointValue: Int = 0,
        var targetHoldings: Double = 0.0,
        var revenue: Double = 0.0,
        var netIncome: Double = 0.0,
        var companySize: Int = 0,
        var marketCap: Double = 0.0,
        var operatingExpense: Double = 0.0,
        var netProfitMargin: Double = 0.0,
        var earningsPerShare: Double = 0.0,
        var cashShortTermInvestments: Double = 0.0,
        var totalAssets: Double = 0.0,
        var totalLiabilities: Double = 0.0,
        var netWorth: Double = 0.0,
        var totalEquity: Double = 0.0,
        var sharesOutstanding: Double = 0.0,
        var priceToBook: Double = 0.0,
        var returnOnAssets: Double = 0.0,
        var returnOnCapital: Double = 0.0,
        var cashFromOperations: Double = 0.0,
        var cashFromInvesting: Double = 0.0,
        var cashFromFinancing: Double = 0.0,
        var netChangeInCash: Double = 0.0,
        var freeCashFlow: Double = 0.0,
        var comments: String = "",
        var latestEntry: Date = Date()
    )

}