package com.chatty.android.etc

import android.util.Log
import com.chatty.android.db.AppDatabase
import com.chatty.android.db.ChatModeEntity
import com.chatty.android.db.ConversationDeletionEntity
import com.chatty.android.db.ConversationEntity
import com.chatty.android.db.MessageEntity
import com.chatty.android.db.NoteCategoryEntity
import com.chatty.android.db.NoteDeletionEntity
import com.chatty.android.db.NoteEntity
import com.chatty.android.db.PriceEntity
import com.chatty.android.db.SamplePromptEntity
import com.chatty.android.db.UsageEntity
import com.chatty.android.etc.DataClasses.ChatActivityType
import com.chatty.android.etc.DataClasses.ChatMessageExtended
import com.chatty.android.etc.DataClasses.ChatUsage
import com.chatty.android.etc.DataClasses.Conversation
import com.chatty.android.etc.DataClasses.NoteCategory
import com.chatty.android.etc.DataClasses.NoteEntry
import com.chatty.android.etc.DataClasses.PriceWorkingSetEntry
import com.chatty.android.etc.DataClasses.SamplePrompt
import com.chatty.android.etc.DynamicContentGeneration.defaultNoteCategories
import java.util.Date

class LocalDatabase(private val db: AppDatabase) {
    private val TAG = "LocalDatabase"
    private var noteCategories = ArrayList<NoteCategory>()

    fun shutDown() { /* Room manages its own connection lifecycle */ }

    fun clearDatabase(recreateDatabase: Boolean = false) {
        if (recreateDatabase) {
            db.clearAllTables()
            db.noteCategoryDao().insertIfNotExists(NoteCategoryEntity(0, "General"))
            noteCategories.clear()
        } else {
            db.conversationDao().deleteAll()
            db.conversationDeletionDao().deleteAll()
            db.messageDao().deleteAll()
            db.usageDao().deleteAll()
            db.chatModeDao().clearAll()
        }
    }

    // ---------------------------------- Conversations ------------------------------------------

    fun getConversationsLastUpdated(userID: String): Long =
        db.conversationDao().getLastModified(userID) ?: 0L

    fun updateConversationDates(conversation: Conversation) {
        db.conversationDao().updateDates(
            conversation.conversationID,
            conversation.dateCreated.time,
            conversation.dateAccessed.time,
            conversation.dateModified.time
        )
    }

    fun saveConversation(conversation: Conversation) {
        Log.d(TAG, "Saving conversation: ${conversation.conversationID}")
        db.conversationDao().upsert(conversation.toEntity())
        updateConversationDates(conversation)
    }

    fun deleteConversation(conversationID: Long) {
        db.conversationDao().delete(conversationID)
        db.messageDao().deleteByConversation(conversationID)
        db.conversationDeletionDao().insert(
            ConversationDeletionEntity(conversationID = conversationID, timeStamp = Date().time)
        )
    }

    fun getDeletedConversations(userID: String): ArrayList<Long> =
        ArrayList(db.conversationDeletionDao().getAll().map { it.conversationID })

    fun getConversations(userID: String, searchString: String = "", limitResultsTo: Int = 50): List<Conversation> {
        val entities = when {
            searchString.isEmpty() && limitResultsTo > 0 ->
                db.conversationDao().getAllLimited(userID, limitResultsTo)
            searchString.isEmpty() ->
                db.conversationDao().getAll(userID)
            limitResultsTo > 0 ->
                db.conversationDao().searchLimited(userID, "%$searchString%", limitResultsTo)
            else ->
                db.conversationDao().search(userID, "%$searchString%")
        }
        return entities.map { it.toConversation() }
    }

    fun getConversation(conversationID: Long, userID: String): Conversation {
        val entity = db.conversationDao().get(conversationID, userID) ?: return Conversation()
        val conv = entity.toConversation()
        conv.saved = true
        return conv
    }

    fun updateConversationModified(conversation: Conversation) {
        conversation.dateModified = Date()
        updateConversationDates(conversation)
    }

    // ---------------------------------- Messages ------------------------------------------

    fun appendMessage(conversation: Conversation, chatMessage: ChatMessageExtended) {
        db.messageDao().insert(
            MessageEntity(chatMessage.conversationID, chatMessage.timeStamp, chatMessage.role, chatMessage.content)
        )
    }

    fun deleteMessage(message: ChatMessageExtended) {
        db.messageDao().delete(message.conversationID, message.timeStamp)
    }

    fun getMessages(conversationID: Long): ArrayList<ChatMessageExtended> =
        ArrayList(db.messageDao().getByConversation(conversationID).map {
            ChatMessageExtended(it.conversationID, it.role, it.content, it.timeStamp)
        })

    // ---------------------------------- Usage ------------------------------------------

