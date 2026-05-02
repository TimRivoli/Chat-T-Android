package com.chatty.android

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.chatty.android.etc.ChatManager
import com.chatty.android.etc.SpeechToTextManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.chatty.android.etc.DataClasses.*
import com.chatty.android.etc.StorageManager
import java.util.Date
import com.chatty.android.etc.TextToSpeechManager
import com.chatty.android.etc.TextUtility
import com.chatty.android.ui.ChatMessageRVAdapter
import com.chatty.android.ui.ChatMessageRVAdapterSwipeCallBack

class ChatActivity: AppCompatActivity(), ChatMessageRVAdapter.SwipeListener {
    private val TAG = "ChatActivity"
    private lateinit var btnSend: ImageButton
    private lateinit var btnOptions: ImageButton
    private lateinit var btnSave: ImageButton
    private lateinit var btnClear: FloatingActionButton
    private lateinit var btnMicrophone: ImageButton
    private lateinit var btnHamburger: ImageButton
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var txtUserInput: TextView
    private lateinit var progBar: ProgressBar
    private lateinit var spinnerChatMode: Spinner
    private lateinit var chatActivities: ArrayList<ChatActivityType>
    private lateinit var chatActivityType: ChatActivityType
    private lateinit var spinnerLanguages: Spinner
    private lateinit var recyclerView: RecyclerView
    private lateinit var scrollUp: ImageButton
    private lateinit var scrollDown: ImageButton
    private lateinit var rvAdapter: ChatMessageRVAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var chatSampleConstraint: ConstraintLayout
    private lateinit var chatSampleItem: TextView
    private var autoSubmitEnabled = false
    private var autoSubmitWaitTime = 2000 //Millis after text changed before auto-submit
    private val autoSubmitTypingWaitTime = 7000 //Little bit longer if the user is fiddling with the keyboard
    private var autoSubmitCountDown = autoSubmitWaitTime
    private var autoSubmitRunning = false
    private var suppressSpinerConversationClear = false
    private var textToSpeak : String = ""
    private var speakingEnabled: Boolean = TextToSpeechManager.speechEnabled
    private var processing: Boolean = false
    private var firstVisibleItemPosition :Int = 0
    private var lastVisibleItemPosition :Int = 0
    private var totalItemCount :Int = 0

    override fun onStart() {
        Log.d(TAG,"onStart")
        super.onStart()
    }

    override fun onResume() {
        Log.d(TAG,"onResume")
        super.onResume()
        if (StorageManager.API_KEY == "") {
            chatSampleConstraint.visibility = View.VISIBLE
            chatSampleItem.text = getString(R.string.status_device_not_activated)
            chatSampleItem.setOnClickListener(null)
        }
    }

    override fun onPause() {
        Log.d(TAG,"onPause")
        super.onPause()
    }

    override fun onStop() {
        Log.d(TAG,"onStop")
        super.onStop()
    }

    override fun onDestroy() {
        Log.d(TAG,"onDestroy")
        super.onDestroy()
    }

