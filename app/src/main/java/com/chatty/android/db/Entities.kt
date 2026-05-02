package com.chatty.android.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val conversationID: Long,
    val dateCreated: Long = 0,
    val dateAccessed: Long = 0,
    val dateModified: Long = 0,
    val saved: Int = 0,
    val title: String = "",
    val summary: String = "",
    val userID: String = "",
    val firebaseID: String = ""
)

@Entity(tableName = "conversations_deleted")
data class ConversationDeletionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val conversationID: Long = 0,
    val timeStamp: Long = 0
)

@Entity(tableName = "messages", primaryKeys = ["conversationID", "timeStamp"])
data class MessageEntity(
    val conversationID: Long,
    val timeStamp: Long,
    val role: String = "",
    val content: String = ""
)

@Entity(tableName = "usage")
data class UsageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val conversationID: Long = 0,
    val timeStamp: Long = 0,
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val totalTokens: Int = 0,
    val userID: String = "",
    val androidID: String = ""
)

@Entity(tableName = "sample_prompts")
data class SamplePromptEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val activityName: String = "",
    val prompt: String = "",
    val used: Int = 0,
    val timeStamp: Long = 0
)

@Entity(tableName = "chat_modes")
data class ChatModeEntity(
    @PrimaryKey val activityName: String,
    val prompt: String = "",
    val conversational: Int = 0,
    val clearConversationOnChange: Int = 1,
    val showLanguageOptions: Int = 0,
    val temperature: Double = 0.2,
    val sequence: Int = 0,
    val timeStamp: Long = 0
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val noteID: Long,
    val categoryID: Int = 0,
    val title: String = "",
    val content: String = "",
    val dateCreated: Long = 0,
    val dateModified: Long = 0,
    val dateAccessed: Long = 0
)

@Entity(tableName = "notes_categories")
data class NoteCategoryEntity(
    @PrimaryKey val categoryID: Int,
    val categoryName: String = ""
)

@Entity(tableName = "notes_deleted")
data class NoteDeletionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val noteID: Long = 0,
    val timeStamp: Long = 0
)

@Entity(tableName = "prices_working_set")
data class PriceEntity(
    @PrimaryKey val ticker: String,
    val companyName: String = "",
    val sector: String = "",
    val sp500Listed: Int = 0,
    val currentPrice: Double = 0.0,
    val average5Day: Double = 0.0,
    val average2Day: Double = 0.0,
    val pc2Year: Double = 0.0,
    val pc1Year: Double = 0.0,
    val pc6Month: Double = 0.0,
    val pc3Month: Double = 0.0,
    val pc2Month: Double = 0.0,
    val pc1Month: Double = 0.0,
    val pc1Day: Double = 0.0,
    val gainMonthly: Double = 0.0,
    val lossStd1Year: Double = 0.0,
    val pointValue: Int = 0,
    val targetHoldings: Double = 0.0,
    val revenue: Double = 0.0,
    val netIncome: Double = 0.0,
    val companySize: Int = 0,
    val marketCap: Double = 0.0,
    val operatingExpense: Double = 0.0,
    val netProfitMargin: Double = 0.0,
    val earningsPerShare: Double = 0.0,
    val cashShortTermInvestments: Double = 0.0,
    val totalAssets: Double = 0.0,
    val totalLiabilities: Double = 0.0,
    val netWorth: Double = 0.0,
    val totalEquity: Double = 0.0,
    val sharesOutstanding: Double = 0.0,
    val priceToBook: Double = 0.0,
    val returnOnAssets: Double = 0.0,
    val returnOnCapital: Double = 0.0,
    val cashFromOperations: Double = 0.0,
    val cashFromInvesting: Double = 0.0,
    val cashFromFinancing: Double = 0.0,
    val netChangeInCash: Double = 0.0,
    val freeCashFlow: Double = 0.0,
    val comments: String = "",
    val latestEntry: Long = 0
)
