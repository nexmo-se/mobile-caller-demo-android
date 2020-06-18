package com.nexmo.mobilecallerdemo.opentok

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.Log
import android.widget.FrameLayout
import com.opentok.android.*

class VideoService(private val context: Context): Session.SessionListener, PublisherKit.PublisherListener {
    companion object {
        const val TAG = "SessionService"
    }

    private var mSession: Session? = null
    private var mPublisherViewContainer: FrameLayout? = null
    private var mSubscriberViewContainer: FrameLayout? = null
    private var mPublisher: Publisher? = null
    private var mSubscriber: Subscriber? = null

    fun init(publisherViewContainer: FrameLayout?, subscriberViewContainer: FrameLayout?) {
        mPublisherViewContainer = publisherViewContainer
        mSubscriberViewContainer = subscriberViewContainer

        // initialize and connect to the session
        mSession = Session.Builder(context, Config.API_KEY, Config.SESSION_ID).build()
        mSession?.setSessionListener(this)
        mSession?.connect(Config.TOKEN)
    }


    override fun onStreamDropped(p0: Session?, p1: Stream?) {
        Log.d(TAG, "Stream Dropped")
        if (mSubscriber != null) {
            mSubscriber = null;
            mSubscriberViewContainer?.removeAllViews()
        }
    }

    override fun onStreamReceived(p0: Session?, p1: Stream?) {
        Log.d(TAG, "Stream Received")
        if (mSubscriber == null) {
            mSubscriber = Subscriber.Builder(context, p1).build()
            mSession?.subscribe(mSubscriber)
            mSubscriberViewContainer?.addView(mSubscriber?.view)
        }
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
    }

    override fun onDisconnected(p0: Session?) {
        Log.d(TAG, "Session Disconnected")
    }

    override fun onError(p0: Session?, p1: OpentokError?) {
        Log.d(TAG, "Session Error: ${p1?.message}")
    }


    override fun onStreamCreated(publisherKit: PublisherKit, stream: Stream) {
        Log.d(TAG, "Publisher onStreamCreated")
    }

    override fun onStreamDestroyed(publisherKit: PublisherKit, stream: Stream) {
        Log.d(TAG, "Publisher onStreamDestroyed")
    }

    override fun onError(publisherKit: PublisherKit, opentokError: OpentokError) {
        Log.d(TAG, "Publisher error: " + opentokError.message)
    }
}