    override fun onItemSwiped(position: Int) {
        val message = ChatManager.messages_extended[position]
        ChatManager.deleteMessage(message)
        Log.d(TAG, "Item at position $position has been swiped.")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        Log.d(TAG, "onSaveInstanceState")
        super.onSaveInstanceState(outState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG,"onCreate ChatActivity")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.chat_activity)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.chatTextUserInputconstraint)) { view, insets ->
            val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            view.setPadding(0, 0, 0, maxOf(navBar.bottom, ime.bottom))
            insets
        }
        //if (savedInstanceState != null) {     }
        autoSubmitEnabled = StorageManager.getAutoSubmitPref()
        speakingEnabled = StorageManager.getSpeakOutputPref()
        chatActivities = StorageManager.getChatModes()
        chatActivityType = chatActivities[0]
        TextToSpeechManager.startup(this)
        txtUserInput = findViewById(R.id.chatTextUserInput)
        btnSend = findViewById(R.id.btnSubmit)
        btnOptions = findViewById(R.id.chat_button_options)
        btnSave = findViewById(R.id.chatButtonSave)
        btnClear = findViewById(R.id.fabNewChat)
        btnHamburger = findViewById(R.id.btnHamburger)
        drawerLayout = findViewById(R.id.drawerLayout)
        navView = findViewById(R.id.navView)
        progBar = findViewById(R.id.chatProgressBar)
        chatSampleConstraint = findViewById(R.id.chatSampleConstraint)
        chatSampleItem = findViewById(R.id.chatSampleItem)

        //RecyclerView bits and bobs
        val layoutManager = LinearLayoutManager(this)
        recyclerView = findViewById(R.id.chatRecyclerView)
        scrollUp = findViewById(R.id.rvScrollUp)
        scrollDown = findViewById(R.id.rvScrollDown)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener {
            Log.d(TAG, "Swipe refreshed, nothing coded to do")
            swipeRefreshLayout.isRefreshing = false
        }
        recyclerView.layoutManager = layoutManager
        rvAdapter = ChatMessageRVAdapter(this, ChatManager.messages_extended, this::onChatItemClick)
        rvAdapter.swipeListener = this
        recyclerView.adapter = rvAdapter
        recyclerView.postDelayed({
            recyclerView.scrollToPosition(rvAdapter.itemCount - 1)
        }, 100)
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                totalItemCount = layoutManager.itemCount
                scrollDown.visibility = if (lastVisibleItemPosition < totalItemCount - 1) View.VISIBLE else View.GONE
                scrollUp.visibility = if (firstVisibleItemPosition > 0) View.VISIBLE else View.GONE
                if (dy > 0) btnClear.hide() else if (dy < 0) btnClear.show()
            }
        })
        scrollDown.setOnClickListener {
            scrollToBottom()
        }
        scrollUp.setOnClickListener {
            scrollToTop()
        }
        val itemTouchHelper = ItemTouchHelper(ChatMessageRVAdapterSwipeCallBack(rvAdapter, this))
        itemTouchHelper.attachToRecyclerView(recyclerView)

        //Other listeners
        btnOptions.setOnClickListener { view -> showPopupMenu(view) }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Log.d(TAG, "onBackPressed via callback")
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else if (ChatManager.messages_extended.isEmpty()) {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                } else {
                    clearConversation()
                }
            }
        })
        //btnSpeak.setOnClickListener {            toggleSpeakingEnabled()        }
        //if (speakingEnabled) {toggleSpeakingEnabled()}  //default is off so turn on
        btnSave.visibility = View.GONE
        btnSave.setOnClickListener {
            lifecycleScope.launch {
                saveConversation()
            }
        }
        btnClear.setOnClickListener {
            lifecycleScope.launch {
                clearConversation()
            }
        }
        btnHamburger.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
        navView.setNavigationItemSelectedListener { item ->
            drawerLayout.closeDrawer(GravityCompat.START)
            when (item.itemId) {
                R.id.nav_home -> startActivity(Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                })
                R.id.nav_conversations -> {
                    stopSpeaking()
                    ChatManager.getConversations()
                    if (ChatManager.conversationList.size > 0) {
                        startConversationListActivity()
                    }
                }
                R.id.nav_notes -> startActivity(Intent(this, NoteListActivity::class.java))
                R.id.nav_usage -> startActivity(Intent(this, UsageActivity::class.java))
                R.id.nav_prices -> startActivity(Intent(this, StockListActivity::class.java))
                R.id.nav_settings -> startActivity(Intent(this, SettingsActivity::class.java))
            }
            true
        }
        if (intent.getBooleanExtra("openConversationList", false)) {
            ChatManager.getConversations()
            if (ChatManager.conversationList.size > 0) {
                startConversationListActivity()
            }
        }
        val p = StorageManager.getSamplePrompt("Conversation")
        if (p.prompt =="") {
            chatSampleItem.visibility = View.GONE
        } else {
            chatSampleItem.visibility = View.VISIBLE
            chatSampleItem.setText(p.prompt)
        }
        chatSampleItem.setOnClickListener {
            txtUserInput.setText(chatSampleItem.text)
        }

        btnSend.setOnClickListener {executeChatRequest() }
        btnSend.setOnLongClickListener {
            toggleChatEngine()
            true
        }
        btnMicrophone = findViewById(R.id.btnMicrophone)
        btnMicrophone.setOnClickListener {
            stopSpeaking()
            if (TextToSpeechManager.isSpeaking) {
                notifyUser("Stopping speech...")
            } else {
                if (SpeechToTextManager.isPermissionGranted(this)) {
                    SpeechToTextManager.startSpeechToText(this, txtUserInput)
                }
            }
        }
        txtUserInput.setOnKeyListener { _, keyCode, event ->
            //Log.i(TAG, "Reseeting autoSubmitCountDown")
            //autoSubmitCountDown = autoSubmitTypingWaitTime
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                executeChatRequest()
                return@setOnKeyListener true
            }
            false
        }
        txtUserInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }
            override fun afterTextChanged(s: Editable?) {
                if (s != null && autoSubmitEnabled) {
                    if (!autoSubmitRunning && s.length > 1) { //Ensure only one coroutine
                        autoSubmitRunning = true
                        autoSubmitCountDown = autoSubmitWaitTime    //Input was changed so start over
                        Log.d(TAG, "Starting auto-submit coroutine.")
                        lifecycleScope.launch(Dispatchers.Default) {
                            while (autoSubmitCountDown > 0 && !processing) {
                                val priorLength = s.length
                                delay(200)
                                if (priorLength == s.length) {
                                    autoSubmitCountDown -= 200
                                } else {
                                    autoSubmitCountDown = autoSubmitTypingWaitTime  //backspace and soft keyboard aren't captured by keypress
                                }
                            }
                            if (!processing) { withContext(Dispatchers.Main) { executeChatRequest() } }
                            autoSubmitCountDown = autoSubmitWaitTime
                            autoSubmitRunning = false
                        }
                    }
                }
            }
        })

        //Spinner setup
        var chatList = ArrayList<String>()
        for (m in chatActivities) {chatList.add(m.activityName)}
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, chatList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerChatMode = findViewById(R.id.chatSpinnerMode)
        spinnerChatMode.adapter = adapter
        val adapter2 = ArrayAdapter(this, android.R.layout.simple_spinner_item, ChatLanguageOption.entries)
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLanguages = findViewById(R.id.chatSpinnerLanguage)
        spinnerLanguages.visibility = View.GONE
        spinnerLanguages.adapter = adapter2
        spinnerChatMode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>, selectedItemView: View?, position: Int, id: Long) {
                val selectedValue = parentView.getItemAtPosition(position)
                if (selectedValue != chatActivityType.activityName  ) {
                    Log.d(TAG, "Changing activity type to $selectedValue")
                    for (x in chatActivities) {
                        if (x.activityName == selectedValue) {chatActivityType = x}
                    }
                    if (!processing && !suppressSpinerConversationClear ) {clearConversation() }
                    if (chatActivityType.showLanguageOptions) {
                        spinnerLanguages.visibility = View.VISIBLE
                    } else {
                        spinnerLanguages.visibility = View.GONE
                    }
                    suppressSpinerConversationClear = false
                }
            }

            override fun onNothingSelected(parentView: AdapterView<*>) {
                spinnerLanguages.visibility = View.GONE
            }
        }
        spinnerLanguages.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>, selectedItemView: View?, position: Int, id: Long) {
                val selectedValue = parentView.getItemAtPosition(position) as ChatLanguageOption
                if (TextToSpeechManager.speechEnabled){
                    TextToSpeechManager.SetLanguage(selectedValue)
                    StorageManager.saveLanguagePref(selectedValue.toString())
                }
            }
            override fun onNothingSelected(parentView: AdapterView<*>) {
            }
        }
        val lang = ChatLanguageOption.fromString(StorageManager.getLanguagePref())
        if (lang != null) {
            spinnerLanguages.setSelection(lang.ordinal)
        }
