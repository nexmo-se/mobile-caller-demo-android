package com.nexmo.mobilecallerdemo

import android.os.Bundle
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.nexmo.mobilecallerdemo.opentok.VideoService

class CallActivity : AppCompatActivity() {
    companion object {
        const val TAG = "CallActivity"
    }

    private lateinit var videoService: VideoService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)

        // initialize view objects from your layout
        val publisherViewContainer = findViewById<FrameLayout>(R.id.publisher_container)
        val subscriberViewContainer = findViewById<FrameLayout>(R.id.subscriber_container)

        videoService = VideoService(this)
        videoService.init(publisherViewContainer, subscriberViewContainer)
    }
}
