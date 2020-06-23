package com.nexmo.mobilecallerdemo.opentok

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Handler
import android.util.Log
import com.opentok.android.BaseAudioDevice
import java.nio.ByteBuffer

class CustomAudioDevice(private val context: Context): BaseAudioDevice() {
    companion object {
        const val TAG = "CustomAudioDevice"

        const val SAMPLING_RATE = 16000

        const val NUM_CHANNELS_CAPTURING = 1
        const val NUM_CHANNELS_RENDERING = 1

        const val CAPTURER_INTERVAL_MS: Long = 1000
        const val RENDERER_INTERVAL_MS: Long = 1000

        const val RENDERER_BUFFER_FRAMES = 10

        const val RENDERER_SESSION_ID = 1
    }

    private var capturerStarted: Boolean = false
    private var rendererStarted: Boolean = false

    private var audioDriverPaused: Boolean = false

    private lateinit var capturerBuffer: ByteBuffer
    private lateinit var rendererBuffer: ByteBuffer

    private var capturerHandler: Handler
    private var rendererHandler: Handler

    private var audioTrack: AudioTrack? = null

    private val capturer = Runnable {
        capturerBuffer.rewind()

        // TODO: Record to capturerBuffer

        audioBus.writeCaptureData(capturerBuffer, SAMPLING_RATE)

        if (capturerStarted && !audioDriverPaused) {
            scheduleNextCapture()
        }
    }

    private fun scheduleNextCapture() {
        capturerHandler.postDelayed(capturer, CAPTURER_INTERVAL_MS)
    }

    private val rendererRunnable = Runnable {
        Log.d(TAG, "Renderer Thread Started")
        var isFirst = true
        while (rendererStarted) {
            rendererBuffer.clear()

            val samplesRead = audioBus.readRenderData(rendererBuffer, SAMPLING_RATE)
            val bytesRead = samplesRead * 2
            Log.d(TAG, "AudioBus (PLAYBACK) $samplesRead samples - $bytesRead bytes - ${rendererBuffer.capacity()} cap")

            val byteArray = ByteArray(bytesRead)
            rendererBuffer.get(byteArray, 0, bytesRead)
            audioTrack?.write(byteArray, 0, bytesRead)

            if (isFirst) {
                audioTrack?.play()
                isFirst = false
            }

            Log.d(TAG, "Playback write ($bytesRead)")

            // Sleep until next cycle
            Thread.sleep(100)
        }

        audioTrack?.flush()
        audioTrack?.stop()
        audioTrack?.release()

        Log.d(TAG, "Renderer Thread Ended")
    }

    private var rendererThread: Thread? = null

    init {
        capturerStarted = false
        rendererStarted = false

        audioDriverPaused = false

        capturerHandler = Handler()
        rendererHandler = Handler()
    }

    override fun initCapturer(): Boolean {
        capturerBuffer = ByteBuffer.allocateDirect(SAMPLING_RATE * 2)
        return true
    }

    override fun startCapturer(): Boolean {
        capturerStarted = true
        scheduleNextCapture()
        return true
    }

    override fun stopCapturer(): Boolean {
        capturerStarted = false
        capturerHandler.removeCallbacks(capturer)
        return true
    }

    override fun destroyCapturer(): Boolean {
        return true
    }

    override fun getCaptureSettings(): AudioSettings {
        return AudioSettings(SAMPLING_RATE, NUM_CHANNELS_CAPTURING)
    }

    override fun getEstimatedCaptureDelay(): Int {
        return 0
    }

    override fun initRenderer(): Boolean {
        Log.d(TAG, "Init Renderer Start")
        rendererBuffer = ByteBuffer.allocateDirect(SAMPLING_RATE * 2 * RENDERER_BUFFER_FRAMES)

        audioTrack = AudioTrack(
            AudioManager.STREAM_VOICE_CALL,
            SAMPLING_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            SAMPLING_RATE * 2 * RENDERER_BUFFER_FRAMES,
            AudioTrack.MODE_STREAM)

        Log.d(TAG, "Init Renderer End")
        return true
    }

    override fun startRenderer(): Boolean {
        Log.d(TAG, "Start Renderer Start")
        rendererStarted = true

        rendererThread = Thread(rendererRunnable)
        rendererThread?.start()

        Log.d(TAG, "Start Renderer End")
        return true
    }

    override fun stopRenderer(): Boolean {
        Log.d(TAG, "Stop Renderer Start")
        rendererStarted = false
        Log.d(TAG, "Stop Renderer End")
        return true
    }

    override fun destroyRenderer(): Boolean {
        Log.d(TAG, "Destroy Renderer Start")
        Log.d(TAG, "Destroy Renderer End")
        return true
    }

    override fun getRenderSettings(): AudioSettings {
        Log.d(TAG, "Get Render Settings")
        return AudioSettings(SAMPLING_RATE, NUM_CHANNELS_RENDERING)
    }

    override fun getEstimatedRenderDelay(): Int {
        return 0
    }

    override fun onResume() {
        audioDriverPaused = false

        if (capturerStarted) {
            scheduleNextCapture()
        }
    }

    override fun onPause() {
        audioDriverPaused = true
        capturerHandler.removeCallbacks(capturer)
    }

}