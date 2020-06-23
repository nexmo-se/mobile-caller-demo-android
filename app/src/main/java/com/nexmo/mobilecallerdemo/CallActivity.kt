package com.nexmo.mobilecallerdemo

import android.app.KeyguardManager
import android.content.*
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.nexmo.mobilecallerdemo.connection.OTAction
import com.nexmo.mobilecallerdemo.connection.OTConnectionService
import com.nexmo.mobilecallerdemo.connection.OTPhone
import com.nexmo.mobilecallerdemo.connection.OTServiceConnection
import com.nexmo.mobilecallerdemo.opentok.CustomAudioDevice
import com.nexmo.mobilecallerdemo.opentok.VideoCallService
import com.nexmo.mobilecallerdemo.opentok.VideoListener
import com.opentok.android.AudioDeviceManager
import com.opentok.android.BaseAudioDevice
import kotlinx.android.synthetic.main.activity_call.*

class CallActivity : AppCompatActivity() {
    companion object {
        const val TAG = "CallActivity"

        const val FROM = "from"
        const val API_KEY = "apiKey"
        const val SESSION_ID = "sessionId"
        const val TOKEN = "token"
        const val DIRECTION = "direction"

    }

    private lateinit var videoCallService: VideoCallService
    private lateinit var otPhone: OTPhone

    private lateinit var publisherViewContainer: FrameLayout
    private lateinit var subscriberViewContainer: FrameLayout

    private lateinit var videoListener: VideoListener
    private lateinit var callBroadcastReceiver: BroadcastReceiver
    private lateinit var audioDevice: BaseAudioDevice

    private lateinit var from: String
    private lateinit var apiKey: String
    private lateinit var sessionId: String
    private lateinit var token: String
    private lateinit var direction: String

    private lateinit var serviceConnection: OTServiceConnection

    private var hasEnded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)

        from = intent.getStringExtra(FROM) ?: ""
        apiKey = intent.getStringExtra(API_KEY) ?: ""
        sessionId = intent.getStringExtra(SESSION_ID) ?: ""
        token = intent.getStringExtra(TOKEN) ?: ""
        direction = intent.getStringExtra(DIRECTION) ?: ""

        // initialize view objects from your layout
        publisherViewContainer = findViewById(R.id.publisher_container)
        subscriberViewContainer = findViewById(R.id.subscriber_container)

        callBroadcastReceiver = object: BroadcastReceiver() {
            override fun onReceive(cbrContext: Context?, cbrIntent: Intent?) {
                if (cbrIntent?.action == OTAction.REMOTE_REJECT) {
                    Log.d(RingActivity.TAG, "Call reject signal received")
                    endCall()
                }
            }
        }

        if (direction == "incoming") {
            sendActionBroadcast(OTAction.LOCAL_ANSWER)
        }

        btn_end_call.setOnClickListener { endCall() }

        // Use Custom Audio Driver
        audioDevice = CustomAudioDevice(this)
        AudioDeviceManager.setAudioDevice(audioDevice)

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
        Log.d(TAG, "OnResume")
        super.onResume()

        val intentFilter = IntentFilter()
        intentFilter.addAction(OTAction.REMOTE_REJECT)
        registerReceiver(callBroadcastReceiver, intentFilter)

        val intent = Intent()
        intent.setClass(this, OTConnectionService::class.java)
        serviceConnection = OTServiceConnection()
        bindService(intent, serviceConnection, 0)
    }

    override fun onPause() {
        Log.d(TAG, "OnPause")
        unregisterReceiver(callBroadcastReceiver)

        unbindService(serviceConnection)

        super.onPause()
    }

    private fun joinCall() {
        videoListener = object: VideoListener {
            override fun onCallStart() {
                Log.d(TAG, "OnCallStart")
            }

            override fun onCallEnd() {
                Log.d(TAG, "OnCallEnd")
                finish()
            }

            override fun onLocalJoin() {
                Log.d(TAG, "OnLocalJoin")
            }

            override fun onRemoteJoin() {
                Log.d(TAG, "OnRemoteJoin")
                sendActionBroadcast(OTAction.REMOTE_ANSWER)
            }

            override fun onLocalHangup() {
                Log.d(TAG, "OnLocalHangup")
                if (!hasEnded) {
                    sendActionBroadcast(OTAction.LOCAL_HANGUP)
                    videoCallService.endSession()
                    hasEnded = true
                }
            }

            override fun onRemoteHangup() {
                Log.d(TAG, "OnRemoteHangup")
                if (!hasEnded) {
                    sendActionBroadcast(OTAction.REMOTE_HANGUP)
                    videoCallService.endSession()
                    hasEnded = true
                }
            }
        }
        videoCallService.init(
            publisherViewContainer, subscriberViewContainer, apiKey, sessionId, token, videoListener
        )
    }


    private fun endCall() {
        videoCallService.unpublish()
    }

    private fun sendActionBroadcast(action: String) {
        val intent = Intent()
        intent.action = action

        intent.putExtra(FROM, from)
        intent.putExtra(API_KEY, apiKey)
        intent.putExtra(SESSION_ID, sessionId)
        intent.putExtra(TOKEN, token)

        sendBroadcast(intent)
    }
}
