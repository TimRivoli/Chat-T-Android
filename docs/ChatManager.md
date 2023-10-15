# ChatManager

The `ChatManager` is responsible for managing interactions with the chatbot model, handling conversations, messages, and usage tracking. It interfaces with the `WebManager` for API calls, and `StorageManager` for data storage operations.

## Table of Contents
- [Classes](#classes)
- [Fields](#fields)
- [Methods](#methods)
- [Usage](#usage)
- [Examples](#examples)

---

## Classes

- `ChatResult`: A data class that holds the result of a chat interaction, including the content and total token count.

---

## Fields

- `TAG`: Constant String for logging purposes.
- `defaultModel`: Default model used for chat interactions.
- `enhancedModel`: Enhanced model for higher performance.
- `currentModel`: The currently active chat model.
- `conversationTokenCount`: Total token count in the conversation.
- `chatModes`: List of available chat activity modes.
- `conversation`: Current conversation object.
- `messages_extended`: List of extended chat messages.
- `chatUsage`: List of chat usage statistics.
- `conversationList`: List of saved conversations.

---

## Methods

- `ToggleEngine()`: Toggles between the default and enhanced chat models.
- `formatPromptPrettyLike(s1: String)`: Formats a prompt string to be more user-friendly.
- `formatHTMLMarkup(text: String)`: Formats text for HTML display.
- `getTokenCount(text: String)`: Counts the tokens in a text.
- `getConversations()`: Loads the list of conversations.
- `clearConversation()`: Clears the current conversation.
- `saveConversation()`: Saves the current conversation to storage.
- `loadConversation(conversationID: Long)`: Loads a conversation by its ID.
- `deleteConversation(conversationID: Long)`: Deletes a conversation by its ID.
- `deleteMessage(message: ChatMessageExtended)`: Deletes a message.
- `_executeQuery(messages: List<ChatMessage>, temperature: Double, model: String, isSystem: Boolean)`: Executes a chat query.
- `chatbotQuery(question: String, chatActivityMode: ChatActivityType, languageOption: ChatLanguageOption, user_instructions: String)`: Queries the chatbot with a user question.
- `conversationSummarize()`: Summarizes the conversation.
- `conversationMakeTitle()`: Generates a title for the conversation.
- `generateSamplePrompts()`: Generates sample prompts for the chatbot.

---

## Usage

1. Initialize the `ChatManager` by invoking it.
2. Use the methods to manage conversations, messages, and interact with the chatbot.

---

## Examples

```kotlin
// Initialize ChatManager
val chatManager = ChatManager()

// Toggle between chat models
val currentModel = chatManager.ToggleEngine()

// Format a prompt
val formattedPrompt = chatManager.formatPromptPrettyLike("what is the weather?")

// Get token count
val tokenCount = chatManager.getTokenCount("How are you?")

// Load conversation by ID
chatManager.loadConversation(123456789)

// Query the chatbot
val response = chatManager.chatbotQuery("Tell me a joke", ChatActivityType.GENERAL, ChatLanguageOption.ENGLISH)
```

For more detailed usage examples, refer to the code comments and method explanations above.