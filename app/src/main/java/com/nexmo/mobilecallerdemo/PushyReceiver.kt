package com.nexmo.mobilecallerdemo

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Bundle
import android.util.Log
import androidx.core.app.NotificationCompat
import me.pushy.sdk.Pushy


class PushyReceiver : BroadcastReceiver() {
    companion object {
        const val TAG = "PushyReceiver"

        const val NOTIFICATION_TITLE = "MobileCallerDemo"
        const val NOTIFICATION_TEXT = "text"

        const val NOTIFICATION_ACTION = "action"
        const val NOTIFICATION_FROM = "from"
        const val NOTIFICATION_API_KEY = "apiKey"
        const val NOTIFICATION_SESSION_ID = "sessionId"
        const val NOTIFICATION_TOKEN = "token"

        const val ACTION_INCOMING_CALL = "ACTION_INCOMING_CALL"
        const val ACTION_REJECT_CALL = "ACTION_REJECT_CALL"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.getStringExtra(NOTIFICATION_ACTION)) {
            ACTION_INCOMING_CALL -> {
                // Incoming Call
                val from = intent.getStringExtra(NOTIFICATION_FROM)
                val apiKey = intent.getStringExtra(NOTIFICATION_API_KEY)
                val sessionId = intent.getStringExtra(NOTIFICATION_SESSION_ID)
                val token = intent.getStringExtra(NOTIFICATION_TOKEN)

                Log.d(TAG, "Incoming Call received")
                Log.d(TAG, "From: $from")
                Log.d(TAG, "OT API Key: $apiKey")
                Log.d(TAG, "OT Session ID: $sessionId")
                Log.d(TAG, "OT Token: $token")

                val newIntent = Intent()
                newIntent.setClass(context, RingService::class.java)

                newIntent.putExtra(NOTIFICATION_FROM, from)
                newIntent.putExtra(NOTIFICATION_API_KEY, apiKey)
                newIntent.putExtra(NOTIFICATION_SESSION_ID, sessionId)
                newIntent.putExtra(NOTIFICATION_TOKEN, token)

                context.startForegroundService(newIntent)
            }
            ACTION_REJECT_CALL -> {
                Log.d(TAG, "Reject Call received")

                val newIntent = Intent(ACTION_REJECT_CALL)
                context.sendBroadcast(newIntent)
            }
            else -> {
                // General Notification
                val notificationText: String? = intent.getStringExtra(NOTIFICATION_TEXT)
                if (notificationText.isNullOrEmpty()) {
                    Log.d(TAG, "No showable push notification")
                    return
                }

                Log.d(TAG, "Displaying Notification")
                displayNotification(context, notificationText)
            }
        }
    }

    private fun displayNotification(context: Context, notificationText: String) {
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, HomeActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        val builder = NotificationCompat.Builder(context)
            .setAutoCancel(true)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(NOTIFICATION_TITLE)
            .setContentText(notificationText)
            .setLights(Color.RED, 1000, 1000)
            .setVibrate(longArrayOf(0, 400, 250, 400))
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setContentIntent(pendingIntent)
        Pushy.setNotificationChannel(builder, context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, builder.build())
    }
}
