package com.nexmo.mobilecallerdemo

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.nexmo.mobilecallerdemo.connection.OTAction
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

    private lateinit var audioFocusRequest: AudioFocusRequest

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
                if (cbrIntent?.action == OTAction.REMOTE_REJECT) {
                    Log.d(RingActivity.TAG, "Call reject signal received")
                    endCall()
                }
            }
        }

        sendActionBroadcast(OTAction.LOCAL_ANSWER)

        btn_end_call.setOnClickListener { endCall() }

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
        intentFilter.addAction(OTAction.REMOTE_REJECT)
        registerReceiver(callBroadcastReceiver, intentFilter)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

        audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(audioAttributes)
            .setOnAudioFocusChangeListener { status -> Log.d(TAG, "AudioFocusChange $status") }
            .build()

        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.requestAudioFocus(audioFocusRequest)

        Log.d(TAG, "Audio Focus Requested")
    }

    override fun onPause() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.abandonAudioFocusRequest(audioFocusRequest)
        Log.d(TAG, "Audio Focus Abandoned")

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
                sendActionBroadcast(OTAction.LOCAL_HANGUP)
                videoCallService.endSession()
            }

            override fun onRemoteHangup() {
                Log.d(TAG, "OnRemoteHangup")
                sendActionBroadcast(OTAction.REMOTE_HANGUP)
                videoCallService.endSession()
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
