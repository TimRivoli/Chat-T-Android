package com.chatty.android.etc

import android.util.Log
import java.util.Date
import kotlin.collections.ArrayList
import com.chatty.android.etc.DataClasses.*

object ChatManager {
	private data class ChatResult(var content: String, var totalTokens: Int)
	private const val TAG = "ChatManager"
	private const val defaultModel: String = "gpt-3.5-turbo"
	private const val enhancedModel: String = "gpt-4"
	private var currentModel = defaultModel
	private var conversationTokenCount: Int=0
	private lateinit var chatModes: ArrayList<ChatActivityType>
	var conversation = Conversation()
	var messages_extended = ArrayList<ChatMessageExtended>()
	var chatUsage = ArrayList<ChatUsage>()
	var conversationList = ArrayList<Conversation>()

	operator fun invoke(): ChatManager {
		chatModes = StorageManager.getChatModes()
		Log.d(TAG,"Invoking ChatManager")
		return ChatManager
	}
	fun ToggleEngine(): String {
		if (currentModel == defaultModel){
			currentModel = enhancedModel
		} else {
			currentModel = defaultModel
		}
		return currentModel

	}

// ------------------------------------- Utility --------------------------------------
	fun formatPromptPrettyLike(s1: String): String {
		var formattedString = s1.trim().capitalize()
		if ("what" in formattedString || "who" in formattedString || "why" in formattedString || "where" in formattedString) {
			if (!formattedString.endsWith("?")) {
				formattedString += "?"
			}
		}
		return formattedString
	}

	fun formatHTMLMarkup(text:String):String{
		val HTMLStartTag = "<!DOCTYPE html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'>\n" +
				"<style> body {font-size: 8px; font-family: sans-serif;  } div { background-color: black; color: white;} </style>\n</head><body>"
		val HTMLEndTag = "</body> </html>"
		val ticks = "\n```"
		var result = text.replace("\n", "<BR>")
		while (result.contains(ticks)) {
			result = result.replaceFirst(ticks, "<pre><div>")
			result = result.replaceFirst(ticks, "</div></pre>")
		}
		return HTMLStartTag + result + HTMLEndTag
	}

	fun getTokenCount(text: String): Int {
		var token_count = 0
		//TODO("use a tokenizer to calculate more accurately")
		if (text.isNotEmpty()) {
			val words = text.split("\\s+".toRegex())
			token_count = words.filter { it.isNotEmpty() }.size
			Log.d(TAG, " The text contains $token_count tokens.")
		}
		return token_count
	}

	// ------------------------------------- Conversation management --------------------------------------
	fun  getConversations(){
		conversationList = StorageManager.getConversationList()
	}

	fun clearConversation() {
		conversation.conversationID = Date().time
		conversation.title = ""
		conversation.summary = ""
		conversation.dateCreated  = Date()
		conversation.dateModified = Date()
		conversation.dateAccessed = Date()
		conversation.saved = false
		messages_extended.clear()
		conversationTokenCount = 0
		Log.d(TAG, " Chat conversation cleared.")
	}

	suspend fun saveConversation() {
		conversationMakeTitle() //This only gets called once so title should be empty
		StorageManager.saveConversation(conversation, messages_extended)
	}

	fun loadConversation(conversationID: Long) {
		Log.d(TAG, "Asked to load conversation: " + conversationID)
		conversation = StorageManager.getConversation(conversationID)
		if (conversation.conversationID != conversationID){
			Log.d(TAG, "Could not locate: " + conversationID)
			clearConversation()
		} else {
			Log.d(TAG, "Found: " + conversation.conversationID)
			Log.d(TAG, "Title: "  + conversation.title)
			messages_extended.clear()
			val x = StorageManager.getMessages(conversation.conversationID)
			for (m in x) {
				messages_extended.add(m)
			}
			conversationTokenCount = StorageManager.getConversationUsage(conversationID)
			Log.d(TAG, "Messages: " + messages_extended.size.toString())
		}
		Log.d(TAG, "Chat conversation load completed." )
	}

	fun deleteConversation(conversationID: Long) {
		StorageManager.deleteConversation(conversationID)
	}

	fun deleteMessage(message: ChatMessageExtended) {
		messages_extended.remove(message)
		StorageManager.deleteMessage(message)
	}

