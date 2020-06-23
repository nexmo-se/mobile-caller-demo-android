package com.nexmo.mobilecallerdemo.connection

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log

class OTServiceConnection: ServiceConnection {
    companion object {
        const val TAG = "OTServiceConnection"
    }

    override fun onServiceConnected(componentName: ComponentName?, binder: IBinder?) {
        Log.d(TAG, "Service Connected - ${componentName?.flattenToString() ?: "Unknown Component Name"}")
    }

    override fun onServiceDisconnected(componentName: ComponentName?) {
        Log.d(TAG, "Service Disconnected - ${componentName?.flattenToString() ?: "Unknown Component Name"}")
    }
}