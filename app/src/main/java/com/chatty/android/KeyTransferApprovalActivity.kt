package com.chatty.android

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.chatty.android.etc.FirebaseManager
import com.chatty.android.etc.StorageManager
import kotlinx.coroutines.launch

class KeyTransferApprovalActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.key_transfer_approval_activity)

        val tvMessage = findViewById<TextView>(R.id.tvMessage)
        val btnAuthorize = findViewById<Button>(R.id.btnAuthorize)
        val btnDeny = findViewById<Button>(R.id.btnDeny)

        val deviceModel = StorageManager.pendingKeyTransferDeviceModel.ifEmpty { "Unknown device" }
        tvMessage.text = getString(R.string.key_transfer_message, deviceModel)

        btnAuthorize.setOnClickListener {
            btnAuthorize.isEnabled = false
            btnDeny.isEnabled = false
            lifecycleScope.launch {
                FirebaseManager.sendKeyTransferResponse()
                Toast.makeText(this@KeyTransferApprovalActivity, getString(R.string.key_transfer_approved), Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        btnDeny.setOnClickListener {
            btnAuthorize.isEnabled = false
            btnDeny.isEnabled = false
            lifecycleScope.launch {
                FirebaseManager.clearKeyTransferRequest()
                Toast.makeText(this@KeyTransferApprovalActivity, getString(R.string.key_transfer_denied), Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