	private fun appendConversation(content: String, tokenCount: Int, isResponse: Boolean) {
		if (content.isNotEmpty()) {
			val m = ChatMessageExtended(conversation.conversationID,"", content)
			if (isResponse) m.role = "assistant" else m.role = "user"
			messages_extended.add(m)
			if (conversation.saved) {
				StorageManager.appendMessage(conversation, m)
			}
		}
	}

	private suspend fun _executeQuery(messages: List<ChatMessage>, temperature: Double=0.2, model: String=defaultModel, isSystem:Boolean=false): ChatResult {
		var result = ChatResult("",0)
		val chatCompletion: ChatCompletion = WebManager.callChatCompletionAPI(messages, temperature, model)
		val message = chatCompletion.choices[0].message
		if (chatCompletion.choices[0].finish_reason == "Error") {
			result.content = message.content
			if (result.content.startsWith("Socket timeout", true)) {
				Log.e(TAG, result.content)
				result.content = "Request timed out."
			}
		} else {
			val usage = chatCompletion.usage
			var u = ChatUsage(conversation.conversationID, usage.prompt_tokens, usage.completion_tokens, usage.total_tokens)
			if (isSystem) { u.conversationID = -100 }
			chatUsage.add(u)
			StorageManager.appendUsage(u)
			result.content = message.content
			result.totalTokens=usage.total_tokens
		}
		Log.i(TAG, chatCompletion.toString())

		return result
	}

	suspend fun chatbotQuery(question: String, chatActivityMode: ChatActivityType, languageOption:ChatLanguageOption, user_instructions: String =""):String {
		var result:String = ""
		if (StorageManager.API_KEY=="") {
			result = "Device awaiting activation..."
		} else if (question !="") {
			val messages = ArrayList<ChatMessage>()
			var system_message: String
			system_message = chatActivityMode.prompt
			if (chatActivityMode.showLanguageOptions) {system_message += " " + languageOption}
			val user_tokens:Int = getTokenCount(user_instructions + ". " + question)
			if (user_instructions !="") { system_message = user_instructions + ". " + system_message }
			//val system_tokens = getTokenCount(system_message)
			messages.add(ChatMessage("system", system_message))
			if (chatActivityMode.conversational) {
				if (conversationTokenCount > 750 && messages_extended.size > 2){conversationSummarize()}
				if (conversation.summary !="") {messages.add(ChatMessage("system", "Conversation summary: " + conversation.summary))}
				for (m in messages_extended) {
					if (m.role == "user"){
						messages.add(ChatMessage(m.role, m.content))
					} else {
						messages.add(ChatMessage(m.role, m.content.take(100))) //Really tends to confuse it, as does the summary
					}
				}
			}
			messages.add(ChatMessage("user", question))
			appendConversation(question, user_tokens, false)
			val queryResult = _executeQuery(messages,  chatActivityMode.temperature,  currentModel, false)
			appendConversation(content = queryResult.content, tokenCount = queryResult.totalTokens, isResponse = true)
			result = queryResult.content
		}
		return result
	}

	private suspend fun conversationSummarize() {
		Log.d(TAG, " Summarizing conversation...")
		val messages = ArrayList<ChatMessage>()
		messages.add(ChatMessage("system","You will be give a conversation, your task is to summarize it making sure include information provided by the user "))
		if (conversation.summary !="") { messages.add(ChatMessage("user", "Please generate a concise summary of this conversation.  Make sure to include nformation provided by the user: " + conversation.summary))		}
		for (m in messages_extended){
			messages.add(ChatMessage(m.role, m.content))
		}
		conversationTokenCount -= getTokenCount(conversation.summary)
		val result = _executeQuery(messages, 0.0, defaultModel, true )
		conversation.summary = result.content
		conversationTokenCount += result.totalTokens
	}

	private suspend fun conversationMakeTitle() {
		Log.d(TAG, " Making conversation title ...")
		if (conversation.summary =="") { conversationSummarize()}
		val messages = ArrayList<ChatMessage>()
		messages.add(ChatMessage("user", "Provide a concise 5 word topic for this conversation:" + conversation.summary))
		val result = _executeQuery(messages, 0.2, defaultModel, true )
		conversation.title = result.content
	}
	suspend fun generateSamplePrompts() {
		Log.d(TAG, "Generating sample prompts ...")
		for (i in 0 until 10) {
			val messages = ArrayList<ChatMessage>()
			messages.add(ChatMessage("user", "Sample prompt."))
			val result = _executeQuery(messages, 0.2, defaultModel, true )
			StorageManager.appendSamplePrompt(Date().time,	SamplePrompt("Conversation",result.content)
			)
		}
	}
}
