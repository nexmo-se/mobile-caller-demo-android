package com.nexmo.mobilecallerdemo.opentok

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.Handler
import android.util.Log
import com.opentok.android.BaseAudioDevice
import java.lang.IllegalStateException
import java.lang.RuntimeException
import java.nio.ByteBuffer
import java.util.concurrent.locks.ReentrantLock

class CustomAudioDevice(private val context: Context): BaseAudioDevice() {
    companion object {
        const val TAG = "CustomAudioDevice"

        const val SAMPLING_RATE = 44100

        const val NUM_CHANNELS_CAPTURING = 1
        const val NUM_CHANNELS_RENDERING = 1

        const val CAPTURER_INTERVAL_MS: Long = 1000
        const val RENDERER_INTERVAL_MS: Long = 1000

        const val RENDERER_BUFFER_FRAMES = 10
    }

    private val renderLock = ReentrantLock(true)
    private val renderEvent = renderLock.newCondition()

    private var capturerStarted: Boolean = false
    private var rendererStarted: Boolean = false

    private var audioDriverPaused: Boolean = false

    private lateinit var capturerBuffer: ByteBuffer
    private lateinit var rendererBuffer: ByteBuffer

    private var capturerHandler: Handler
    private var rendererHandler: Handler

    private var audioTrack: AudioTrack? = null
    private var killRendererThread: Boolean = false

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
        val samplesPerBuffer = 440
        val bufferSize = 1760

        val playBuffer = ByteBuffer.allocateDirect(bufferSize)
        val tempBufPlay = ByteArray(bufferSize)



        while (!killRendererThread) {
            renderLock.lock()

            try {
                if (!rendererStarted) {
                    renderEvent.await()
                } else {
                    // Read from Audio Bus
                    renderLock.unlock()
                    playBuffer.clear()
                    var readSize = audioBus.readRenderData(playBuffer, samplesPerBuffer)
                    renderLock.lock()

                    if (audioTrack != null && rendererStarted) {
                        readSize = (readSize shl 1) * 1
                        playBuffer.get(tempBufPlay, 0, readSize)

                        val writeSize = audioTrack!!.write(tempBufPlay, 0, readSize)
                        if (writeSize <= 0) {
                            when (writeSize) {
                                -3 -> {
                                    throw RuntimeException("rendererRunnable(): AudioTrack.ERROR_INVALID_OPERATION")
                                }
                                -2 -> {
                                    throw RuntimeException("rendererRunnable(): AudioTrack.ERROR_BAD_VALUE")
                                }
                                else -> {
                                    throw RuntimeException("rendererRunnable(): AudioTrack.ERROR or default")
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                renderLock.unlock()
            }
        }
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
        val minimumBufferSize = AudioTrack.getMinBufferSize(renderSettings.sampleRate, 4, 2)
        audioTrack = AudioTrack(
            0,
            renderSettings.sampleRate,
            4,
            2,
            if (minimumBufferSize >= 6000) minimumBufferSize else minimumBufferSize * 2,
            1
        )

        rendererThread = Thread(rendererRunnable)
        rendererThread?.start()

        Log.d(TAG, "Init Renderer End")
        return true
    }

    private fun destroyAudioTrack() {
        renderLock.lock()
        audioTrack?.release()
        audioTrack = null
        killRendererThread = true
        renderEvent.signal()
        renderLock.unlock()
    }

    override fun startRenderer(): Boolean {
        Log.d(TAG, "Start Renderer Start")

        if (audioTrack == null) {
            throw IllegalStateException("startRenderer(): play() called on uninitialized AudioTrack.")
        } else {
            try {
                audioTrack!!.play()
            } catch (e: IllegalStateException) {
                throw RuntimeException(e.message)
            }

            renderLock.lock()
            rendererStarted = true
            renderEvent.signal()
            renderLock.unlock()
        }

        Log.d(TAG, "Start Renderer End")
        return true
    }

    override fun stopRenderer(): Boolean {
        Log.d(TAG, "Stop Renderer Start")

        if (audioTrack == null) {
            throw IllegalStateException("stopRenderer(): stop() called on uninitialized AudioTrack.")
        } else {
            renderLock.lock()

            try {
                if (audioTrack!!.playState == 3) {
                    audioTrack!!.stop()
                }

                audioTrack!!.flush()
            } catch (e: Exception) {
                throw RuntimeException(e.message)
            } finally {
                rendererStarted = false
                renderLock.unlock()
            }
        }


        Log.d(TAG, "Stop Renderer End")
        return true
    }

    override fun destroyRenderer(): Boolean {
        Log.d(TAG, "Destroy Renderer Start")
        destroyAudioTrack()
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