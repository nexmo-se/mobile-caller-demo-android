package com.nexmo.mobilecallerdemo.connection

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.telecom.*
import android.util.Log
import android.widget.Toast
import com.nexmo.mobilecallerdemo.api.ApiService
import com.nexmo.mobilecallerdemo.api.OutgoingCallRequest
import com.nexmo.mobilecallerdemo.notification.IncomingCallRequest
import com.nexmo.mobilecallerdemo.notification.NotificationService
import com.nexmo.mobilecallerdemo.persistence.PersistenceService
import org.json.JSONObject

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


        // Start a call
        val outgoingCallRequest = makeOpentokCall(to)

        // Create Connection
        val mobileNumber = persistenceService.getMobileNumber() ?: "Unknown"
        val otConnection = OTConnection(
            this,
            notificationService,
            false,
            mobileNumber,
            outgoingCallRequest.to,
            outgoingCallRequest.apiKey,
            outgoingCallRequest.sessionId,
            outgoingCallRequest.token
        )

        connections[outgoingCallRequest.to] = otConnection
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
            }
            OTAction.LOCAL_REJECT -> {
                Log.d(TAG, "Notification Action: $action")
                connection?.setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
                connection?.setIncomingCallUiShowing(false)
            }
            OTAction.LOCAL_HANGUP -> {
                Log.d(TAG, "Notification Action: $action")
                connection?.setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
            }
            OTAction.REMOTE_ANSWER -> {
                Log.d(TAG, "Notification Action: $action")
                connection?.setActive()
            }
            OTAction.REMOTE_REJECT -> {
                Log.d(TAG, "Notification Action: $action")
                connection?.setDisconnected(DisconnectCause(DisconnectCause.REJECTED))
            }
            OTAction.REMOTE_HANGUP -> {
                Log.d(TAG, "Notification Action: $action")
                connection?.setDisconnected(DisconnectCause(DisconnectCause.REMOTE))
            }
        }
    }

    private fun makeOpentokCall(to: String): OutgoingCallRequest {
        val from = persistenceService.getMobileNumber() ?: "Unknown"
        val response = apiService.call(from, to)
        Log.d(TAG, "Call made to server")
        val responseObject = JSONObject(response)

        val apiKey = responseObject.getString("apiKey")
        val sessionId = responseObject.getString("sessionId")
        val token = responseObject.getString("token")

        return OutgoingCallRequest(to, apiKey, sessionId, token)
    }
}