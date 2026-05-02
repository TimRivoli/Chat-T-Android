package com.chatty.android

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chatty.android.etc.ChatManager.chatUsage
import com.chatty.android.ui.UsageRVAdapter
import com.google.android.material.navigation.NavigationView

class UsageActivity: AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var scrollUp: ImageButton
    private lateinit var scrollDown: ImageButton
    private lateinit var btnHamburger: ImageButton
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var tvEmpty: TextView
    private lateinit var tvSummaryPrompt: TextView
    private lateinit var tvSummaryCompletion: TextView
    private lateinit var tvSummaryTotal: TextView
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
        btnHamburger = findViewById(R.id.btnHamburger)
        drawerLayout = findViewById(R.id.drawerLayout)
        navView = findViewById(R.id.navView)
        tvEmpty = findViewById(R.id.tvEmpty)
        tvSummaryPrompt = findViewById(R.id.tvSummaryPrompt)
        tvSummaryCompletion = findViewById(R.id.tvSummaryCompletion)
        tvSummaryTotal = findViewById(R.id.tvSummaryTotal)

        navView.setCheckedItem(R.id.nav_usage)

        var rvAdapter: UsageRVAdapter = UsageRVAdapter(this, chatUsage, this::onItemClick)
        tvEmpty.visibility = if (chatUsage.isEmpty()) View.VISIBLE else View.GONE
        tvSummaryPrompt.text = chatUsage.sumOf { it.promptTokens }.toString()
        tvSummaryCompletion.text = chatUsage.sumOf { it.completionTokens }.toString()
        tvSummaryTotal.text = chatUsage.sumOf { it.totalTokens }.toString()
        recyclerView.adapter = rvAdapter
        recyclerView.layoutManager = layoutManager
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
        scrollDown.setOnClickListener {
            recyclerView.smoothScrollToPosition(rvAdapter.getItemCount()-1)
        }
        scrollUp.setOnClickListener {
            recyclerView.smoothScrollToPosition(0)
        }
        btnHamburger.setOnClickListener { drawerLayout.openDrawer(GravityCompat.START) }
        navView.setNavigationItemSelectedListener { item ->
            drawerLayout.closeDrawer(GravityCompat.START)
            when (item.itemId) {
                R.id.nav_home -> startActivity(Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                })
                R.id.nav_conversations -> startActivity(Intent(this, ChatActivity::class.java).apply {
                    putExtra("openConversationList", true)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                })
                R.id.nav_notes -> startActivity(Intent(this, NoteListActivity::class.java))
                R.id.nav_usage -> { /* already here */ }
                R.id.nav_prices -> startActivity(Intent(this, StockListActivity::class.java))
            }
            true
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun onItemClick(text: String) {
        println("Conversation item clicked: " + text)
    }
}
