package com.chatty.android

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chatty.android.etc.DataClasses.PriceWorkingSetEntry
import com.chatty.android.etc.StorageManager
import com.chatty.android.etc.TextUtility
import com.chatty.android.ui.PriceWorkingSetRVAdapter
import com.google.android.material.navigation.NavigationView

class StockListActivity : AppCompatActivity() {

    private val TAG = "StockListActivity"

    private lateinit var btnHamburger: ImageButton
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var recyclerView: RecyclerView
    private lateinit var rvAdapter: PriceWorkingSetRVAdapter
    private lateinit var scrollUp: ImageButton
    private lateinit var scrollDown: ImageButton
    private lateinit var spinPriceSort: Spinner
    private lateinit var txtPriceSearch: EditText
    private lateinit var tvEmpty: TextView
    private lateinit var listContainer: View
    private lateinit var btnTargetFilter: ImageButton

    private var allEntries: List<PriceWorkingSetEntry> = emptyList()
    private var filterTargetHoldings = false
    private var firstVisibleItemPosition = 0
    private var lastVisibleItemPosition = 0
    private var totalItemCount = 0

    private val sortOptions = listOf(
        "Sort: Ticker A→Z",
        "Sort: Price (High→Low)",
        "Sort: 1-Day % (High→Low)",
        "Sort: 1-Month % (High→Low)",
        "Sort: 1-Year % (High→Low)",
        "Sort: Target (High→Low)"
    )

    override fun onStart() { Log.d(TAG, "onStart"); super.onStart() }
    override fun onResume() { Log.d(TAG, "onResume"); super.onResume() }
    override fun onPause() { Log.d(TAG, "onPause"); super.onPause() }
    override fun onStop() { Log.d(TAG, "onStop"); super.onStop() }
    override fun onDestroy() { Log.d(TAG, "onDestroy"); super.onDestroy() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.price_working_set_list_activity)
        val layoutManager = LinearLayoutManager(this)

        btnHamburger = findViewById(R.id.btnHamburger)
        drawerLayout = findViewById(R.id.drawerLayout)
        navView = findViewById(R.id.navView)
        recyclerView = findViewById(R.id.rvPriceList)
        scrollUp = findViewById(R.id.rvScrollUp)
        scrollDown = findViewById(R.id.rvScrollDown)
        spinPriceSort = findViewById(R.id.spinPriceSort)
        txtPriceSearch = findViewById(R.id.txtPriceSearch)
        tvEmpty = findViewById(R.id.tvEmpty)
        listContainer = findViewById(R.id.listContainer)
        btnTargetFilter = findViewById(R.id.btnTargetFilter)

        navView.setCheckedItem(R.id.nav_prices)
        btnHamburger.setOnClickListener { drawerLayout.openDrawer(GravityCompat.START) }
        btnTargetFilter.setOnClickListener { toggleTargetFilter() }

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
                R.id.nav_usage -> startActivity(Intent(this, UsageActivity::class.java))
                R.id.nav_prices -> { /* already here */ }
                R.id.nav_settings -> startActivity(Intent(this, SettingsActivity::class.java))
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

        val sortAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sortOptions)
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinPriceSort.adapter = sortAdapter
        spinPriceSort.setSelection(5, false)
        spinPriceSort.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                applyFilterAndSort()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        rvAdapter = PriceWorkingSetRVAdapter(this, emptyList(), this::onTickerClicked)
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
        scrollDown.setOnClickListener { recyclerView.smoothScrollToPosition(rvAdapter.itemCount - 1) }
        scrollUp.setOnClickListener { recyclerView.smoothScrollToPosition(0) }

        txtPriceSearch.addTextChangedListener(TextUtility.TypingTextWatcher {
            applyFilterAndSort()
        })

        loadData()
    }

    private fun loadData() {
        allEntries = StorageManager.getPricesWorkingSet()
        listContainer.visibility = View.VISIBLE
        applyFilterAndSort()
    }

    private fun toggleTargetFilter() {
        filterTargetHoldings = !filterTargetHoldings
        val icon = if (filterTargetHoldings) R.drawable.filter_clear else R.drawable.filter
        btnTargetFilter.setImageDrawable(resources.getDrawable(icon, null))
        applyFilterAndSort()
    }

    private fun applyFilterAndSort() {
        val searchText = txtPriceSearch.text.toString().trim().lowercase()
        val filtered = allEntries.filter { entry ->
            val matchesSearch = searchText.isEmpty() ||
                entry.ticker.lowercase().contains(searchText) ||
                entry.companyName.lowercase().contains(searchText)
            val matchesTarget = !filterTargetHoldings || entry.targetHoldings > 0
            matchesSearch && matchesTarget
        }
        val sorted = when (spinPriceSort.selectedItemPosition) {
            0 -> filtered.sortedBy { it.ticker }
            1 -> filtered.sortedByDescending { it.currentPrice }
            2 -> filtered.sortedByDescending { it.pc1Day }
            3 -> filtered.sortedByDescending { it.pc1Month }
            4 -> filtered.sortedByDescending { it.pc1Year }
            5 -> filtered.sortedByDescending { it.targetHoldings }
            else -> filtered.sortedBy { it.ticker }
        }
        rvAdapter.updateData(sorted)
        tvEmpty.visibility = if (sorted.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun onTickerClicked(ticker: String) {
        Log.d(TAG, "Opening detail for: $ticker")
        startActivity(Intent(this, StockDetailActivity::class.java).apply {
            putExtra("ticker", ticker)
        })
    }
}
