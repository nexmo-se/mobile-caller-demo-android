package com.nexmo.mobilecallerdemo.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.nexmo.mobilecallerdemo.HomeActivity
import com.nexmo.mobilecallerdemo.PushyReceiver
import com.nexmo.mobilecallerdemo.R
import com.nexmo.mobilecallerdemo.RingActivity
import com.nexmo.mobilecallerdemo.connection.OTAction
import me.pushy.sdk.Pushy

class NotificationService(private val context: Context) {
    companion object {
        const val TAG = "NotificationService"

        const val CHANNEL_ID_RING = "mobile_caller_demo_channel"
        const val CHANNEL_NAME_RING = "Incoming Call"

        const val REQUEST_CODE_RINGER = 0
        const val REQUEST_CODE_ANSWER = 1
        const val REQUEST_CODE_REJECT = 2

        const val NOTIFICATION_FROM = "from"
        const val NOTIFICATION_API_KEY = "apiKey"
        const val NOTIFICATION_SESSION_ID = "sessionId"
        const val NOTIFICATION_TOKEN = "token"

        const val NOTIFICATION_ACTION_ANSWER = "Answer"
        const val NOTIFICATION_ACTION_REJECT = "Reject"
    }

    init {
        createRingChannel()
    }


    fun createRingingNotification(incomingCallRequest: IncomingCallRequest): Notification {
        val contentText = incomingCallRequest.from

        val answerPendingIntent = createAnswerPendingIntent(context, incomingCallRequest)
        val rejectPendingIntent = createRejectPendingIntent(context, incomingCallRequest)
        val ringerPendingIntent = createRingerPendingIntent(context, incomingCallRequest)

        val builder = NotificationCompat.Builder(context)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Incoming video call")
            .setContentText(contentText)
            .setLights(Color.RED, 1000, 1000)
            .setVibrate(longArrayOf(0, 400, 250, 400))
            .setChannelId(CHANNEL_ID_RING)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .addAction(
                android.R.drawable.presence_audio_online,
                NOTIFICATION_ACTION_ANSWER, answerPendingIntent)
            .addAction(android.R.drawable.presence_audio_busy,
                NOTIFICATION_ACTION_REJECT,
                rejectPendingIntent
            )
            .setOngoing(true)
            .setFullScreenIntent(ringerPendingIntent, true)

        return builder.build()
    }

    private fun createRingChannel() {
        val importance = NotificationManager.IMPORTANCE_HIGH
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        val channel = NotificationChannel(CHANNEL_ID_RING, CHANNEL_NAME_RING, importance)
        channel.setSound(soundUri, audioAttributes)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun createIntent(action: String, incomingCallRequest: IncomingCallRequest): Intent {
        val intent = Intent()
        intent.action = action

        intent.putExtra(NOTIFICATION_FROM, incomingCallRequest.from)
        intent.putExtra(NOTIFICATION_API_KEY, incomingCallRequest.apiKey)
        intent.putExtra(NOTIFICATION_SESSION_ID, incomingCallRequest.sessionId)
        intent.putExtra(NOTIFICATION_TOKEN, incomingCallRequest.token)

        return intent
    }

    private fun createRingerIntent(incomingCallRequest: IncomingCallRequest): Intent {
        val intent = Intent()

        intent.setClass(context, RingActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

        intent.putExtra(NOTIFICATION_FROM, incomingCallRequest.from)
        intent.putExtra(NOTIFICATION_API_KEY, incomingCallRequest.apiKey)
        intent.putExtra(NOTIFICATION_SESSION_ID, incomingCallRequest.sessionId)
        intent.putExtra(NOTIFICATION_TOKEN, incomingCallRequest.token)

        return intent
    }

    private fun createAnswerPendingIntent(
        context: Context, incomingCallRequest: IncomingCallRequest
    ): PendingIntent {
        val answerIntent = createIntent(OTAction.LOCAL_ANSWER, incomingCallRequest)
        return PendingIntent.getBroadcast(context, REQUEST_CODE_ANSWER,
            answerIntent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun createRejectPendingIntent(
        context: Context, incomingCallRequest: IncomingCallRequest
    ): PendingIntent {
        val rejectIntent = createIntent(OTAction.LOCAL_REJECT, incomingCallRequest)
        return PendingIntent.getBroadcast(context, REQUEST_CODE_REJECT,
            rejectIntent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun createRingerPendingIntent(
        context: Context, incomingCallRequest: IncomingCallRequest
    ): PendingIntent {
        val ringerIntent = createRingerIntent(incomingCallRequest)
        return PendingIntent.getActivity(context, REQUEST_CODE_RINGER,
            ringerIntent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    fun showNotification(context: Context, notificationText: String) {
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, HomeActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        val builder = NotificationCompat.Builder(context)
            .setAutoCancel(true)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(PushyReceiver.NOTIFICATION_TITLE)
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