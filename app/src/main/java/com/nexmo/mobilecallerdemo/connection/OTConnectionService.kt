package com.nexmo.mobilecallerdemo.connection

import android.content.Intent
import android.telecom.*
import android.util.Log

class OTConnectionService : ConnectionService() {
    companion object {
        const val TAG = "OTConnectionService"
    }

    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection {
        Log.d(TAG, "OnCreateIncomingConnection ${connectionManagerPhoneAccount?.id}")
        Log.d(TAG, request?.extras.toString())
        return OTConnection(true)
    }

    override fun onCreateOutgoingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection {
        Log.d(TAG, "OnCreateOutgoingConnection ${connectionManagerPhoneAccount?.id}")
        Log.d(TAG, request?.extras.toString())
        return OTConnection(false)
    }

    override fun onCreateIncomingConnectionFailed(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ) {
        Log.d(TAG, "OnCreateIncomingConnectionFailed ${connectionManagerPhoneAccount?.id}")
        Log.d(TAG, request?.extras.toString())
        super.onCreateIncomingConnectionFailed(connectionManagerPhoneAccount, request)
    }

    override fun onCreateOutgoingConnectionFailed(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ) {
        Log.d(TAG, "OnCreateOutGoingConnectionFailed ${connectionManagerPhoneAccount?.id}")
        Log.d(TAG, request?.extras.toString())
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

}