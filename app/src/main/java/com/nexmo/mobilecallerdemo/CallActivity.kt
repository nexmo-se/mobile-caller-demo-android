package com.nexmo.mobilecallerdemo

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.nexmo.mobilecallerdemo.connection.OTPhone
import com.nexmo.mobilecallerdemo.opentok.VideoCallService
import com.nexmo.mobilecallerdemo.opentok.VideoListener
import kotlinx.android.synthetic.main.activity_call.*

class CallActivity : AppCompatActivity() {
    companion object {
        const val TAG = "CallActivity"

        const val FROM = "from"
        const val API_KEY = "apiKey"
        const val SESSION_ID = "sessionId"
        const val TOKEN = "token"

        const val ACTION_ANSWER_CALL = "ACTION_ANSWER_CALL"
        const val ACTION_REJECT_CALL = "ACTION_REJECT_CALL"
    }

    private lateinit var videoCallService: VideoCallService
    private lateinit var otPhone: OTPhone

    private lateinit var publisherViewContainer: FrameLayout
    private lateinit var subscriberViewContainer: FrameLayout

    private lateinit var videoListener: VideoListener
    private lateinit var callBroadcastReceiver: BroadcastReceiver

    private lateinit var from: String
    private lateinit var apiKey: String
    private lateinit var sessionId: String
    private lateinit var token: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)

        from = intent.getStringExtra(FROM) ?: ""
        apiKey = intent.getStringExtra(API_KEY) ?: ""
        sessionId = intent.getStringExtra(SESSION_ID) ?: ""
        token = intent.getStringExtra(TOKEN) ?: ""

        // initialize view objects from your layout
        publisherViewContainer = findViewById(R.id.publisher_container)
        subscriberViewContainer = findViewById(R.id.subscriber_container)

        callBroadcastReceiver = object: BroadcastReceiver() {
            override fun onReceive(cbrContext: Context?, cbrIntent: Intent?) {
                if (!cbrIntent?.action.isNullOrEmpty() && cbrIntent?.action == ACTION_REJECT_CALL) {
                    Log.d(RingActivity.TAG, "Call reject signal received")
                    endCall()
                }
            }
        }

        val callAnswerIntent = Intent(ACTION_ANSWER_CALL)
        sendBroadcast(callAnswerIntent)

        btn_end_call.setOnClickListener {
            endCall()
        }

        otPhone = OTPhone(this)
        videoCallService = VideoCallService(this)


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

        joinCall()
    }

    override fun onResume() {
        super.onResume()

        val intentFilter = IntentFilter()
        intentFilter.addAction(ACTION_REJECT_CALL)
        registerReceiver(callBroadcastReceiver, intentFilter)
    }

    override fun onPause() {
        unregisterReceiver(callBroadcastReceiver)
        super.onPause()
    }

    private fun joinCall() {
        videoListener = object: VideoListener {
            override fun onCallStart() {
                Log.d(TAG, "OnCallStart")
            }

            override fun onCallEnd() {
                Log.d(TAG, "OnCallEnd")

                // TODO: Notify ConnectionService Disconnect
                finish()
            }

            override fun onLocalJoin() {
                Log.d(TAG, "OnLocalJoin")
            }

            override fun onRemoteJoin() {
                Log.d(TAG, "OnRemoteJoin")

                // TODO: Notify ConnectionService Active
                Log.d(TAG, "Call is now active")
            }
        }
        videoCallService.init(
            publisherViewContainer, subscriberViewContainer, apiKey, sessionId, token, videoListener
        )
    }


    private fun endCall() {
        videoCallService.endSession()
        finish()
    }
}
