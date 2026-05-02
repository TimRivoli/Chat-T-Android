package com.chatty.android

import android.content.Intent
import android.os.Build
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
import com.chatty.android.etc.StorageManager
import com.chatty.android.etc.TextUtility
import com.chatty.android.ui.NoteListRVAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView

class NoteListActivity: AppCompatActivity() {
    private val TAG = "NoteListActivity"
    private val maxResults=50
    private lateinit var btnHamburger: ImageButton
    private lateinit var btnFilter: ImageButton
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var btnNewNote: FloatingActionButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var rvAdapter: NoteListRVAdapter
    private lateinit var scrollUp: ImageButton
    private lateinit var scrollDown: ImageButton
    private lateinit var spinNoteCategory: Spinner
    private lateinit var txtNoteSearch: EditText
    private lateinit var tvEmpty: TextView
    private var currentlyProcessing = false
    private var filterApplied = false
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.note_list_activity)
        val layoutManager = LinearLayoutManager(this)
        btnHamburger = findViewById(R.id.btnHamburger)
        btnNewNote = findViewById(R.id.fabNewNote)
        btnFilter = findViewById(R.id.btnNoteSearch)
        txtNoteSearch = findViewById(R.id.txtNoteSearch)
        recyclerView = findViewById(R.id.rvNoteList)
        scrollUp = findViewById(R.id.rvScrollUp)
        scrollDown = findViewById(R.id.rvScrollDown)
        spinNoteCategory = findViewById(R.id.spinNoteCategoryNoteList)
        tvEmpty = findViewById(R.id.tvEmpty)
        drawerLayout = findViewById(R.id.drawerLayout)
        navView = findViewById(R.id.navView)

        navView.setCheckedItem(R.id.nav_notes)
        btnHamburger.setOnClickListener { drawerLayout.openDrawer(GravityCompat.START) }
        btnNewNote.setOnClickListener { goNote() }
        btnFilter.setOnClickListener { toggleFilter() }
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
                R.id.nav_notes -> { /* already here */ }
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
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, StorageManager.getNoteCategories(true))
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinNoteCategory.adapter = adapter
        spinNoteCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>, selectedItemView: View?, position: Int, id: Long) {
                if (filterApplied) {
                    applyFilter()
                } else {
                    toggleFilter()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {
                // Optionally handle the case when no item is selected
            }
        }
        recyclerView.layoutManager = layoutManager
        var noteList = StorageManager.getNotes(-1,"", maxResults)
        rvAdapter =  NoteListRVAdapter(this, noteList, this::onNoteItemClicked)
        recyclerView.adapter = rvAdapter
        updateEmptyState()
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

        txtNoteSearch.addTextChangedListener(TextUtility.TypingTextWatcher {
            filterApplied = true
            applyFilter()
        })

        scrollDown.setOnClickListener {
            recyclerView.smoothScrollToPosition(rvAdapter.getItemCount()-1)
        }
        scrollUp.setOnClickListener {
            recyclerView.smoothScrollToPosition(0)
        }
    }

    private fun onNoteItemClicked(noteID: Long) {
        Log.d(TAG, "Note item clicked: $noteID")
        goNote(noteID)
    }

    private fun applyFilter() {
        var categoryid:Int = -1
        var searchText = ""
        if (!currentlyProcessing) {
            startProcessing()
            if (filterApplied) {
                val drawable = resources.getDrawable(R.drawable.filter, null)
                btnFilter.setImageDrawable(drawable)
                categoryid = StorageManager.getNoteCategoryID(spinNoteCategory.selectedItem.toString())
                searchText = txtNoteSearch.text.toString()
            } else {
                val drawable = resources.getDrawable(R.drawable.filter_clear, null)
                btnFilter.setImageDrawable(drawable)
            }
            //Log.d(TAG, "Applying filter categoryid: ${categoryid} and text: ${searchText}")
            val noteList = StorageManager.getNotes(categoryid,searchText, maxResults)
            rvAdapter.updateData(noteList)
            updateEmptyState()
            endProcessing()
        }
    }

    private fun toggleFilter() {
        filterApplied = !filterApplied
        if (!filterApplied) {txtNoteSearch.setText("")}
        applyFilter()
    }
    private fun startProcessing() {
        currentlyProcessing = true
        txtNoteSearch.isEnabled = false
        spinNoteCategory.isEnabled = false
        recyclerView.isEnabled = false
    }
    private fun endProcessing() {
        currentlyProcessing = false
        txtNoteSearch.isEnabled = true
        spinNoteCategory.isEnabled = true
        recyclerView.isEnabled = true
    }

    private fun updateEmptyState() {
        tvEmpty.visibility = if (rvAdapter.itemCount == 0) View.VISIBLE else View.GONE
    }

    private fun goNote(noteID: Long=0) {
        Log.d(TAG, "Going to NoteID: ${noteID}")
        val intent = Intent(this, NoteActivity::class.java)
        intent.putExtra("noteID", noteID)
        intent.putExtra("categoryName", spinNoteCategory.selectedItem.toString())
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
    }
}