    fun getConversationUsage(conversationID: Long): Int =
        db.usageDao().getByConversation(conversationID).sumOf { it.totalTokens }

    fun getUsage(userID: String, androidID: String): ArrayList<ChatUsage> =
        ArrayList(db.usageDao().getByUser(userID, androidID).map { it.toChatUsage() })

    fun getUsageLastUpdated(userID: String, androidID: String): Long =
        db.usageDao().getLastUpdated(userID, androidID) ?: 0L

    fun appendUsage(usage: ChatUsage) {
        db.usageDao().insert(
            UsageEntity(
                conversationID = usage.conversationID,
                promptTokens = usage.promptTokens,
                completionTokens = usage.completionTokens,
                totalTokens = usage.totalTokens,
                userID = usage.userID,
                androidID = usage.androidID,
                timeStamp = usage.timeStamp
            )
        )
    }

    // ---------------------------------- Note Categories ------------------------------------------

    private fun cacheNoteCategories() {
        noteCategories = ArrayList(db.noteCategoryDao().getAll().map { NoteCategory(it.categoryID, it.categoryName) })
        if (noteCategories.size < 2) {
            val defaults = defaultNoteCategories()
            for (c in defaults) { createNoteCategory(c.categoryName, c.categoryID) }
        }
    }

    fun getNoteCategoryMaxID(): Int {
        if (noteCategories.isEmpty()) cacheNoteCategories()
        return noteCategories.maxOfOrNull { it.categoryID } ?: 0
    }

    fun getNoteCategoryID(categoryName: String = ""): Int {
        if (noteCategories.isEmpty()) cacheNoteCategories()
        return noteCategories.firstOrNull { it.categoryName == categoryName }?.categoryID ?: -1
    }

    fun getNoteCategoryName(categoryID: Int): String {
        if (noteCategories.isEmpty()) cacheNoteCategories()
        return noteCategories.firstOrNull { it.categoryID == categoryID }?.categoryName ?: ""
    }

    fun getNoteCategories(includeAnyOption: Boolean = false): ArrayList<String> {
        if (noteCategories.isEmpty()) cacheNoteCategories()
        val result = ArrayList<String>()
        if (includeAnyOption) result.add("Any")
        noteCategories.forEach { result.add(it.categoryName) }
        return result
    }

    fun createNoteCategory(categoryName: String, categoryID: Int = -1) {
        val newID = if (categoryID < 0) getNoteCategoryMaxID() + 1 else categoryID
        try {
            db.noteCategoryDao().insertIfNotExists(NoteCategoryEntity(newID, categoryName))
            noteCategories.clear()
            Log.d(TAG, "Note category $categoryName created")
        } catch (e: Exception) {
            Log.w(TAG, "Note category creation failed: ${e.message}")
        }
    }

    fun deleteNoteCategory(categoryID: Int, replacementCategoryID: Int = 0) {
        db.noteDao().reassignCategory(categoryID, replacementCategoryID)
        db.noteCategoryDao().delete(categoryID)
        noteCategories.clear()
    }

    // ---------------------------------- Notes ------------------------------------------

    fun getNotes(categoryID: Int = -1, searchString: String = "", limitResultsTo: Int = 0, metaDataOnly: Boolean = false): ArrayList<NoteEntry> {
        val hasCategory = categoryID >= 0
        val hasSearch = searchString.isNotEmpty()
        val hasLimit = limitResultsTo > 0
        val search = "%$searchString%"

        val entities = when {
            hasCategory && hasSearch && hasLimit -> db.noteDao().searchInCategoryLimited(categoryID, search, limitResultsTo)
            hasCategory && hasSearch -> db.noteDao().searchInCategory(categoryID, search)
            hasCategory && hasLimit -> db.noteDao().getByCategoryLimited(categoryID, limitResultsTo)
            hasCategory -> db.noteDao().getByCategory(categoryID)
            hasSearch && hasLimit -> db.noteDao().searchLimited(search, limitResultsTo)
            hasSearch -> db.noteDao().search(search)
            hasLimit -> db.noteDao().getAllLimited(limitResultsTo)
            else -> db.noteDao().getAll()
        }

        return ArrayList(entities.map { entity ->
            val note = entity.toNoteEntry(getNoteCategoryName(entity.categoryID))
            if (metaDataOnly) { note.title = ""; note.content = "" }
            note
        })
    }

    fun saveNote(note: NoteEntry) {
        Log.d(TAG, "Saving note: ${note.noteID}")
        if (note.categoryName == "General") note.categoryID = 0
        db.noteDao().upsert(note.toEntity())
        updateNoteDates(note)
    }