//        handleIntent()        //Check for passed intents
        startProcessing(false)
    }

//    private fun handleIntent() {
//        Log.d(TAG, "handleIntent")
//        val conversationID = intent.getLongExtra("conversationID", 0)
//        if (conversationID > 0) {
//            Log.d(TAG, "Loading from intent conversationID: $conversationID")
//            startProcessing(true)
//            stopSpeaking()
//            spinnerChatMode.setSelection(0)
//            loadConversation(conversationID)
//            startProcessing(false)
//        }
//    }

    private val conversationListResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        Log.d(TAG, "conversationListResultLauncher")
        if (result.resultCode == RESULT_OK) {
            val data: Intent? = result.data
            val conversationID = data?.getLongExtra("conversationID", 0)
            if (conversationID != null) {
                when {
                    conversationID > 0 -> {
                        Log.d(TAG, "Loading from conversationListResultLauncher conversationID: $conversationID")
                        suppressSpinerConversationClear = true
                        spinnerChatMode.setSelection(0)
                        loadConversation(conversationID)
                    }
                    conversationID == -1L -> {
                        Log.d(TAG, "New conversation requested from conversation list")
                        clearConversation()
                    }
                }
            }
        } else {
            Log.d(TAG, "conversationList was canceled")
        }
    }

    fun startConversationListActivity() {
        val intent = Intent(this, ConversationListActivity::class.java)
        conversationListResultLauncher.launch(intent)
    }

    private fun scrollToTop() {
        if (::rvAdapter.isInitialized) {recyclerView.smoothScrollToPosition(0) }
    }
    private fun scrollToBottom(){
        if (::rvAdapter.isInitialized) {
            if (rvAdapter.getItemCount() > 1) {
                recyclerView.smoothScrollToPosition(rvAdapter.getItemCount()-1)
            }
        }
    }
    fun notifyUser(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private  fun startProcessing(start:Boolean){
        //Log.d(TAG, "startProcessing $start")
        if (start) {
            processing = true
            btnClear.isEnabled = false
            btnSend.isEnabled = false
            recyclerView.isEnabled = false
            btnSend.visibility = View.INVISIBLE
            progBar.visibility = View.VISIBLE
            txtUserInput.isEnabled = false
        } else {
            btnSend.visibility = View.VISIBLE
            progBar.visibility = View.INVISIBLE
            if (ChatManager.messages_extended.size > 0) {btnSave.visibility = View.VISIBLE}
            recyclerView.isEnabled = true
            recyclerView.isSelected = false
            btnClear.isEnabled = true
            btnSend.isEnabled = true
            txtUserInput.isEnabled = true
            txtUserInput.isActivated = true
            processing = false
        }
        if (rvAdapter.getItemCount() > 0) {
            chatSampleConstraint.visibility = View.GONE
        } else {
            chatSampleConstraint.visibility = View.VISIBLE
        }
    }

    private fun notifyDataChanged(){
        recyclerView.adapter?.notifyDataSetChanged()

    }
    private fun executeChatRequest(){
        if (StorageManager.API_KEY == "") {
            showActivationDialog()
            return
        }
        var question:String = txtUserInput.text.toString()
        question = question.replace("\n", "")
        if (question !="" && !processing){
            lifecycleScope.launch {
                try {
                    startProcessing(true)
                    val selectedLanguage = spinnerLanguages.selectedItem as ChatLanguageOption
                    question = TextUtility.formatPromptPrettyLike(question)
                    textToSpeak = ChatManager.chatbotQuery(question, chatActivityType, selectedLanguage, "")
                    notifyDataChanged()
                    speakText()
                    txtUserInput.text = ""
                    scrollToBottom()
                } catch (e: Exception) {
                    Log.e(TAG, "Chat request failed: $e")
                    ChatManager.messages_extended.add(
                        ChatMessageExtended(
                            ChatManager.conversation.conversationID, "assistant", "Error: ${e.message}"
                        )
                    )
                    notifyDataChanged()
                    scrollToBottom()
                } finally {
                    startProcessing(false)
                }
            }
        }
    }
    private fun showActivationDialog() {
        val activationKey = StorageManager.getActivationKey()
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_title_device_not_activated)
            .setMessage(getString(R.string.dialog_message_device_not_activated, activationKey))
            .setPositiveButton(R.string.dialog_button_copy_key) { _, _ ->
                val clipboard = getSystemService(ClipboardManager::class.java)
                clipboard.setPrimaryClip(ClipData.newPlainText("Activation Key", activationKey))
                notifyUser("Activation key copied to clipboard")
            }
            .setNegativeButton(R.string.dialog_button_dismiss, null)
            .show()
    }

    private fun clearConversation(){
        if (!processing){
            Log.d(TAG, "Clearing conversation...")
            startProcessing(true)
            btnSave.visibility = View.GONE
            ChatManager.clearConversation()
            notifyDataChanged()
            stopSpeaking()
            setSaveButtonState()
            val p = StorageManager.getSamplePrompt(spinnerChatMode.selectedItem.toString())
            if (p.prompt =="") {
                chatSampleItem.visibility = View.GONE
            } else {
                chatSampleItem.visibility = View.VISIBLE
                chatSampleItem.setText(p.prompt)
            }
            startProcessing(false)
        }
    }
    private fun setSaveButtonState() {
        val drawableSave = resources.getDrawable(R.drawable.download, null)
        val drawableSaved = resources.getDrawable(R.drawable.checked, null)
        if (ChatManager.conversation.saved) {
            btnSave.setImageDrawable(drawableSaved)
            btnSave.isEnabled = false
        } else {
            btnSave.setImageDrawable(drawableSave)
            btnSave.isEnabled = true
        }
    }
    private fun loadConversation(conversationID: Long){
        if (!processing){
            stopSpeaking()
            lifecycleScope.launch {
                startProcessing(true)
                ChatManager.loadConversation(conversationID)
                notifyDataChanged()
                setSaveButtonState()
                scrollToBottom()
                TextToSpeechManager.stopRequested = false
                startProcessing(false)
            }
        }
    }

    private suspend fun saveConversation(){
        if (!processing && ChatManager.messages_extended.size > 0){
            startProcessing(true)
            if (!ChatManager.conversation.saved) {
                ChatManager.saveConversation()
            }
            setSaveButtonState()
            startProcessing(false)
        }
    }
    private fun toggleChatEngine(){
        if (StorageManager.subscriptionLevel > 1) {
            val text = ChatManager.ToggleEngine()
            notifyUser("Model set to: $text")
        }
    }
