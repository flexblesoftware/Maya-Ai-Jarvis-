package com.maya.android.app.ai

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log

class AudioOutputManager {

    companion object {
        const val SAMPLE_RATE = 24000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private var audioTrack: AudioTrack? = null
    private var isPlaying = false

    init {
        initAudioTrack()
    }

    private fun initAudioTrack() {
        try {
            val minBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            val bufferSize = (minBufferSize * 4).coerceAtLeast(8192)

            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            val format = AudioFormat.Builder()
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(CHANNEL_CONFIG)
                .setEncoding(AUDIO_FORMAT)
                .build()

            audioTrack = AudioTrack(
                attributes,
                format,
                bufferSize,
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            )
            audioTrack?.play()
            isPlaying = true
            Log.i("AudioOutputManager", "AudioTrack initialized at 24kHz mono PCM16")
        } catch (e: Exception) {
            Log.e("AudioOutputManager", "Failed to initialize AudioTrack", e)
        }
    }

    fun writeAudioChunk(pcmBytes: ByteArray) {
        if (pcmBytes.isEmpty()) return
        try {
            if (audioTrack == null || audioTrack?.state != AudioTrack.STATE_INITIALIZED) {
                initAudioTrack()
            }
            if (audioTrack?.playState != AudioTrack.PLAYSTATE_PLAYING) {
                audioTrack?.play()
            }
            audioTrack?.write(pcmBytes, 0, pcmBytes.size, AudioTrack.WRITE_BLOCKING)
        } catch (e: Exception) {
            Log.w("AudioOutputManager", "Error writing audio chunk", e)
        }
    }

    fun flush() {
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.play()
            Log.d("AudioOutputManager", "AudioTrack flushed immediately (barge-in)")
        } catch (e: Exception) {
            Log.w("AudioOutputManager", "Error flushing AudioTrack", e)
        }
    }

    fun release() {
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            Log.w("AudioOutputManager", "Error releasing AudioTrack", e)
        }
        audioTrack = null
        isPlaying = false
    }
}
