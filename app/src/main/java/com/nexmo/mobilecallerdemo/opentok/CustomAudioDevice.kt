package com.nexmo.mobilecallerdemo.opentok

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
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
    }

    private val captureLock = ReentrantLock(true)
    private val captureEvent = captureLock.newCondition()

    private val renderLock = ReentrantLock(true)
    private val renderEvent = renderLock.newCondition()

    private var capturerStarted: Boolean = false
    private var rendererStarted: Boolean = false

    private var audioDriverPaused: Boolean = false

    private var audioRecord: AudioRecord? = null
    private var killCapturerThread: Boolean = false

    private var audioTrack: AudioTrack? = null
    private var killRendererThread: Boolean = false

    private val capturerRunnable = Runnable {
        val samplesPerBuffer = 441
        val bufferSize = 1760

        val recBuffer = ByteBuffer.allocateDirect(bufferSize)
        val tempBufRec = ByteArray(bufferSize)

        while (!killCapturerThread) {
            captureLock.lock()

            try {
                if (!capturerStarted) {
                    captureEvent.await()
                    continue
                }

                if (audioRecord != null && capturerStarted) {
                    var readSize = (samplesPerBuffer shl 1) * 1
                    readSize = audioRecord!!.read(tempBufRec, 0, readSize)
                    if (readSize < 0) {
                        when (readSize) {
                            -3 -> {
                                throw RuntimeException("captureRunnable(): AudioRecord.ERROR_INVALID_OPERATION")
                            }
                            -2 -> {
                                throw RuntimeException("captureRunnable(): AudioRecord.ERROR_BAD_VALUE")
                            }
                            else -> {
                                throw RuntimeException("captureRunnable(): AudioRecord.ERROR or default")
                            }
                        }
                    }

                    recBuffer.rewind()
                    recBuffer.put(tempBufRec)

                    val writeSize = (readSize shr 1) / 1

                    audioBus.writeCaptureData(recBuffer, writeSize)
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            } finally {
                captureLock.unlock()
            }
        }
    }

    private var capturerThread: Thread? = null

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
                    continue
                }

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
    }

    override fun initCapturer(): Boolean {
        Log.d(TAG, "Init Capturer Start")

        val minimumBufferSize = AudioRecord.getMinBufferSize(
            captureSettings.sampleRate,
            16,
            2
        )
        val bufferSize = minimumBufferSize * 2

        if (audioRecord != null) {
            audioRecord!!.release()
            audioRecord = null
        }

        audioRecord = AudioRecord(
            7,
            captureSettings.sampleRate,
            16,
            2,
            bufferSize
        )

        if (audioRecord!!.state != 1) {
            throw RuntimeException("Audio capture is not initialized ${captureSettings.sampleRate}")
        }

        killCapturerThread = false
        capturerThread = Thread(capturerRunnable)
        capturerThread?.start()

        Log.d(TAG, "Init Capturer End")
        return true
    }

    override fun startCapturer(): Boolean {
        Log.d(TAG, "Start Capturer Start")

        if (audioRecord == null) {
            throw IllegalStateException("startCapturer(): startRecording() called on an uninitialized AudioRecord.")
        }

        try {
            audioRecord!!.startRecording()
        } catch (e: IllegalStateException) {
            throw RuntimeException(e.message)
        }

        captureLock.lock()
        capturerStarted = true
        captureEvent.signal()
        captureLock.unlock()

        Log.d(TAG, "Start Capturer End")
        return true
    }

    override fun stopCapturer(): Boolean {
        Log.d(TAG, "Stop Capturer Start")

        if (audioRecord == null) {
            throw IllegalStateException("stopCapturer(): stop() called on an uninitialized AudioRecord.")
        }

        captureLock.lock()

        try {
            if (audioRecord!!.recordingState == 3) {
                audioRecord!!.stop()
            }
        } catch (e: java.lang.Exception) {
            throw RuntimeException(e.message)
        } finally {
            capturerStarted = false
            captureLock.unlock()
        }

        Log.d(TAG, "Stop Capturer End")
        return true
    }

    override fun destroyCapturer(): Boolean {
        Log.d(TAG, "Destroy Capturer Start")

        captureLock.lock()
        audioRecord?.release()
        audioRecord = null
        killCapturerThread = true
        captureEvent.signal()
        captureLock.unlock()

        Log.d(TAG, "Destroy Capturer End")
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
        val minimumBufferSize = AudioTrack.getMinBufferSize(renderSettings.sampleRate, 4, 2)
        audioTrack = AudioTrack(
            0,
            renderSettings.sampleRate,
            4,
            2,
            if (minimumBufferSize >= 6000) minimumBufferSize else minimumBufferSize * 2,
            1
        )

        if (audioTrack!!.state != 1) {
            throw RuntimeException("Audio renderer not initialized ${renderSettings.sampleRate}")
        }

        killRendererThread = false
        rendererThread = Thread(rendererRunnable)
        rendererThread?.start()

        Log.d(TAG, "Init Renderer End")
        return true
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

        renderLock.lock()
        audioTrack?.release()
        audioTrack = null
        killRendererThread = true
        renderEvent.signal()
        renderLock.unlock()

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
    }

    override fun onPause() {
        audioDriverPaused = true
    }

}