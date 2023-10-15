package com.chatty.android

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.chatty.android.etc.ChatManager
import com.chatty.android.etc.ChatManager.conversationList
import com.chatty.android.etc.StorageManager
import com.chatty.android.ui.ConversationListRVAdapter
import com.chatty.android.ui.ConversationListRVAdapterSwipeCallBack
import kotlinx.coroutines.launch

class ConversationListActivity: AppCompatActivity(), ConversationListRVAdapter.SwipeListener {
    private val TAG = "ConversationListActivity"
    private lateinit var recyclerView: RecyclerView
    private lateinit var scrollUp: ImageButton
    private lateinit var scrollDown: ImageButton
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private var firstVisibleItemPosition :Int = 0
    private var lastVisibleItemPosition :Int = 0
    private var totalItemCount :Int = 0

    override fun onItemSwiped(position: Int, conversationID: String) {
        Log.d(TAG, "Swiped conversation deletion: " + conversationID)
        conversationList.removeAt(position)
        ChatManager.deleteConversation(conversationID.toLong())
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
        scrollUp = findViewById(R.id.rvScrollUp)
        scrollDown = findViewById(R.id.rvScrollDown)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener {
            Toast.makeText(this, "Background sync started...", Toast.LENGTH_SHORT).show()
            StorageManager.syncDatabases()
            swipeRefreshLayout.isRefreshing = false
        }
        recyclerView.layoutManager = layoutManager
        var rvAdapter: ConversationListRVAdapter = ConversationListRVAdapter(this, conversationList, this::onItemClick)
        rvAdapter.swipeListener = this
        recyclerView.adapter = rvAdapter
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
            recyclerView.smoothScrollToPosition(rvAdapter.getItemCount()-1)
        }
        scrollUp.setOnClickListener {
            recyclerView.smoothScrollToPosition(0)
        }
        val itemTouchHelper = ItemTouchHelper(ConversationListRVAdapterSwipeCallBack(rvAdapter, this))
        itemTouchHelper.attachToRecyclerView(recyclerView)

    }

    private fun onItemClick(conversationID: Long) {
        Log.d(TAG, "Conversation item clicked: $conversationID")
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra("conversationID", conversationID)
        setResult(RESULT_OK, intent)
        finish()
    }
}
