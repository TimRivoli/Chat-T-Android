package com.chatty.android

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.chatty.android.etc.FirebaseManager
import com.chatty.android.etc.NetworkManager
import com.chatty.android.etc.StorageManager
import com.chatty.android.etc.googleClientID
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"
    private lateinit var auth: FirebaseAuth
    private lateinit var btnLogo: ImageButton
    private lateinit var btnGoChat: ImageButton
    private lateinit var btnGoNotes: ImageButton
    private lateinit var txtStatusDisplay: TextView
    private var signInCompleted = false
    private val credentialManager by lazy { CredentialManager.create(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        btnLogo = findViewById(R.id.btnLogo)
        btnGoChat = findViewById(R.id.btnGoChat)
        btnGoNotes = findViewById(R.id.btnGoNotes)
        txtStatusDisplay = findViewById(R.id.txtStatusDisplay)
        btnGoChat.isEnabled = false
        btnGoChat.setOnClickListener { goChat() }
        btnGoNotes.setOnClickListener { goNotes() }
        btnLogo.setOnClickListener { doLogoClick() }
        auth = FirebaseAuth.getInstance()
        waitForInitialization()
    }

    private fun waitForInitialization() {
        txtStatusDisplay.text = getString(R.string.status_initializing)
        lifecycleScope.launch {
            while (!StorageManager.settingsLoaded) {
                delay(500)
            }
            val currentUser = auth.currentUser
            if (StorageManager.useGoogleAuth && (currentUser == null || currentUser.isAnonymous)) {
                Log.d(TAG, "waitForInitialization: Google auth required, prompting sign-in")
                DisplayMessage(getString(R.string.status_authenticating_google))
                signInWithGoogle()
            } else {
                doLogoClick()
            }
        }
    }

    private fun signInWithGoogle() {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(googleClientID)
            .setAutoSelectEnabled(false)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(request = request, context = this@MainActivity)
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
                firebaseAuthWithGoogle(googleIdTokenCredential.idToken)
            } catch (e: GetCredentialException) {
                Log.w(TAG, "Google sign in failed: ${e.message}")
                handleSubscriptionCheckResult()
            }
        }
    }

    override fun onStart() {
        Log.d(TAG, "onStart")
        super.onStart()
    }

    override fun startActivity(intent: Intent?, options: Bundle?) {
        Log.d(TAG, "startActivity")
        NetworkManager.checkNetworkStatus(this@MainActivity)
        super.startActivity(intent, options)
    }

    override fun onRestart() {
        Log.d(TAG, "onRestart")
        super.onRestart()
    }

    override fun onResume() {
        Log.d(TAG, "onResume")
        super.onResume()
        if (signInCompleted) {
            handleSubscriptionCheckResult()
        }
    }

    override fun onPause() {
        Log.d(TAG, "onPause")
        super.onPause()
    }

    override fun onStop() {
        Log.d(TAG, "onStop")
        super.onStop()
    }

    override fun isFinishing(): Boolean {
        Log.d(TAG, "isFinishing")
        return super.isFinishing()
    }

    override fun finish() {
        Log.d(TAG, "finish")
        super.finish()
    }

    private fun doLogoClick() {
        NetworkManager.checkNetworkStatus(this@MainActivity)
        if (NetworkManager.isNetworkGood) {
            if (!signInCompleted) {
                login()
            } else {
                if (!FirebaseManager.isFunctional) {
                    DisplayMessage(getString(R.string.status_cloud_not_functional))
                    if (StorageManager.encryptionPending) {
                        DisplayMessage(getString(R.string.status_awaiting_decryption_key))
                    }
                } else if (StorageManager.encryptContent) {
                    DisplayMessage(getString(R.string.status_running_sync))
                    StorageManager.syncDatabases()
                }
            }
        } else {
            DisplayMessage(getString(R.string.status_no_internet))
            btnGoChat.isEnabled = false
        }
    }

    private fun showAlert(message: String) {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setMessage(message)
        dialogBuilder.setPositiveButton(R.string.dialog_button_ok) { dialog, _ ->
            dialog.dismiss()
        }
        dialogBuilder.create().show()
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val googleUID = auth.currentUser?.uid
                    if (googleUID != null) {
                        Log.d(TAG, "Firebase auth with Google: success, UID=$googleUID")
                        FirebaseManager.onGoogleAuthComplete(googleUID)
                        StorageManager.useGoogleAuth = true
                        StorageManager.postGoogleAuthInit()
                    }
                    handleSubscriptionCheckResult()
                } else {
                    Log.w(TAG, "Firebase auth with Google: failure", task.exception)
                    handleSubscriptionCheckResult()
                }
            }
    }

    private fun login() {
        if (!StorageManager.useGoogleAuth) {
            Log.d(TAG, "login: Google auth not required for this device.")
            handleSubscriptionCheckResult()
            return
        }
        val currentUser = auth.currentUser
        if (currentUser != null && !currentUser.isAnonymous) {
            Log.d(TAG, "login: Already signed in with Google account ${currentUser.uid}")
            handleSubscriptionCheckResult()
        } else {
            DisplayMessage("Authenticating with your Google account...")
            signInWithGoogle()
        }
    }

    private fun handleSubscriptionCheckResult() {
        if (StorageManager.subscriptionLevel == 0) {
            DisplayMessage(getString(R.string.status_no_subscription))
        } else {
            DisplayMessage(getString(R.string.status_choose_section))
            enableChat()
            signInCompleted = true
            if (StorageManager.pendingKeyTransferPublicKey.isNotEmpty()) {
                startActivity(Intent(this, KeyTransferApprovalActivity::class.java))
            }
        }
    }

    private fun enableChat() {
        val drawable = resources.getDrawable(R.drawable.chat, null)
        btnGoChat.setImageDrawable(drawable)
        btnGoChat.isEnabled = true
    }

    private fun goChat() {
        Log.d(TAG, "goChat clicked")
        val intent = Intent(this, ChatActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
    }

    private fun goNotes() {
        val intent = Intent(this, NoteListActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
    }

    fun DisplayMessage(text: String, append: Boolean = false) {
        if (append) {
            txtStatusDisplay.setText(txtStatusDisplay.text.toString() + "/n" + text)
        } else {
            txtStatusDisplay.setText(text)
        }
    }
}
