package com.nexmo.mobilecallerdemo.connection

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.telecom.*
import android.util.Log
import android.widget.Toast
import com.nexmo.mobilecallerdemo.CallActivity
import com.nexmo.mobilecallerdemo.api.ApiService
import com.nexmo.mobilecallerdemo.notification.NotificationService
import com.nexmo.mobilecallerdemo.persistence.PersistenceService

class OTConnectionService : ConnectionService() {
    companion object {
        const val TAG = "OTConnectionService"

        const val EXTRA_FROM = "from"
        const val EXTRA_API_KEY = "apiKey"
        const val EXTRA_SESSION_ID = "sessionId"
        const val EXTRA_TOKEN = "token"

        const val FOREGROUND_NOTIFICATION_ID = 10
    }

    private val actions = listOf(
        OTAction.INCOMING_CALL,
        OTAction.SHOW_RINGER,
        OTAction.LOCAL_ANSWER,
        OTAction.LOCAL_REJECT,
        OTAction.LOCAL_HANGUP,
        OTAction.REMOTE_ANSWER,
        OTAction.REMOTE_REJECT,
        OTAction.REMOTE_HANGUP
    )

    private lateinit var apiService: ApiService
    private lateinit var notificationService: NotificationService
    private lateinit var persistenceService: PersistenceService
    private lateinit var otPhone: OTPhone
    private lateinit var broadcastReceiver: BroadcastReceiver

    private val connections = HashMap<String, OTConnection>()

    override fun onCreate() {
        super.onCreate()

        apiService = ApiService()
        notificationService = NotificationService(this)
        persistenceService = PersistenceService(this)
        otPhone = OTPhone(this)

        engageReceiver()
    }

    override fun onDestroy() {
        disengageReceiver()
        super.onDestroy()
    }

    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection {
        Log.d(TAG, "OnCreateIncomingConnection ${connectionManagerPhoneAccount?.id}")
        Log.d(TAG, "Extra: ${request?.extras.toString()}")
        Log.d(TAG, "Address ${request?.address.toString()}")
        val address = request?.address.toString()

        val from = address.replace("tel:+", "")
        Log.d(TAG, "Phone Number [From] $from")

        val apiKey = request?.extras?.getString(EXTRA_API_KEY) ?: ""
        val sessionId = request?.extras?.getString(EXTRA_SESSION_ID) ?: ""
        val token = request?.extras?.getString(EXTRA_TOKEN) ?: ""

        // Create Connection
        val mobileNumber = persistenceService.getMobileNumber() ?: "Unknown"
        val otConnection = OTConnection(
            this,
            notificationService,
            true,
            mobileNumber,
            from,
            apiKey,
            sessionId,
            token
        )

        otConnection.setVideoState(VideoProfile.STATE_BIDIRECTIONAL)

        otConnection.setRinging()
        otConnection.setIncomingCallUiShowing(true)
        connections[from] = otConnection

        return otConnection
    }

    override fun onCreateOutgoingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection {
        Log.d(TAG, "OnCreateOutgoingConnection ${connectionManagerPhoneAccount?.id}")
        Log.d(TAG, "Extra: ${request?.extras.toString()}")
        Log.d(TAG, "Address ${request?.address.toString()}")

        val address = request?.address.toString()
        val to = address.replace("tel:+", "")
        Log.d(TAG, "Phone Number [To] $to")

        val apiKey = request?.extras?.getString(EXTRA_API_KEY) ?: ""
        val sessionId = request?.extras?.getString(EXTRA_SESSION_ID) ?: ""
        val token = request?.extras?.getString(EXTRA_TOKEN) ?: ""

        // Create Connection
        val mobileNumber = persistenceService.getMobileNumber() ?: "Unknown"
        val otConnection = OTConnection(
            this,
            notificationService,
            false,
            mobileNumber,
            to,
            apiKey,
            sessionId,
            token
        )

        makeOpentokCall(to, sessionId)

        otConnection.setVideoState(VideoProfile.STATE_BIDIRECTIONAL)

        otConnection.setDialing()
        connections[to] = otConnection
        return otConnection
    }

    override fun onCreateIncomingConnectionFailed(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ) {
        Log.d(TAG, "OnCreateIncomingConnectionFailed ${connectionManagerPhoneAccount?.id}")
        Log.d(TAG, "Extra: ${request?.extras.toString()}")
        Log.d(TAG, "Address ${request?.address.toString()}")

        // Show Notification
        val address = request?.address.toString()
        val from = address.replace("tel:+", "")
        Log.d(TAG, "Phone Number [From] $from")
        notificationService.showNotification(this, "Missed call: $from")

        super.onCreateIncomingConnectionFailed(connectionManagerPhoneAccount, request)
    }