//
//    private fun toggleSpeakingEnabled(){
//        val drawableEnabled = resources.getDrawable(R.drawable.sound_up, null)
//        val drawableDisabled = resources.getDrawable(R.drawable.sound_off, null)
//        textToSpeak = ""
//        if (TextToSpeechManager.speechEnabled){
//            if (TextToSpeechManager.isSpeaking) { stopSpeaking() }
//            speakingEnabled = !speakingEnabled
//            Log.d(TAG, "toggleSpeakingEnabled to $speakingEnabled")
//            if (speakingEnabled) {
//                //btnSpeak.setImageDrawable(drawableEnabled)
//            } else {
//               // btnSpeak.setImageDrawable(drawableDisabled)
//            }
//        } else {
//           // btnSpeak.setImageDrawable(drawableDisabled)
//          //  btnSpeak.isEnabled = false
//            speakingEnabled = false
//        }
//    }

    private fun toggleSpeakingPause() {
        if (speakingEnabled) {
            Log.d(TAG, "toggleSpeakingPause ${TextToSpeechManager.isSpeaking}")
            if (TextToSpeechManager.isSpeaking){
                TextToSpeechManager.pauseRequested = true
                notifyUser("Speech pausing...")
            } else {
                TextToSpeechManager.pauseRequested = false
            }
        }
    }
    private suspend fun speakText(){
        Log.d(TAG, "speakText")
        if (speakingEnabled && textToSpeak !="") {
            TextToSpeechManager.pauseRequested = false
            TextToSpeechManager.speak(textToSpeak)
        }
    }

    private fun stopSpeaking() {
        Log.d(TAG, "stopSpeaking")
        if (TextToSpeechManager.isSpeaking || TextToSpeechManager.pauseRequested) { //TextToSpeechManager.currentText !=""
            TextToSpeechManager.stopRequested = true
            TextToSpeechManager.pauseRequested = false
        }
        textToSpeak = ""
    }

    private fun onChatItemClick(text: String, isUser:Boolean) {
        if (isUser) {
            Log.d(TAG, "Prompt item clicked: " + text.take(25) + "...")
            txtUserInput.setText(text.take(250))
        } else {
            if (speakingEnabled && !TextToSpeechManager.stopRequested) {
                Log.d(TAG, "onChatItemClick: Response item clicked $speakingEnabled - " + text.take(25) + "...")
                if (!TextToSpeechManager.isSpeaking && !TextToSpeechManager.pauseRequested) {
                    Log.d(TAG, "Clearing prior speech text")
                    textToSpeak=""
                }
                if (textToSpeak == text) {
                    Log.d(TAG, "onChatItemClick: Matching text, pause toggle")
                    toggleSpeakingPause()
                } else {
                    Log.d(TAG, "onChatItemClick: New text, stop old and start new")
                    startProcessing(true)
                    if (TextToSpeechManager.isSpeaking) {
                        notifyUser("Stopping...")
                        stopSpeaking()
                    }
                    textToSpeak = text
                    lifecycleScope.launch {
                        speakText()
                        startProcessing(false)
                    }
                }
            } else {
                Log.d(TAG, "Ignoring tap due to stop request pending...")
            }
        }
    }

    private fun showPopupMenu(view: View) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.conversation_options_menu, popup.menu)
        popup.menu.findItem(R.id.option_speak)?.isChecked = speakingEnabled
        popup.menu.findItem(R.id.option_autosubmit)?.isChecked = autoSubmitEnabled
        popup.menu.findItem(R.id.option_save_as_note)?.isEnabled =
            ChatManager.messages_extended.any { it.role == "assistant" }

        popup.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.option_save_as_note -> {
                    saveAsNote()
                    true
                }
                else -> {
                    val newState = !item.isChecked
                    item.isChecked = newState
                    updateMenuItemTitle(item)
                    when (item.itemId) {
                        R.id.option_speak -> {
                            speakingEnabled = newState
                            StorageManager.saveSpeakOutputPref(newState)
                            if (TextToSpeechManager.isSpeaking) { stopSpeaking() }
                            true
                        }
                        R.id.option_autosubmit -> {
                            autoSubmitEnabled = newState
                            StorageManager.saveAutoSubmitPref(newState)
                            true
                        }
                        else -> false
                    }
                }
            }
        }
        popup.show()
    }

    private fun saveAsNote() {
        val content = ChatManager.messages_extended
            .filter { it.role == "assistant" }
            .joinToString("\n\n") { it.content.trim() }
        if (content.isBlank()) return
        val note = NoteEntry(noteID = Date().time, content = content)
        StorageManager.saveNote(note)
        stopSpeaking()
        val intent = Intent(this, NoteActivity::class.java)
        intent.putExtra("noteID", note.noteID)
        startActivity(intent)
    }
    private fun updateMenuItemTitle(item: MenuItem) {
        val state = if (item.isChecked) "On" else "Off"
        val baseTitle = item.title?.split(" ")?.firstOrNull() ?: return
        item.title = "$baseTitle $state"
    }

}