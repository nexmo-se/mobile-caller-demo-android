package com.nexmo.mobilecallerdemo

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.nexmo.mobilecallerdemo.api.ApiService
import com.nexmo.mobilecallerdemo.connection.OTAction
import com.nexmo.mobilecallerdemo.connection.OTConnectionService
import com.nexmo.mobilecallerdemo.notification.IncomingCallRequest
import kotlinx.android.synthetic.main.activity_ring.*

class RingActivity : AppCompatActivity() {
    companion object {
        const val TAG = "RingActivity"

        const val FROM = "from"
        const val API_KEY = "apiKey"
        const val SESSION_ID = "sessionId"
        const val TOKEN = "token"
    }

    private lateinit var from: String
    private lateinit var apiKey: String
    private lateinit var sessionId: String
    private lateinit var token: String

    private lateinit var ringtone: Ringtone

    private lateinit var apiService: ApiService


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ring)

        apiService = ApiService()

        val incomingCallRequest = createIncomingCallRequest(intent)
        sendActionBroadcast(OTAction.SHOW_RINGER, incomingCallRequest)

        // Setup Views
        from = incomingCallRequest.from
        apiKey = incomingCallRequest.apiKey
        sessionId = incomingCallRequest.sessionId
        token = incomingCallRequest.token

        Log.d(TAG, "From: $from")
        Log.d(TAG, "OT API Key: $apiKey")
        Log.d(TAG, "OT Session ID: $sessionId")
        Log.d(TAG, "OT Token: $token")

        tv_from.text = from

        btn_accept.setOnClickListener {
            Toast.makeText(this, "Accepted", Toast.LENGTH_SHORT).show()
            sendActionBroadcast(OTAction.LOCAL_ANSWER, incomingCallRequest)
            finish()
        }

        btn_reject.setOnClickListener {
            Toast.makeText(this, "Rejected", Toast.LENGTH_SHORT).show()
            sendActionBroadcast(OTAction.LOCAL_REJECT, incomingCallRequest)
            finish()
        }

        showOnLock()
    }

    override fun onResume() {
        super.onResume()

        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        ringtone = RingtoneManager.getRingtone(this, uri)
        ringtone.play()
    }

    override fun onPause() {
        ringtone.stop()
        super.onPause()
    }

    private fun showOnLock() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            Log.d(TAG, "After Android O_MR1")
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyManager.requestDismissKeyguard(this, null)
        } else {
            Log.d(TAG, "Before Android O_MR1")
            this.window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_FULLSCREEN or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    private fun createIncomingCallRequest(intent: Intent): IncomingCallRequest {
        val from = intent.getStringExtra(FROM) ?: ""
        val apiKey = intent.getStringExtra(API_KEY) ?: ""
        val sessionId = intent.getStringExtra(SESSION_ID) ?: ""
        val token = intent.getStringExtra(TOKEN) ?: ""

        return IncomingCallRequest(from, apiKey, sessionId, token)
    }

    private fun sendActionBroadcast(action: String, incomingCallRequest: IncomingCallRequest) {
        val intent = Intent()
        intent.action = action

        intent.putExtra(FROM, incomingCallRequest.from)
        intent.putExtra(API_KEY, incomingCallRequest.apiKey)
        intent.putExtra(SESSION_ID, incomingCallRequest.sessionId)
        intent.putExtra(TOKEN, incomingCallRequest.token)

        sendBroadcast(intent)
    }
}