    fun updateNoteDates(note: NoteEntry) {
        db.noteDao().updateDates(
            note.noteID,
            note.dateCreated.time,
            note.dateAccessed.time,
            note.dateModified.time
        )
    }

    fun getNote(noteID: Long): NoteEntry {
        val entity = db.noteDao().get(noteID) ?: return NoteEntry(noteID = Date().time)
        return entity.toNoteEntry(getNoteCategoryName(entity.categoryID))
    }

    fun deleteNote(noteID: Long) {
        db.noteDao().delete(noteID)
        db.noteDeletionDao().insert(NoteDeletionEntity(noteID = noteID, timeStamp = Date().time))
        Log.d(TAG, "Note $noteID deleted")
    }

    fun getDeletedNotes(): ArrayList<Long> =
        ArrayList(db.noteDeletionDao().getAll().map { it.noteID })

    fun getNotesLastUpdated(): Long {
        val notesMax = db.noteDao().getLastModified() ?: 0L
        val deletedMax = db.noteDeletionDao().getLastUpdated() ?: 0L
        return maxOf(notesMax, deletedMax)
    }

    // ---------------------------------- Sample Prompts ------------------------------------------

    fun getSamplePrompt(activityName: String, unused: Boolean = false): SamplePrompt {
        val entity = if (unused) {
            db.samplePromptDao().getUnused(activityName) ?: db.samplePromptDao().getRandom(activityName)
        } else {
            db.samplePromptDao().getRandom(activityName)
        }
        if (entity != null) {
            db.samplePromptDao().markUsed(activityName, entity.prompt)
            return SamplePrompt(entity.activityName, entity.prompt)
        }
        return SamplePrompt()
    }

    fun reuseSamplePrompts() = db.samplePromptDao().resetUsed()

    fun getSamplePrompts(cutoff: Long, unused: Boolean = false): ArrayList<SamplePrompt> {
        val entities = if (unused) db.samplePromptDao().getSinceUnused(cutoff)
                       else db.samplePromptDao().getSince(cutoff)
        return ArrayList(entities.map { SamplePrompt(it.activityName, it.prompt) })
    }

    fun cleanSamplePrompts() = db.samplePromptDao().removeDuplicates()

    fun appendSamplePrompt(timeStamp: Long, sample: SamplePrompt) {
        db.samplePromptDao().insert(
            SamplePromptEntity(activityName = sample.activityName, prompt = sample.prompt, used = 0, timeStamp = timeStamp)
        )
        Log.d(TAG, "Added sample prompt: ${sample.activityName}: ${sample.prompt}")
    }

    fun getSamplePromptsLastUpdated(): Long = db.samplePromptDao().getLastUpdated() ?: 0L

    fun getSamplePromptsCount(unused: Boolean = false): Long =
        if (unused) db.samplePromptDao().countUnused() else db.samplePromptDao().count()

    // ---------------------------------- Chat Modes ------------------------------------------

    fun getChatModes(): ArrayList<ChatActivityType> =
        ArrayList(db.chatModeDao().getAll().map {
            ChatActivityType(
                it.activityName, it.prompt,
                it.conversational != 0, it.clearConversationOnChange != 0,
                it.showLanguageOptions != 0, it.temperature
            )
        })

    fun clearChatModes() = db.chatModeDao().clearAll()

    fun appendChatMode(cm: ChatActivityType, sequence: Int, timeStamp: Long) {
        db.chatModeDao().insert(
            ChatModeEntity(
                activityName = cm.activityName, prompt = cm.prompt,
                conversational = if (cm.conversational) 1 else 0,
                clearConversationOnChange = if (cm.clearConversationOnChange) 1 else 0,
                showLanguageOptions = if (cm.showLanguageOptions) 1 else 0,
                temperature = cm.temperature, sequence = sequence, timeStamp = timeStamp
            )
        )
    }

    fun getChatModesLastUpdated(): Long = db.chatModeDao().getLastUpdated() ?: 0L

    // ---------------------------------- Prices ------------------------------------------

    fun getPricesLastUpdated(): Long = db.priceDao().getLastUpdated() ?: 0L

    fun savePriceEntry(entry: PriceWorkingSetEntry) = db.priceDao().upsert(entry.toEntity())

    fun getPricesWorkingSet(): ArrayList<PriceWorkingSetEntry> =
        ArrayList(db.priceDao().getAll().map { it.toPriceWorkingSetEntry() })

    fun clearPrices() = db.priceDao().clearAll()

    // ---------------------------------- Fixes ------------------------------------------

    fun applyChatDatabaseFixes(userID: String, androidID: String) {
        Log.d(TAG, "Applying database fixes...")
        db.conversationDao().fixEmptyUserIDs(userID)
        db.usageDao().fixEmptyUserIDs(userID)
        if (androidID.isNotEmpty()) db.usageDao().fixEmptyAndroidIDs(androidID)
        Log.d(TAG, "Database fixes complete")
    }
}

