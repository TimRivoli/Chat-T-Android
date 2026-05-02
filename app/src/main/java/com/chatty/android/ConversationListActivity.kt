package com.chatty.android

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.activity.OnBackPressedCallback
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.chatty.android.etc.ChatManager
import com.google.android.material.navigation.NavigationView
import com.chatty.android.etc.ChatManager.conversationList
import com.chatty.android.etc.StorageManager
import com.chatty.android.etc.TextUtility
import com.chatty.android.ui.ConversationListRVAdapter
import com.chatty.android.ui.ConversationListRVAdapterSwipeCallBack
import kotlinx.coroutines.launch

class ConversationListActivity: AppCompatActivity(), ConversationListRVAdapter.SwipeListener {
    private val TAG = "ConversationListActivity"
    private val MaxResults = 50
    private lateinit var recyclerView: RecyclerView
    private lateinit var rvAdapter:ConversationListRVAdapter
    private lateinit var btnHamburger: ImageButton
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var tvEmpty: TextView
    private lateinit var scrollUp: ImageButton
    private lateinit var scrollDown: ImageButton
    private lateinit var txtSearch: EditText
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var fabNewConversation: FloatingActionButton
    private var firstVisibleItemPosition :Int = 0
    private var lastVisibleItemPosition :Int = 0
    private var totalItemCount :Int = 0
    private var currentlyProcessing = false

    override fun onItemSwiped(position: Int, conversationID: String) {
        Log.d(TAG, "Swiped conversation deletion: " + conversationID)
        conversationList.removeAt(position)
        ChatManager.deleteConversation(conversationID.toLong())
        updateEmptyState()
    }
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.conversation_list_activity)
        val layoutManager = LinearLayoutManager(this)
        recyclerView = findViewById(R.id.chatConversationRecyclerView)
        btnHamburger = findViewById(R.id.btnHamburger)
        drawerLayout = findViewById(R.id.drawerLayout)
        navView = findViewById(R.id.navView)
        tvEmpty = findViewById(R.id.tvEmpty)
        scrollUp = findViewById(R.id.rvScrollUp)
        scrollDown = findViewById(R.id.rvScrollDown)
        txtSearch = findViewById(R.id.txtConversationSearch)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        fabNewConversation = findViewById(R.id.fabNewConversation)
        swipeRefreshLayout.setOnRefreshListener {
            Toast.makeText(this, R.string.toast_sync_started, Toast.LENGTH_SHORT).show()
            StorageManager.syncDatabases()
            swipeRefreshLayout.isRefreshing = false
        }
        recyclerView.layoutManager = layoutManager
        rvAdapter = ConversationListRVAdapter(this, conversationList, this::onItemClick)
        rvAdapter.swipeListener = this
        recyclerView.adapter = rvAdapter
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                totalItemCount = layoutManager.itemCount
                scrollDown.visibility = if (lastVisibleItemPosition < totalItemCount - 1) View.VISIBLE else View.GONE
                scrollUp.visibility = if (firstVisibleItemPosition > 0) View.VISIBLE else View.GONE
            }
        })
        navView.setCheckedItem(R.id.nav_conversations)
        btnHamburger.setOnClickListener { drawerLayout.openDrawer(GravityCompat.START) }
        navView.setNavigationItemSelectedListener { item ->
            drawerLayout.closeDrawer(GravityCompat.START)
            when (item.itemId) {
                R.id.nav_home -> startActivity(Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                })
                R.id.nav_conversations -> { /* already here */ }
                R.id.nav_notes -> startActivity(Intent(this, NoteListActivity::class.java))
                R.id.nav_usage -> startActivity(Intent(this, UsageActivity::class.java))
                R.id.nav_prices -> startActivity(Intent(this, StockListActivity::class.java))
                R.id.nav_settings -> startActivity(Intent(this, SettingsActivity::class.java))
            }
            true
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    goBack()
                }
            }
        })
        updateEmptyState()
        scrollDown.setOnClickListener {
            recyclerView.smoothScrollToPosition(rvAdapter.getItemCount()-1)
        }
        scrollUp.setOnClickListener {
            recyclerView.smoothScrollToPosition(0)
        }
        txtSearch.addTextChangedListener(TextUtility.TypingTextWatcher {
            applyFilter()
        })

        val itemTouchHelper = ItemTouchHelper(ConversationListRVAdapterSwipeCallBack(rvAdapter, this))
        itemTouchHelper.attachToRecyclerView(recyclerView)

        fabNewConversation.setOnClickListener { startNewConversation() }
    }

    private fun onItemClick(conversationID: Long) {
        Log.d(TAG, "Conversation item clicked: $conversationID")
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra("conversationID", conversationID)
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun startProcessing() {
        currentlyProcessing = true
        txtSearch.isEnabled = false
        recyclerView.isEnabled = false
    }
    private fun endProcessing() {
        currentlyProcessing = false
        txtSearch.isEnabled = true
        recyclerView.isEnabled = true
    }

    private fun applyFilter() {
        var searchText = ""
        if (!currentlyProcessing) {
            startProcessing()
            searchText = txtSearch.text.toString()
            Log.d(TAG, "Applying filter text: ${searchText}")
            ChatManager.getConversations(searchText, MaxResults)
            rvAdapter.SetMessages(ChatManager.conversationList)
            updateEmptyState()
            endProcessing()
        }
    }
    private fun updateEmptyState() {
        tvEmpty.visibility = if (conversationList.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun startNewConversation() {
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra("conversationID", -1L)
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun goBack () {
        val intent = Intent(this, ChatActivity::class.java)
        setResult(RESULT_CANCELED, intent)
        finish()
    }

}