    override fun onCreateOutgoingConnectionFailed(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ) {
        Log.d(TAG, "OnCreateOutGoingConnectionFailed ${connectionManagerPhoneAccount?.id}")
        Log.d(TAG, "Extra: ${request?.extras.toString()}")
        Log.d(TAG, "Address ${request?.address.toString()}")

        // Show Toast
        Toast.makeText(this, "Failed to create outgoing connection", Toast.LENGTH_SHORT).show()

        super.onCreateOutgoingConnectionFailed(connectionManagerPhoneAccount, request)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "OnUnbind")
        return super.onUnbind(intent)
    }

    override fun onConnectionServiceFocusLost() {
        Log.d(TAG, "OnConnectionServiceFocusLost")
        super.onConnectionServiceFocusLost()
    }

    override fun onConnectionServiceFocusGained() {
        Log.d(TAG, "OnConnectionServiceFocusGained")
        dropAudioFocus()
        super.onConnectionServiceFocusGained()
    }

    override fun onRemoteExistingConnectionAdded(connection: RemoteConnection?) {
        Log.d(TAG, "OnRemoteExistingConnectionAdded ${connection?.address.toString()}")
        super.onRemoteExistingConnectionAdded(connection)
    }

    private fun engageReceiver() {
        // Create Broadcast Receiver
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(cbrContext: Context?, cbrIntent: Intent?) {
                Log.d(TAG, "Broadcast Received: ${cbrIntent?.action ?: "Unknown action"}")
                handleAction(cbrIntent)
            }
        }

        // Create Intent Filter
        val intentFilter = IntentFilter()
        for (action in actions) {
            intentFilter.addAction(action)
        }

        // Register Broadcast Receiver
        registerReceiver(broadcastReceiver, intentFilter)

        Log.d(TAG, "Broadcast Receiver Registered")
    }

    private fun disengageReceiver() {
        // Unregister Broadcast Receiver
        unregisterReceiver(broadcastReceiver)

        Log.d(TAG, "Broadcast Receiver Unregistered")
    }


    private fun handleAction(intent: Intent?) {
        if (intent == null) {
            Log.e(TAG, "Null Intent")
            return
        }

        val action = intent.action
        val from = intent.getStringExtra(EXTRA_FROM) ?: ""
        val connection = connections[from]

        when (action) {
            OTAction.LOCAL_ANSWER -> {
                Log.d(TAG, "Notification Action: $action")
                connection?.setActive()
                connection?.setIncomingCallUiShowing(false)
                answerOpentokCall(from)
            }
            OTAction.LOCAL_REJECT -> {
                Log.d(TAG, "Notification Action: $action")
                connection?.setDisconnected(DisconnectCause(DisconnectCause.REJECTED))
                connection?.setIncomingCallUiShowing(false)
                connection?.destroy()
                rejectOpentokCall(from)
            }
            OTAction.LOCAL_HANGUP -> {
                Log.d(TAG, "Notification Action: $action")
                connection?.setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
                connection?.destroy()
            }
            OTAction.REMOTE_ANSWER -> {
                Log.d(TAG, "Notification Action: $action")
                connection?.setActive()
                Log.d(TAG, "Set Active")
            }
            OTAction.REMOTE_REJECT -> {
                Log.d(TAG, "Notification Action: $action")
                connection?.setDisconnected(DisconnectCause(DisconnectCause.REJECTED))
                connection?.destroy()
            }
            OTAction.REMOTE_HANGUP -> {
                Log.d(TAG, "Notification Action: $action")
                connection?.setDisconnected(DisconnectCause(DisconnectCause.REMOTE))
                connection?.destroy()
            }
        }
    }

    private fun makeOpentokCall(to: String, sessionId: String) {
        val runnable = Runnable {
            val from = persistenceService.getMobileNumber() ?: "Unknown"
            apiService.notifyCallee(from, to, sessionId)
            Log.d(TAG, "Callee notified")
        }
        val thread = Thread(runnable)
        thread.start()
    }

    private fun answerOpentokCall(from: String) {
        val runnable = Runnable {
            apiService.answerCall(from)
            Log.d(TAG, "Call answered")
        }
        val thread = Thread(runnable)
        thread.start()
    }

    private fun rejectOpentokCall(from: String) {
        val runnable = Runnable {
            apiService.rejectCall(from)
            Log.d(TAG, "Call rejected")
        }
        val thread = Thread(runnable)
        thread.start()
    }

    private fun dropAudioFocus() {
        Log.d(TAG, "Dropping AudioFocus")
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val result = audioManager.abandonAudioFocus { audioFocus -> Log.d(OTConnection.TAG, "AudioFocus Changed to $audioFocus") }
        Log.d(TAG, "AudioFocus Dropped - $result")
    }
}