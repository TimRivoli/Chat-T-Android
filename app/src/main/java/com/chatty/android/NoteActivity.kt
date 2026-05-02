package com.chatty.android

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ImageButton
import android.widget.TextView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.chatty.android.etc.DataClasses
import com.chatty.android.etc.StorageManager
import com.chatty.android.etc.TextUtility


class NoteActivity: AppCompatActivity() {
    private val TAG = "NoteActivity"
    private lateinit var btnHamburger: ImageButton
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var btnEnableEdit: ImageButton
    private lateinit var btnSave: ImageButton
    private lateinit var btnDelete: ImageButton
    private lateinit var btnNewParagraph: ImageButton
    private lateinit var btnNewBulletItem: ImageButton
    private lateinit var btnUndo: ImageButton
    private lateinit var spinNoteCategory: Spinner
    private lateinit var txtNoteTitle: TextView
    private lateinit var txtNoteContent: EditText
    private lateinit var cvEnableEdit: CardView
    private lateinit var cvEditingTools: CardView
    private lateinit var currentNote: DataClasses.NoteEntry
    private var editEnabled: Boolean = false
    private var saveNeeded: Boolean = false
    private var currentlyProcessing = false

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

    override fun onSaveInstanceState(outState: Bundle) {
        Log.d(TAG, "onSaveInstanceState")
        super.onSaveInstanceState(outState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate NoteActivity")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.note_activity)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.noteScrollView)) { view, insets ->
            val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            view.setPadding(0, 0, 0, maxOf(navBar.bottom, ime.bottom))
            insets
        }

        val noteID = intent.getLongExtra("noteID", 0)
        var defaultCategoryName: String = intent.getStringExtra("categoryName") ?: ""
        currentNote = StorageManager.getNote(noteID)
        defaultCategoryName = currentNote.categoryName
        if (defaultCategoryName=="Any") {defaultCategoryName = ""}

        cvEnableEdit = findViewById(R.id.cvEnableEdit)
        cvEditingTools = findViewById(R.id.cvEditingTools)
        btnHamburger = findViewById(R.id.btnHamburger)
        drawerLayout = findViewById(R.id.drawerLayout)
        navView = findViewById(R.id.navView)
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
                R.id.nav_usage -> startActivity(Intent(this, UsageActivity::class.java))
                R.id.nav_settings -> startActivity(Intent(this, SettingsActivity::class.java))
            }
            true
        }
        btnEnableEdit = findViewById(R.id.btnNoteEnableEdit)
        btnSave = findViewById(R.id.btnNoteSave)
        btnDelete = findViewById(R.id.btnNoteDelete)
        btnNewParagraph = findViewById(R.id.btnNewParagraph)
        btnNewBulletItem = findViewById(R.id.btnNewBulletItem)
        btnUndo = findViewById(R.id.btnUndo)
        txtNoteTitle = findViewById(R.id.txtNoteTitle)
        txtNoteContent = findViewById(R.id.txtNoteContent)
        spinNoteCategory = findViewById(R.id.spinNoteCategoryNote)

        btnEnableEdit.setOnClickListener { enableEdit() }
        txtNoteTitle.setOnClickListener { if (!editEnabled) enableEdit() }
        txtNoteContent.setOnClickListener { if (!editEnabled) enableEdit() }

        btnSave.setOnClickListener {saveNote() }
        btnDelete.isLongClickable=true
        btnDelete.setOnLongClickListener() {
            deleteNote()
            true}
        btnNewParagraph.setOnClickListener {
            TextUtility.newParagraph(txtNoteContent)
            setEditFocusWithKeyboardUp()
        }
        btnNewBulletItem.setOnClickListener {
            TextUtility.newBulletItem(txtNoteContent)
            setEditFocusWithKeyboardUp()
        }
        btnUndo.setOnClickListener {
            txtNoteContent.onTextContextMenuItem(android.R.id.undo)
        }

        cvEnableEdit.visibility = View.VISIBLE
        cvEditingTools.visibility = View.GONE
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, StorageManager.getNoteCategories())
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinNoteCategory.adapter = adapter

        val position = adapter.getPosition(defaultCategoryName)
        if (position >= 0) {
            spinNoteCategory.setSelection(position)
        }
        spinNoteCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>, selectedItemView: View?, position: Int, id: Long) {
                val selectedValue: String = parentView.getItemAtPosition(position).toString()
                if (selectedValue != currentNote.categoryName && editEnabled ) { setNoteCategory(selectedValue) }
            }
            override fun onNothingSelected(parentView: AdapterView<*>) {  }
        }

        txtNoteTitle.text = currentNote.title
        txtNoteContent.gravity = android.view.Gravity.TOP or android.view.Gravity.START
        if (currentNote.title == "") {
            txtNoteContent.setText(currentNote.content)
            enableEdit()
        } else {
            txtNoteContent.setText(TextUtility.applyMarkdownSpans(currentNote.content))
        }
        spinNoteCategory.isEnabled = editEnabled

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Log.d(TAG, "onBackPressed via callback")
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    goBack()
                }
            }
        })
    }

    private fun setEditFocusWithKeyboardUp() {
        txtNoteContent.requestFocus()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(txtNoteContent, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun enableEdit() {
        editEnabled = true
        cvEditingTools.visibility = View.VISIBLE
        cvEnableEdit.visibility = View.GONE
        txtNoteTitle.isFocusable = true
        txtNoteTitle.isFocusableInTouchMode = true
        txtNoteTitle.isCursorVisible = true
        txtNoteTitle.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        txtNoteContent.isFocusable = true
        txtNoteContent.isFocusableInTouchMode = true
        txtNoteContent.isCursorVisible = true
        txtNoteContent.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        txtNoteContent.setLines(4)
        txtNoteContent.maxLines = Integer.MAX_VALUE
        txtNoteContent.imeOptions = android.view.inputmethod.EditorInfo.IME_FLAG_NO_ENTER_ACTION
        txtNoteContent.setText(currentNote.content)
        spinNoteCategory.isEnabled = true

        val markDirtyWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { markDirty() }
        }
        txtNoteTitle.addTextChangedListener(markDirtyWatcher)
        txtNoteContent.addTextChangedListener(markDirtyWatcher)
        txtNoteTitle.addTextChangedListener(TextUtility.TypingTextWatcher { saveNote() })
        txtNoteContent.addTextChangedListener(TextUtility.TypingTextWatcher { saveNote() })

        setEditFocusWithKeyboardUp()
    }

    private fun setNoteCategory(categoryName: String) {
        val categoryID = StorageManager.getNoteCategoryID(categoryName)
        currentNote.categoryID = categoryID
        currentNote.categoryName = categoryName
        markDirty()
    }
    private fun markDirty () {
        saveNeeded=true
        btnSave.visibility = View.VISIBLE
    }
    private fun saveNote() {
        if (txtNoteContent.text.toString() !="") {
            var title = txtNoteTitle.text.toString()
            val content = txtNoteContent.editableText.toString()
            if (title == "") {
                title = content.take(35)
                txtNoteTitle.setText(title)
            }
            val minorChange = !(title != currentNote.title || content!=currentNote.content)
            currentNote.title = title
            currentNote.content = content
            currentNote.categoryName = spinNoteCategory.selectedItem.toString()
            StorageManager.saveNote(currentNote, minorChange)
            saveNeeded=false
            btnSave.visibility = View.GONE
        }
    }

    private fun deleteNote() {
        StorageManager.deleteNote(currentNote.noteID)
        saveNeeded=false
        goBack()
    }
    private fun goBack () {
        if (saveNeeded) { saveNote() }
        finish()
    }
}