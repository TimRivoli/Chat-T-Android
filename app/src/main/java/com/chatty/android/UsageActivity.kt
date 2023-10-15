package com.chatty.android

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chatty.android.etc.ChatManager.chatUsage
import com.chatty.android.ui.UsageRVAdapter

class UsageActivity: AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var scrollUp: ImageButton
    private lateinit var scrollDown: ImageButton
    private var firstVisibleItemPosition :Int = 0
    private var lastVisibleItemPosition :Int = 0
    private var totalItemCount :Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.usage_activity)
        val layoutManager = LinearLayoutManager(this)
        recyclerView = findViewById(R.id.chatUsageRecyclerView)
        scrollUp = findViewById(R.id.rvScrollUp)
        scrollDown = findViewById(R.id.rvScrollDown)
        var rvAdapter: UsageRVAdapter = UsageRVAdapter(this, chatUsage, this::onItemClick)
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
    }
    private fun onItemClick(text: String) {
        println("Conversation item clicked: " + text)
    }
}
