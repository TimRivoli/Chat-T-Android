package com.chatty.android

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.chatty.android.etc.StorageManager
import com.google.android.material.navigation.NavigationView

class SettingsActivity : AppCompatActivity() {
    private lateinit var btnHamburger: ImageButton
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var switchSpeakOutput: SwitchCompat
    private lateinit var switchAutoSubmit: SwitchCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        btnHamburger = findViewById(R.id.btnHamburger)
        drawerLayout = findViewById(R.id.drawerLayout)
        navView = findViewById(R.id.navView)
        switchSpeakOutput = findViewById(R.id.switchSpeakOutput)
        switchAutoSubmit = findViewById(R.id.switchAutoSubmit)

        switchSpeakOutput.isChecked = StorageManager.getSpeakOutputPref()
        switchAutoSubmit.isChecked = StorageManager.getAutoSubmitPref()

        switchSpeakOutput.setOnCheckedChangeListener { _, isChecked ->
            StorageManager.saveSpeakOutputPref(isChecked)
        }
        switchAutoSubmit.setOnCheckedChangeListener { _, isChecked ->
            StorageManager.saveAutoSubmitPref(isChecked)
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
                R.id.nav_usage -> startActivity(Intent(this, UsageActivity::class.java))
                R.id.nav_settings -> { /* already here */ }
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
}