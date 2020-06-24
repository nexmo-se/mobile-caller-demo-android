package com.nexmo.mobilecallerdemo.push

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.nexmo.mobilecallerdemo.api.ApiService
import me.pushy.sdk.Pushy
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class PushService(private val context: Context, private val apiService: ApiService) {
    companion object {
        const val TAG = "PushService"
    }

    fun init(mobileNumber: String) {
        val runnable = Runnable {
            if (!Pushy.isRegistered(context)) {
                // Register Pushy
                Log.d(TAG, "Pushy Registering")
                val token = Pushy.register(context)
                Log.d(TAG, "Pushy Registered")

                Log.d(TAG, "Token1 $token")

                Log.d(TAG, "Saving to Server")
                apiService.registerToken(mobileNumber, token)
                Log.d(TAG, "Saved to Server")
            }

            val token2 = Pushy.getDeviceCredentials(context).token
            Log.d(TAG, "Token2 $token2")

            Pushy.listen(context)
            Log.d(TAG, "Pushy Listening")
        }
        val thread = Thread(runnable)
        thread.start()
    }

    fun destroy(mobileNumber: String) {
        val runnable = Runnable {
            Log.d(TAG, "Pushy Unregistering")
            Pushy.unregister(context)
            Log.d(TAG, "Pushy Unregistered")

            Log.d(TAG, "Clearing from Server")
            apiService.unregisterToken(mobileNumber)
            Log.d(TAG, "Cleared from Server")
        }

        val thread = Thread(runnable)
        thread.start()
    }
}