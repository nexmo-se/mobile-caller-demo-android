package com.nexmo.mobilecallerdemo.connection

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Bundle
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.util.Log
import android.widget.Toast
import com.nexmo.mobilecallerdemo.R

class OTPhone(private val context: Context) {
    companion object {
        const val TAG = "OTPhone"

        const val CALL_PROVIDER_ID = "opentok_OTConnectionService_CALL_PROVIDER_ID"
        const val PHONE_ACCOUNT_LABEL = "Opentok Call Provider"
        const val PHONE_ACCOUNT_DESCRIPTION = "Opentok Video Call"
        const val URI_SCHEME = "tel"

        const val EXTRA_FROM = "from"
        const val EXTRA_API_KEY = "apiKey"
        const val EXTRA_SESSION_ID = "sessionId"
        const val EXTRA_TOKEN = "token"
    }

    private val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager

    private fun getPhoneUri(mobileNumber: String): Uri {
        val uriString = if (mobileNumber.startsWith("+")) {
            "$URI_SCHEME:$mobileNumber"
        } else {
            "$URI_SCHEME:+$mobileNumber"
        }

        return Uri.parse(uriString)
    }

    private fun getPhoneAccountHandle(): PhoneAccountHandle {
        return PhoneAccountHandle(
            ComponentName(context, OTConnectionService::class.java),
            CALL_PROVIDER_ID
        )
    }

    private fun getPhoneAccount(mobileNumber: String, handle: PhoneAccountHandle): PhoneAccount {
        val uri = getPhoneUri(mobileNumber)
        val capabilities = PhoneAccount.CAPABILITY_SELF_MANAGED or PhoneAccount.CAPABILITY_VIDEO_CALLING

        return PhoneAccount.builder(handle, PHONE_ACCOUNT_LABEL)
            .setAddress(uri)
            .setSubscriptionAddress(uri)
            .setCapabilities(capabilities)
            .setIcon(Icon.createWithResource(context, R.drawable.ic_launcher_foreground))
            .setHighlightColor(Color.RED)
            .setShortDescription(PHONE_ACCOUNT_DESCRIPTION)
            .setSupportedUriSchemes(listOf(URI_SCHEME))
            .build()
    }

    fun init(mobileNumber: String) {
        unregister()
        register(mobileNumber)
    }

    private fun register(mobileNumber: String) {
        val phoneAccountHandle = getPhoneAccountHandle()
        val phoneAccount = getPhoneAccount(mobileNumber, phoneAccountHandle)
        telecomManager.registerPhoneAccount(phoneAccount)
        Log.d(TAG, "Registered Phone Account")
    }

    private fun unregister() {
        val phoneAccountHandle = getPhoneAccountHandle()
        telecomManager.unregisterPhoneAccount(phoneAccountHandle)
        Log.d(TAG, "Unregistered Phone Account")
    }

    @SuppressLint("MissingPermission")
    fun callOut(mobileNumber: String) {
        val uri = getPhoneUri(mobileNumber)
        val phoneAccountHandle = getPhoneAccountHandle()
//        val outgoingCallPermitted = telecomManager.isOutgoingCallPermitted(phoneAccountHandle)
//
//        if (!outgoingCallPermitted) {
//            Log.w(TAG, "Outgoing call not permitted")
//            Toast.makeText(context, "Outgoing call not permitted", Toast.LENGTH_SHORT).show()
//            return
//        }

        val bundle = Bundle()
        bundle.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandle)

        Log.d(TAG, "Outgoing Call to $uri")
        telecomManager.placeCall(uri, bundle)
    }

    fun callIn(mobileNumber: String, apiKey: String, sessionId: String, token: String) {
        val uri = getPhoneUri(mobileNumber)
        val phoneAccountHandle = getPhoneAccountHandle()
//        val incomingCallPermitted = telecomManager.isIncomingCallPermitted(phoneAccountHandle)

//        if (!incomingCallPermitted) {
//            Log.w(TAG, "Incoming call not permitted")
//            Toast.makeText(context, "Incoming call not permitted", Toast.LENGTH_SHORT).show()
//            return
//        }

        val bundle = Bundle()
        bundle.putParcelable(TelecomManager.EXTRA_INCOMING_CALL_ADDRESS, uri)

        bundle.putString(EXTRA_FROM, mobileNumber)
        bundle.putString(EXTRA_API_KEY, apiKey)
        bundle.putString(EXTRA_SESSION_ID, sessionId)
        bundle.putString(EXTRA_TOKEN, token)

        Log.d(TAG, "Incoming Call from $uri")
        telecomManager.addNewIncomingCall(phoneAccountHandle, bundle)
    }
}