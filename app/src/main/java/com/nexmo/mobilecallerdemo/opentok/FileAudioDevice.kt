package com.nexmo.mobilecallerdemo.opentok

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Environment
import android.os.Handler
import android.util.Log
import com.opentok.android.BaseAudioDevice
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer

class FileAudioDevice(private val context: Context): BaseAudioDevice() {
    companion object {
        const val TAG = "FileAudioDevice"

        const val SAMPLING_RATE = 44100

        const val NUM_CHANNELS_CAPTURING = 1
        const val NUM_CHANNELS_RENDERING = 1

        const val CAPTURER_INTERVAL_MS: Long = 1000
        const val RENDERER_INTERVAL_MS: Long = 1000
    }

    private var capturerStarted: Boolean = false
    private var rendererStarted: Boolean = false

    private var audioDriverPaused: Boolean = false

    private lateinit var capturerBuffer: ByteBuffer
    private lateinit var rendererBuffer: ByteBuffer

    private var rendererFile: File? = null

    private var capturerHandler: Handler
    private var rendererHandler: Handler

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

    private val renderer = Runnable {
        rendererBuffer.clear()

        val sampleRead = audioBus.readRenderData(rendererBuffer, SAMPLING_RATE)

        // TODO: Play from rendererBuffer
        try {
            val outputStream = FileOutputStream(rendererFile!!)
            outputStream.write(rendererBuffer.array(), 0, sampleRead * 2)
            outputStream.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        if (rendererStarted && !audioDriverPaused) {
            scheduleNextRender()
        }
    }

    private fun scheduleNextRender() {
        rendererHandler.postDelayed(renderer, RENDERER_INTERVAL_MS)
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
        rendererBuffer = ByteBuffer.allocateDirect(SAMPLING_RATE * 2)

        rendererFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "output.raw")

        if (!rendererFile!!.exists()) {
            try {
                rendererFile!!.parentFile!!.mkdirs()
                Log.d(TAG, "Created File Parent")
                rendererFile!!.createNewFile()
                Log.d(TAG, "Created File")
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }



        Log.d(TAG, "Init Renderer End")
        return true
    }

    override fun startRenderer(): Boolean {
        Log.d(TAG, "Start Renderer Start")
        rendererStarted = true
        scheduleNextRender()

        Log.d(TAG, "Start Renderer End")
        return true
    }

    override fun stopRenderer(): Boolean {
        Log.d(TAG, "Stop Renderer Start")
        rendererStarted = false
        rendererHandler.removeCallbacks(renderer)
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

        if (rendererStarted) {
            scheduleNextRender()
        }
    }

    override fun onPause() {
        audioDriverPaused = true
        capturerHandler.removeCallbacks(capturer)
        rendererHandler.removeCallbacks(renderer)
    }

}