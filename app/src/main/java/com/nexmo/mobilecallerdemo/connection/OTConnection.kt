package com.nexmo.mobilecallerdemo.connection

import android.os.Bundle
import android.telecom.CallAudioState
import android.telecom.Connection
import android.telecom.DisconnectCause
import android.util.Log

class OTConnection(private val incomingCall: Boolean): Connection() {
    companion object {
        const val TAG = "OTConnection"
    }

    private var incomingCallUiShowing = false

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
        super.onShowIncomingCallUi()
    }

    override fun onUnhold() {
        Log.d(TAG, "OnUnhold")
        super.onUnhold()
    }

    override fun onStateChanged(state: Int) {
        Log.d(TAG, "OnStateChanged - $state")
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

    fun setConnectionActive() {
        Log.d(TAG, "Setting Connection to Active")
        setActive()

        // TODO: Call connected now, should now attempt to join OpenTok Session
    }

    fun setConnectionHold() {
        Log.d(TAG, "Setting Connection to Hold")
        setOnHold()

        // TODO: Call on hold now, should either unsubscribe/unpublish or mute audio and video
    }

    fun setConnectionDisconnected(cause: Int) {
        val disconnectionCause = DisconnectCause(cause)
        Log.d(TAG, "Setting Connection to Disconnected - ${disconnectionCause.reason}")
        setDisconnected(disconnectCause)

        // TODO: Call disconnected now, should disconnect from OpenTok Session
    }

    fun setIncomingCallUiShowing(showing: Boolean) {
        Log.d(TAG, "Setting IsIncomingCall UI Showing to $showing")
        incomingCallUiShowing = showing
    }

    fun isIncomingCall(): Boolean {
        return incomingCall
    }
}