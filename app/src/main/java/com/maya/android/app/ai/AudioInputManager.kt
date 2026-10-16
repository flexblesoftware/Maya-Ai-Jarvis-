package com.maya.android.app.ai

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioInputManager(
    private val context: Context,
    private val wakeWordEngine: WakeWordEngine,
    private val onWakeWordDetected: () -> Unit,
    private val onLiveAudioChunk: (ByteArray) -> Unit
) {
    companion object {
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        const val CHUNK_SIZE_SAMPLES = 1024
    }

    private var audioRecord: AudioRecord? = null
    private var echoCanceler: AcousticEchoCanceler? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var recordingJob: Job? = null
    private var isLiveSessionActive = false
    private var isRecording = false

    fun setLiveSessionActive(active: Boolean) {
        isLiveSessionActive = active
        Log.d("AudioInputManager", "Live session mode: $active")
    }

    @SuppressLint("MissingPermission")
    fun start(scope: CoroutineScope) {
        if (isRecording) return

        try {
            val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            val bufferSize = (minBufferSize * 2).coerceAtLeast(CHUNK_SIZE_SAMPLES * 2 * 4)

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.w("AudioInputManager", "VOICE_COMMUNICATION uninitialized, falling back to MIC")
                audioRecord?.release()
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
                )
            }

            val audioSessionId = audioRecord?.audioSessionId ?: 0
            if (audioSessionId != 0) {
                if (AcousticEchoCanceler.isAvailable()) {
                    try {
                        echoCanceler = AcousticEchoCanceler.create(audioSessionId)?.apply {
                            enabled = true
                        }
                    } catch (e: Exception) {
                        Log.w("AudioInputManager", "AEC enable failed", e)
                    }
                }
                if (NoiseSuppressor.isAvailable()) {
                    try {
                        noiseSuppressor = NoiseSuppressor.create(audioSessionId)?.apply {
                            enabled = true
                        }
                    } catch (e: Exception) {
                        Log.w("AudioInputManager", "NoiseSuppressor enable failed", e)
                    }
                }
            }

            audioRecord?.startRecording()
            isRecording = true

            recordingJob = scope.launch(Dispatchers.IO) {
                val shortBuffer = ShortArray(CHUNK_SIZE_SAMPLES)
                val byteBuffer = ByteArray(CHUNK_SIZE_SAMPLES * 2)

                while (isActive && isRecording) {
                    val readShorts = audioRecord?.read(shortBuffer, 0, CHUNK_SIZE_SAMPLES) ?: 0
                    if (readShorts > 0) {
                        // Convert shorts to little-endian byte array
                        ByteBuffer.wrap(byteBuffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(shortBuffer, 0, readShorts)

                        if (isLiveSessionActive) {
                            // Active Live Session: stream audio directly to Gemini Live
                            val exactBytes = ByteArray(readShorts * 2)
                            System.arraycopy(byteBuffer, 0, exactBytes, 0, readShorts * 2)
                            onLiveAudioChunk(exactBytes)
                        } else {
                            // Idle state: feed wake-word detector
                            val detected = wakeWordEngine.processAudio(shortBuffer, readShorts)
                            if (detected) {
                                scope.launch(Dispatchers.Main) {
                                    onWakeWordDetected()
                                }
                            }
                        }
                    }
                }
            }
            Log.i("AudioInputManager", "Shared audio input pipeline started successfully")
        } catch (e: Exception) {
            Log.e("AudioInputManager", "Failed to start AudioRecord pipeline", e)
            isRecording = false
        }
    }

    fun stop() {
        isRecording = false
        recordingJob?.cancel()
        recordingJob = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.w("AudioInputManager", "Error stopping audioRecord", e)
        }
        audioRecord = null

        try {
            echoCanceler?.release()
            noiseSuppressor?.release()
        } catch (e: Exception) {
            Log.w("AudioInputManager", "Error releasing audio effects", e)
        }
        echoCanceler = null
        noiseSuppressor = null
    }
}
