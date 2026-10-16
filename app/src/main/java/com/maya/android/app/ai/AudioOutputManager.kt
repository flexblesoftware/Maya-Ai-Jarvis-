package com.maya.android.app.ai

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

class AudioOutputManager(private val context: Context) {

    companion object {
        const val SAMPLE_RATE = 24000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private var audioTrack: AudioTrack? = null
    private var isPlaying = false

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var isTtsSpeaking = false

    init {
        initAudioTrack()
        initTts()
    }

    private fun initAudioTrack() {
        try {
            val minBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            val bufferSize = (minBufferSize * 4).coerceAtLeast(8192)

            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
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
            Log.i("AudioOutputManager", "AudioTrack initialized at 24kHz mono PCM16 with USAGE_MEDIA")
        } catch (e: Exception) {
            Log.e("AudioOutputManager", "Failed to initialize AudioTrack", e)
        }
    }

    private fun initTts() {
        try {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val result = tts?.setLanguage(Locale("hi", "IN"))
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        tts?.language = Locale.ENGLISH
                    }
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            isTtsSpeaking = true
                        }
                        override fun onDone(utteranceId: String?) {
                            isTtsSpeaking = false
                        }
                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            isTtsSpeaking = false
                        }
                    })
                    isTtsReady = true
                    Log.i("AudioOutputManager", "TTS engine initialized successfully")
                }
            }
        } catch (e: Exception) {
            Log.w("AudioOutputManager", "Error initializing TTS", e)
        }
    }

    fun writeAudioChunk(pcmBytes: ByteArray) {
        if (pcmBytes.isEmpty()) return
        try {
            if (isTtsSpeaking) {
                tts?.stop()
                isTtsSpeaking = false
            }
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

    fun speakText(text: String, pitch: Float = 1.0f, speechRate: Float = 1.0f, onDone: (() -> Unit)? = null) {
        if (text.isBlank()) return
        Log.i("AudioOutputManager", "Speaking aloud via TTS: $text")
        try {
            if (!isTtsReady || tts == null) {
                initTts()
            }
            tts?.setPitch(pitch)
            tts?.setSpeechRate(speechRate)
            isTtsSpeaking = true
            val utteranceId = "maya_speech_${System.currentTimeMillis()}"
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        } catch (e: Exception) {
            Log.e("AudioOutputManager", "TTS speak failed", e)
        }
    }

    fun isSpeaking(): Boolean = isTtsSpeaking || isPlaying

    fun flush() {
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.play()
            tts?.stop()
            isTtsSpeaking = false
            Log.d("AudioOutputManager", "Playback and TTS flushed immediately (barge-in)")
        } catch (e: Exception) {
            Log.w("AudioOutputManager", "Error flushing playback", e)
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

        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.w("AudioOutputManager", "Error releasing TTS", e)
        }
        tts = null
        isTtsReady = false
        isTtsSpeaking = false
    }
}