// ---------------------------------- Entity <-> DataClass conversions ------------------------------------------

private fun ConversationEntity.toConversation() = Conversation(
    conversationID = conversationID,
    title = title,
    summary = summary,
    saved = saved != 0,
    userID = userID,
    dateCreated = Date(dateCreated),
    dateAccessed = Date(dateAccessed),
    dateModified = Date(dateModified)
)

private fun Conversation.toEntity() = ConversationEntity(
    conversationID = conversationID,
    dateCreated = dateCreated.time,
    dateAccessed = dateAccessed.time,
    dateModified = dateModified.time,
    saved = if (saved) 1 else 0,
    title = title,
    summary = summary,
    userID = userID,
    firebaseID = ""
)

private fun UsageEntity.toChatUsage() = ChatUsage(
    conversationID = conversationID,
    promptTokens = promptTokens,
    completionTokens = completionTokens,
    totalTokens = totalTokens,
    userID = userID,
    androidID = androidID,
    timeStamp = timeStamp
)

private fun NoteEntity.toNoteEntry(categoryName: String) = NoteEntry(
    noteID = noteID,
    categoryID = categoryID,
    categoryName = categoryName,
    title = title,
    content = content,
    dateCreated = Date(dateCreated),
    dateAccessed = Date(dateAccessed),
    dateModified = Date(dateModified)
)

private fun NoteEntry.toEntity() = NoteEntity(
    noteID = noteID,
    categoryID = categoryID,
    title = title,
    content = content,
    dateCreated = dateCreated.time,
    dateAccessed = dateAccessed.time,
    dateModified = dateModified.time
)

private fun PriceEntity.toPriceWorkingSetEntry() = PriceWorkingSetEntry(
    ticker = ticker, companyName = companyName, sector = sector,
    sp500Listed = sp500Listed != 0, currentPrice = currentPrice,
    average5Day = average5Day, average2Day = average2Day,
    pc2Year = pc2Year, pc1Year = pc1Year, pc6Month = pc6Month,
    pc3Month = pc3Month, pc2Month = pc2Month, pc1Month = pc1Month, pc1Day = pc1Day,
    gainMonthly = gainMonthly, lossStd1Year = lossStd1Year, pointValue = pointValue,
    targetHoldings = targetHoldings, revenue = revenue, netIncome = netIncome,
    companySize = companySize, marketCap = marketCap, operatingExpense = operatingExpense,
    netProfitMargin = netProfitMargin, earningsPerShare = earningsPerShare,
    cashShortTermInvestments = cashShortTermInvestments, totalAssets = totalAssets,
    totalLiabilities = totalLiabilities, netWorth = netWorth, totalEquity = totalEquity,
    sharesOutstanding = sharesOutstanding, priceToBook = priceToBook,
    returnOnAssets = returnOnAssets, returnOnCapital = returnOnCapital,
    cashFromOperations = cashFromOperations, cashFromInvesting = cashFromInvesting,
    cashFromFinancing = cashFromFinancing, netChangeInCash = netChangeInCash,
    freeCashFlow = freeCashFlow, comments = comments, latestEntry = Date(latestEntry)
)

private fun PriceWorkingSetEntry.toEntity() = PriceEntity(
    ticker = ticker, companyName = companyName, sector = sector,
    sp500Listed = if (sp500Listed) 1 else 0, currentPrice = currentPrice,
    average5Day = average5Day, average2Day = average2Day,
    pc2Year = pc2Year, pc1Year = pc1Year, pc6Month = pc6Month,
    pc3Month = pc3Month, pc2Month = pc2Month, pc1Month = pc1Month, pc1Day = pc1Day,
    gainMonthly = gainMonthly, lossStd1Year = lossStd1Year, pointValue = pointValue,
    targetHoldings = targetHoldings, revenue = revenue, netIncome = netIncome,
    companySize = companySize, marketCap = marketCap, operatingExpense = operatingExpense,
    netProfitMargin = netProfitMargin, earningsPerShare = earningsPerShare,
    cashShortTermInvestments = cashShortTermInvestments, totalAssets = totalAssets,
    totalLiabilities = totalLiabilities, netWorth = netWorth, totalEquity = totalEquity,
    sharesOutstanding = sharesOutstanding, priceToBook = priceToBook,
    returnOnAssets = returnOnAssets, returnOnCapital = returnOnCapital,
    cashFromOperations = cashFromOperations, cashFromInvesting = cashFromInvesting,
    cashFromFinancing = cashFromFinancing, netChangeInCash = netChangeInCash,
    freeCashFlow = freeCashFlow, comments = comments, latestEntry = latestEntry.time
)
