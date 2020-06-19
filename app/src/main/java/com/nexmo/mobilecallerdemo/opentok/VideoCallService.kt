package com.nexmo.mobilecallerdemo.opentok

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.Log
import android.widget.FrameLayout
import com.opentok.android.*

class VideoCallService(private val context: Context): Session.SessionListener, PublisherKit.PublisherListener {
    companion object {
        const val TAG = "SessionService"
    }

    private var mSession: Session? = null
    private var mPublisherViewContainer: FrameLayout? = null
    private var mSubscriberViewContainer: FrameLayout? = null
    private var mPublisher: Publisher? = null
    private var mSubscriber: Subscriber? = null

    private lateinit var videoListener: VideoListener

    fun init(
        publisherViewContainer: FrameLayout?, subscriberViewContainer: FrameLayout?,
        apiKey: String, sessionId: String, token: String,
        videoListener: VideoListener
    ) {
        mPublisherViewContainer = publisherViewContainer
        mSubscriberViewContainer = subscriberViewContainer
        this.videoListener = videoListener

        // initialize and connect to the session
        Log.d(TAG, "Init Session")

        mSession = Session.Builder(context, apiKey, sessionId).build()
        mSession?.setSessionListener(this)

        mSession?.connect(token)
        Log.d(TAG, "Session Initialized")
    }

    fun endSession() {
        Log.d(TAG, "Ending Session")
        mSession?.disconnect()
        Log.d(TAG, "Session Ended")
    }


    override fun onStreamDropped(p0: Session?, p1: Stream?) {
        Log.d(TAG, "Stream Dropped")
        if (mSubscriber != null) {
            mSubscriber = null
            mSubscriberViewContainer?.removeAllViews()
        }

        endSession()
    }

    override fun onStreamReceived(p0: Session?, p1: Stream?) {
        Log.d(TAG, "Stream Received")
        if (mSubscriber == null) {
            mSubscriber = Subscriber.Builder(context, p1).build()
            mSession?.subscribe(mSubscriber)
            mSubscriberViewContainer?.addView(mSubscriber?.view)
        }

        videoListener.onRemoteJoin()
    }

    override fun onConnected(p0: Session?) {
        Log.d(TAG, "Session Connected")
        mPublisher = Publisher.Builder(context)
            .build()
        mPublisher?.setPublisherListener(this)

        mPublisherViewContainer?.addView(mPublisher?.view)

        if (mPublisher?.view is GLSurfaceView) {
            (mPublisher?.view as GLSurfaceView).setZOrderOnTop(true)
        }

        mSession?.publish(mPublisher)

        videoListener.onCallStart()
    }

    override fun onDisconnected(p0: Session?) {
        Log.d(TAG, "Session Disconnected")
        videoListener.onCallEnd()
    }

    override fun onError(p0: Session?, p1: OpentokError?) {
        Log.d(TAG, "Session Error: ${p1?.message}")
    }


    override fun onStreamCreated(publisherKit: PublisherKit, stream: Stream) {
        Log.d(TAG, "Publisher onStreamCreated")
        videoListener.onLocalJoin()
    }

    override fun onStreamDestroyed(publisherKit: PublisherKit, stream: Stream) {
        Log.d(TAG, "Publisher onStreamDestroyed")
        endSession()
    }

    override fun onError(publisherKit: PublisherKit, opentokError: OpentokError) {
        Log.d(TAG, "Publisher error: " + opentokError.message)
    }
}