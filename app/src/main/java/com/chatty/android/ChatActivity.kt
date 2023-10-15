package com.chatty.android

import android.content.Intent
import android.os.Bundle
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
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.chatty.android.etc.ChatManager
import com.chatty.android.etc.SpeechToTextManager
import kotlinx.coroutines.launch
import com.chatty.android.etc.DataClasses.*
import com.chatty.android.etc.StorageManager
import com.chatty.android.etc.TextToSpeechManager
import com.chatty.android.ui.ChatMessageRVAdapter
import com.chatty.android.ui.ChatMessageRVAdapterSwipeCallBack

class ChatActivity: AppCompatActivity(), ChatMessageRVAdapter.SwipeListener {
    private val TAG = "ChatActivity"
    private lateinit var btnSend: ImageButton
    private lateinit var btnSpeak: ImageButton
    private lateinit var btnSave: ImageButton
    private lateinit var btnClear: ImageButton
    private lateinit var btnMicrophone: ImageButton
    private lateinit var btnConversationList: ImageButton
    private lateinit var txtUserInput: TextView
    private lateinit var progBar: ProgressBar
    private lateinit var spinnerChatMode: Spinner
    private lateinit var chatActivities: ArrayList<ChatActivityType>
    private lateinit var chatActivityType: ChatActivityType
    private lateinit var spinnerLanguages: Spinner
    private lateinit var spinnerContainer: CardView
    private lateinit var recyclerView: RecyclerView
    private lateinit var scrollUp: ImageButton
    private lateinit var scrollDown: ImageButton
    private lateinit var rvAdapter: ChatMessageRVAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var chatSampleConstraint: ConstraintLayout
    private lateinit var chatSampleItem: TextView
    private val spinnerContainerWidthNarrow: Int = 330
    private val spinnerContainerWidthWide: Int = 550
    private val autoSubmitWaitTime = 2000 //Millis after text changed before auto-submit
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

    override fun onBackPressed() {
        Log.d(TAG,"onBackPressed")
        if (ChatManager.messages_extended.size==0) {
            finishAffinity()
            //super.onBackPressed()
        } else {
            clearConversation()
        }
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
        //if (savedInstanceState != null) {     }
        chatActivities = StorageManager.getChatModes()
        chatActivityType = chatActivities[0]
        TextToSpeechManager.startup(this)
        txtUserInput = findViewById(R.id.chatTextUserInput)
        btnSend = findViewById(R.id.btnSubmit)
        btnSpeak = findViewById(R.id.chatButtonSpeak)
        btnSave = findViewById(R.id.chatButtonSave)
        btnClear = findViewById(R.id.chatButtonClear)
        btnConversationList = findViewById(R.id.btnListConversations)
        progBar = findViewById(R.id.chatProgressBar)
        spinnerContainer = findViewById(R.id.chatSpinnderCardContainer)
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
                if (dy > 0 && (lastVisibleItemPosition < totalItemCount - 1)) {
                    scrollDown.visibility = View.VISIBLE
                } else {
                    scrollDown.visibility = View.GONE
                }
                if (dy < 0 && (firstVisibleItemPosition > 0)) {
                    scrollUp.visibility = View.VISIBLE
                } else {
                    scrollUp.visibility = View.GONE
                }
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
        btnSpeak.setOnClickListener {
            toggleSpeakingEnabled()
        }
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
        btnConversationList.setOnClickListener {
            stopSpeaking()
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
            Log.i(TAG, "Reseeting autoSubmitCountDown")
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
                if (s != null) {
                    if (!autoSubmitRunning && s.length > 1) { //Ensure only one thread
                        autoSubmitRunning = true
                        autoSubmitCountDown = autoSubmitWaitTime    //Input was changed so start over
                        Log.d(TAG, "Starting auto-submit thread.")
                        val thread = Thread {
                            while (autoSubmitCountDown > 0 && !processing) {
                                val priorLength = s.length
                                Thread.sleep(200)
                                if (priorLength == s.length) {
                                    autoSubmitCountDown -= 200
                                } else {
                                    autoSubmitCountDown = autoSubmitTypingWaitTime  //backspace and soft keyboard aren't captured by keypress
                                }
                            }
                            if (!processing) { executeChatRequest() }
                            //Log.d(TAG, "Resetting auto-submit countdown.")
                            autoSubmitCountDown = autoSubmitWaitTime    //Input was changed so start over
                            autoSubmitRunning = false
                        }
                        thread.start()
                    }
                }
            }
        })

        //Spinner setup
        var chatList = ArrayList<String>()
        for (m in chatActivities) {chatList.add(m.activityName)}
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, chatList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerContainer.layoutParams.width=spinnerContainerWidthNarrow
        spinnerChatMode = findViewById(R.id.chatSpinnerMode)
        spinnerChatMode.adapter = adapter
        val adapter2 = ArrayAdapter(this, android.R.layout.simple_spinner_item, ChatLanguageOption.values())
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
                        spinnerContainer.layoutParams.width=spinnerContainerWidthWide
                        spinnerContainer.requestLayout()
                    } else {
                         spinnerLanguages.visibility = View.INVISIBLE
                        spinnerContainer.layoutParams.width=spinnerContainerWidthNarrow
                        spinnerContainer.requestLayout()
                    }
                    suppressSpinerConversationClear = false
                }
            }

            override fun onNothingSelected(parentView: AdapterView<*>) {
                spinnerLanguages.visibility = View.INVISIBLE
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
                if (conversationID > 0) {
                    Log.d(TAG, "Loading from conversationListResultLauncher conversationID: $conversationID")
                    suppressSpinerConversationClear = true
                    spinnerChatMode.setSelection(0)
                    loadConversation(conversationID)
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
        var question:String = txtUserInput.text.toString()
        question = question.replace("\n", "")
        if (question !="" && !processing){
            lifecycleScope.launch {
                startProcessing(true)
                val selectedLanguage = spinnerLanguages.selectedItem as ChatLanguageOption
                question = ChatManager.formatPromptPrettyLike(question)
                textToSpeak = ChatManager.chatbotQuery(question, chatActivityType, selectedLanguage, "")
                notifyDataChanged()
                speakText()
                txtUserInput.text = ""
                scrollToBottom()
                startProcessing(false)
            }
        }
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

    private fun toggleSpeakingEnabled(){
        val drawableEnabled = resources.getDrawable(R.drawable.sound_up, null)
        val drawableDisabled = resources.getDrawable(R.drawable.sound_off, null)
        textToSpeak = ""
        if (TextToSpeechManager.speechEnabled){
            if (TextToSpeechManager.isSpeaking) { stopSpeaking() }
            speakingEnabled = !speakingEnabled
            Log.d(TAG, "toggleSpeakingEnabled to $speakingEnabled")
            if (speakingEnabled) {
                btnSpeak.setImageDrawable(drawableEnabled)
            } else {
                btnSpeak.setImageDrawable(drawableDisabled)
            }
        } else {
            btnSpeak.setImageDrawable(drawableDisabled)
            btnSpeak.isEnabled = false
            speakingEnabled = false
        }
    }

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
    private fun speakText(){
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
                    speakText()
                    startProcessing(false)
                }
            } else {
                Log.d(TAG, "Ignoring tap due to stop request pending...")
            }
        }
    }

}