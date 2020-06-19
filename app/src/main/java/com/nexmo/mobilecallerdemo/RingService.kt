package com.nexmo.mobilecallerdemo

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.nexmo.mobilecallerdemo.api.ApiService
import me.pushy.sdk.Pushy

class RingService : Service() {
    companion object {
        const val TAG = "RingService"

        const val NOTIFICATION_FROM = "from"
        const val NOTIFICATION_API_KEY = "apiKey"
        const val NOTIFICATION_SESSION_ID = "sessionId"
        const val NOTIFICATION_TOKEN = "token"

        const val ACTION_ANSWER_CALL = "ACTION_ANSWER_CALL"
        const val ACTION_REJECT_CALL = "ACTION_REJECT_CALL"
        const val ACTION_SHOW_RINGER = "ACTION_SHOW_RINGER"
    }

    private lateinit var from: String
    private lateinit var apiKey: String
    private lateinit var sessionId: String
    private lateinit var token: String

    private lateinit var ringtone: Ringtone

    private lateinit var apiService: ApiService
    private lateinit var callBroadcastReceiver: BroadcastReceiver

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            return super.onStartCommand(intent, flags, startId)
        }

        val notification = createNotification(this, intent!!)
        startForeground(10, notification)

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        super.onCreate()

        apiService = ApiService()
        callBroadcastReceiver = object: BroadcastReceiver() {
            override fun onReceive(cbrContext: Context?, cbrIntent: Intent?) {
                if (cbrIntent?.action == ACTION_ANSWER_CALL) {
                    Log.d(TAG, "Call answer signal received")
                    ringtone.stop()
                    stopForeground(true)
                } else if (cbrIntent?.action == ACTION_REJECT_CALL) {
                    Log.d(TAG, "Call reject signal received")
                    val runnable = Runnable {
                        apiService.rejectCall(from)
                        ringtone.stop()
                        stopForeground(true)
                    }
                    val thread = Thread(runnable)
                    thread.start()
                } else if (cbrIntent?.action == ACTION_SHOW_RINGER) {
                    Log.d(TAG, "Show ringer signal received")
                    ringtone.stop()
                    stopForeground(true)
                }
            }
        }

        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        ringtone = RingtoneManager.getRingtone(this, uri)
        ringtone.play()

        val intentFilter = IntentFilter()
        intentFilter.addAction(ACTION_ANSWER_CALL)
        intentFilter.addAction(ACTION_REJECT_CALL)
        intentFilter.addAction(ACTION_SHOW_RINGER)
        registerReceiver(callBroadcastReceiver, intentFilter)
    }

    override fun onDestroy() {
        unregisterReceiver(callBroadcastReceiver)
        super.onDestroy()
    }

    private fun createNotification(context: Context, intent: Intent): Notification {
        from = intent.getStringExtra(NOTIFICATION_FROM) ?: ""
        apiKey = intent.getStringExtra(NOTIFICATION_API_KEY) ?: ""
        sessionId = intent.getStringExtra(NOTIFICATION_SESSION_ID) ?: ""
        token = intent.getStringExtra(NOTIFICATION_TOKEN) ?: ""

        Log.d(TAG, "Incoming Call received")
        Log.d(TAG, "From: $from")
        Log.d(TAG, "OT API Key: $apiKey")
        Log.d(TAG, "OT Session ID: $sessionId")
        Log.d(TAG, "OT Token: $token")

        val fullscreenIntent = Intent(context, RingActivity::class.java)
        fullscreenIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        fullscreenIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

        fullscreenIntent.putExtra(NOTIFICATION_FROM, from)
        fullscreenIntent.putExtra(NOTIFICATION_API_KEY, apiKey)
        fullscreenIntent.putExtra(NOTIFICATION_SESSION_ID, sessionId)
        fullscreenIntent.putExtra(NOTIFICATION_TOKEN, token)

        val answerIntent = Intent(context, CallActivity::class.java)
        answerIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        answerIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

        answerIntent.putExtra(NOTIFICATION_FROM, from)
        answerIntent.putExtra(NOTIFICATION_API_KEY, apiKey)
        answerIntent.putExtra(NOTIFICATION_SESSION_ID, sessionId)
        answerIntent.putExtra(NOTIFICATION_TOKEN, token)


        val rejectIntent = Intent(ACTION_REJECT_CALL)

        val fullscreenPendingIntent = PendingIntent.getActivity(context, 0,
            fullscreenIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val answerPendingIntent = PendingIntent.getActivity(context, 1,
            answerIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val rejectPendingIntent = PendingIntent.getBroadcast(context, 2,
            rejectIntent, PendingIntent.FLAG_UPDATE_CURRENT)


        val builder = NotificationCompat.Builder(context)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Incoming video call")
            .setContentText(from)
            .setLights(Color.RED, 1000, 1000)
            .setVibrate(longArrayOf(0, 400, 250, 400))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .addAction(android.R.drawable.presence_audio_online, "Answer", answerPendingIntent)
            .addAction(android.R.drawable.presence_audio_busy, "Reject", rejectPendingIntent)
            .setFullScreenIntent(fullscreenPendingIntent, true)

        Pushy.setNotificationChannel(builder, context)
        return builder.build()
    }
}
