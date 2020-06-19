package com.nexmo.mobilecallerdemo

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.nexmo.mobilecallerdemo.api.ApiService
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

    private lateinit var apiService: ApiService


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ring)

        apiService = ApiService()

        from = intent.getStringExtra(FROM) ?: "Unknown"
        apiKey = intent.getStringExtra(API_KEY) ?: ""
        sessionId = intent.getStringExtra(SESSION_ID) ?: ""
        token = intent.getStringExtra(TOKEN) ?: ""

        Log.d(TAG, intent.dataString ?: "null")
        Log.d(TAG, "From: $from")
        Log.d(TAG, "OT API Key: $apiKey")
        Log.d(TAG, "OT Session ID: $sessionId")
        Log.d(TAG, "OT Token: $token")

        tv_from.text = from

        btn_accept.setOnClickListener {
            Toast.makeText(this, "Accepted", Toast.LENGTH_SHORT).show()

            val newIntent = Intent()
            newIntent.setClass(this, CallActivity::class.java)

            newIntent.putExtra(FROM, from)
            newIntent.putExtra(API_KEY, apiKey)
            newIntent.putExtra(SESSION_ID, sessionId)
            newIntent.putExtra(TOKEN, token)

            startActivity(newIntent)
            finish()
        }

        btn_reject.setOnClickListener {
            Toast.makeText(this, "Rejected", Toast.LENGTH_SHORT).show()
            val runnable = Runnable {
                apiService.rejectCall(from)
                Log.d(TAG, "Reject Call Signal sent")

                val uiRunnable = Runnable { finish() }
                runOnUiThread(uiRunnable)
            }
            val thread = Thread(runnable)
            thread.start()
        }

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
}
