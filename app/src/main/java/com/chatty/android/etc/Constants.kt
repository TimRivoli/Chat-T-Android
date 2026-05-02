package com.chatty.android.etc
const val GOOGLE_ENDPOINT = "https://www.google.com"
const val OPENAI_API_ENDPOINT = "https://api.openai.com"
const val OPENAI_COMPLETIONS_ENDPOINT = "https://api.openai.com/v1/chat/completions"

const val googleClientID = "YOUR_GOOGLE_CLIENTID"
const val FORCE_GOOGLE_AUTH = false  // Debug: set to false to allow anonymous auth based on device settings
const val ConversationTableName = "conversations"
const val ConversationDeletionsTableName = "conversations_deleted"
const val MessageTableName = "messages"
const val UsageTableName = "usage"
const val ActivatedDevicesTableName = "devices_activated"
const val RegistrationTableName = "devices_registration"
const val SamplePromptTableName = "sample_prompts"
const val ChatModesTableName = "chat_modes"
const val NotesTableName = "notes"
const val NotesCategoryTableName = "notes_categories"
const val NotesDeletionsTableName = "notes_deleted"
const val PricesWorkingSetTableName = "prices_working_set"

//If you want to use a fixed username and password instead of OAuth or anonymous
const val firebaseUserID=""
const val firebasePwd=""
