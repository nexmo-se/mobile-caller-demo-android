package com.nexmo.mobilecallerdemo

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.nexmo.mobilecallerdemo.connection.OTAction
import com.nexmo.mobilecallerdemo.connection.OTPhone
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

        const val PUSHY_ACTION_INCOMING_CALL = "ACTION_INCOMING_CALL"
        const val PUSHY_ACTION_ANSWER_CALL = "ACTION_ANSWER_CALL"
        const val PUSHY_ACTION_REJECT_CALL = "ACTION_REJECT_CALL"
    }

    private lateinit var otPhone: OTPhone

    override fun onReceive(context: Context, intent: Intent) {
        otPhone = OTPhone(context)

        when (intent.getStringExtra(NOTIFICATION_ACTION)) {
            PUSHY_ACTION_INCOMING_CALL -> {
                Log.d(TAG, "Incoming Call received")

                val from = intent.getStringExtra(NOTIFICATION_FROM) ?: "Unknown caller"
                val apiKey = intent.getStringExtra(NOTIFICATION_API_KEY) ?: ""
                val sessionId = intent.getStringExtra(NOTIFICATION_SESSION_ID) ?: ""
                val token = intent.getStringExtra(NOTIFICATION_TOKEN) ?: ""

                otPhone.callIn(from, apiKey, sessionId, token)
            }
            PUSHY_ACTION_ANSWER_CALL -> {
                Log.d(TAG, "Answer Call received")

                val newIntent = createBroadcastIntent(OTAction.REMOTE_ANSWER, intent)
                context.sendBroadcast(newIntent)
            }
            PUSHY_ACTION_REJECT_CALL -> {
                Log.d(TAG, "Reject Call received")

                val newIntent = createBroadcastIntent(OTAction.REMOTE_REJECT, intent)
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

    private fun createBroadcastIntent(action: String, intent: Intent): Intent {
        val newIntent = Intent()
        newIntent.action = action

        newIntent.putExtra(NOTIFICATION_FROM, intent.getStringExtra(NOTIFICATION_FROM))
        newIntent.putExtra(NOTIFICATION_API_KEY, intent.getStringExtra(NOTIFICATION_API_KEY))
        newIntent.putExtra(NOTIFICATION_SESSION_ID, intent.getStringExtra(NOTIFICATION_SESSION_ID))
        newIntent.putExtra(NOTIFICATION_TOKEN,intent.getStringExtra(NOTIFICATION_TOKEN))

        return newIntent;
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
