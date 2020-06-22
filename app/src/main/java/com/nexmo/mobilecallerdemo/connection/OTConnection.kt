package com.nexmo.mobilecallerdemo.connection

import android.content.Intent
import android.os.Bundle
import android.telecom.CallAudioState
import android.telecom.Connection
import android.telecom.TelecomManager
import android.util.Log
import com.nexmo.mobilecallerdemo.CallActivity
import com.nexmo.mobilecallerdemo.notification.IncomingCallRequest
import com.nexmo.mobilecallerdemo.notification.NotificationService

class OTConnection(
    private val connectionService: OTConnectionService,
    private val notificationService: NotificationService,
    private val incomingCall: Boolean,
    private val localMobileNumber: String,
    private val remoteMobileNumber: String,
    private val apiKey: String,
    private val sessionId: String,
    private val token: String
) : Connection() {
    companion object {
        const val TAG = "OTConnection"

        const val EXTRA_FROM = "from"
        const val EXTRA_API_KEY = "apiKey"
        const val EXTRA_SESSION_ID = "sessionId"
        const val EXTRA_TOKEN = "token"
    }

    private var incomingCallUiShowing = false

    init {
        this.connectionProperties = Connection.PROPERTY_SELF_MANAGED
        this.setCallerDisplayName(remoteMobileNumber, TelecomManager.PRESENTATION_ALLOWED)
    }

    override fun onCallAudioStateChanged(state: CallAudioState?) {
        Log.d(TAG, "Call Audio State Changed - ${state?.isMuted}")
        super.onCallAudioStateChanged(state)
    }

    override fun onAnswer(videoState: Int) {
        Log.d(TAG, "OnAnswer - $videoState")
        super.onAnswer(videoState)
    }

    override fun onAnswer() {
        Log.d(TAG, "OnAnswer")
        super.onAnswer()
    }

    override fun onDisconnect() {
        Log.d(TAG, "OnDisconnect")
        super.onDisconnect()
    }

    override fun onAbort() {
        Log.d(TAG, "OnAbort")
        super.onAbort()
    }

    override fun onCallEvent(event: String?, extras: Bundle?) {
        Log.d(TAG, "OnCallEvent $event")
        super.onCallEvent(event, extras)
    }

    override fun onHold() {
        Log.d(TAG, "OnHold")
        super.onHold()
    }

    override fun onShowIncomingCallUi() {
        Log.d(TAG, "OnShowIncomingCallUi")

        // Ring Notification
        val incomingCallRequest = IncomingCallRequest(remoteMobileNumber, apiKey, sessionId, token)
        val ringNotification = notificationService.createRingingNotification(incomingCallRequest)
        connectionService.startForeground(OTConnectionService.FOREGROUND_NOTIFICATION_ID, ringNotification)

        super.onShowIncomingCallUi()
    }

    override fun onUnhold() {
        Log.d(TAG, "OnUnhold")
        super.onUnhold()
    }

    override fun onStateChanged(state: Int) {
        Log.d(TAG, "OnStateChanged - $state")
        when (state) {
            STATE_ACTIVE -> {
                Log.d(TAG, "State: Active")
                val callIntent = Intent(connectionService, CallActivity::class.java)
                callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                callIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

                callIntent.putExtra(EXTRA_FROM, remoteMobileNumber)
                callIntent.putExtra(EXTRA_API_KEY, apiKey)
                callIntent.putExtra(EXTRA_SESSION_ID, sessionId)
                callIntent.putExtra(EXTRA_TOKEN, token)

                connectionService.startActivity(callIntent)
            }
            STATE_RINGING -> {
                Log.d(TAG, "State: Ringing")
            }
            STATE_DIALING -> {
                Log.d(TAG, "State: Dialing")
            }
            STATE_DISCONNECTED -> {
                Log.d(TAG, "State: Disconnected")
            }
        }
        super.onStateChanged(state)
    }

    override fun onReject() {
        Log.d(TAG, "OnReject")
        super.onReject()
    }

    override fun onReject(replyMessage: String?) {
        Log.d(TAG, "OnReject - $replyMessage")
        super.onReject(replyMessage)
    }

    fun setIncomingCallUiShowing(showing: Boolean) {
        Log.d(TAG, "Setting IsIncomingCall UI Showing to $showing")
        incomingCallUiShowing = showing

        if (!showing) {
            connectionService.stopForeground(true)
        }
    }

    fun isIncomingCall(): Boolean {
        return incomingCall
    }

    private fun sendActionBroadcast(action: String) {
        val intent = Intent()
        intent.action = action
        connectionService.sendBroadcast(intent)
    }
}