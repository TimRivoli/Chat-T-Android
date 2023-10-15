package com.chatty.android

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.chatty.android.etc.webClientID
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.chatty.android.etc.StorageManager

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"
//Primary function is to log the user in
    private val RC_SIGN_IN = 123
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
    }

    override fun onStart() {
        Log.d(TAG, "onStart")
        super.onStart()
        login()
    }

    override fun startActivity(intent: Intent?, options: Bundle?) {
        Log.d(TAG, "startActivity")
        super.startActivity(intent, options)
    }
    override fun onRestart() {
        Log.d(TAG, "onRestart")
        super.onRestart()
    }

    override fun onResume() {
        Log.d(TAG, "onResume")
        super.onResume()
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

//    override fun onBackPressed() {
//        //super.onBackPressed()
//        //finish()
//    }

    private val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(webClientID)
        .requestEmail()
        .build()
    private val mGoogleSignInClient by lazy { GoogleSignIn.getClient(this, gso)  }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult: getting the results of Google authentication...")
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    Log.d(TAG, "Google authentication succeeded, passing token to Firebase...")
                    firebaseAuthWithGoogle(account.idToken!!)
                }
            } catch (e: ApiException) {
                Log.w(TAG, "Google sign in failed", e)
                startChat()
            }
        } else {
            Log.w(TAG, "Request code doesn't match.  Continuing without it...")
            startChat()
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    //val user = auth.currentUser
                    Log.d(TAG, "Firebase auth with Google: success")
                    startChat()
                } else {
                    Log.w(TAG, "Firebase auth with Google: failure", task.exception)
                    startChat()
                }
            }
    }

    private fun login() {
        if (!StorageManager.useGoogleAuth) {
            Log.d(TAG, "login: Google auth is not enabled, starting chat without it.")
            startChat()
        } else {
            auth = FirebaseAuth.getInstance()
            val signInIntent = mGoogleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
            Log.d(TAG, "login: Google auth is enabled, starting signInIntent..")
        }
    }

    fun showAlert(message: String) {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setMessage(message)
        dialogBuilder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        val alertDialog = dialogBuilder.create()
        alertDialog.show()
    }

    private fun startChat() {
        if (StorageManager.subscriptionLevel== 0){
            showAlert("No device subscription plan:" + StorageManager.deviceID)
            finish()
        } else {
            val intent = Intent(this, ChatActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }
    }
}