package com.maya.android.app.ai

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

data class GeminiVoice(
    val name: String,
    val styleTag: String,
    val gender: String,
    val pitch: Float = 1.0f,
    val speechRate: Float = 1.0f,
    val description: String
)

object VoiceDirectory {
    val ALL_VOICES = listOf(
        GeminiVoice("Charon", "Authoritative & Deep", "Deep Male", 0.85f, 0.98f, "Calm, commanding presence. Default for Jarvis."),
        GeminiVoice("Sulafat", "Warm & Loving", "Gentle Female", 1.25f, 1.02f, "Soft, playful, intimate. Default for Girlfriend."),
        GeminiVoice("Zephyr", "Breezy & Bright", "Female", 1.20f, 1.05f, "Upbeat, energetic and crisp."),
        GeminiVoice("Puck", "Playful & Mischievous", "Male", 1.05f, 1.10f, "Youthful, witty and dynamic."),
        GeminiVoice("Kore", "Serene & Gentle", "Female", 1.15f, 0.95f, "Relaxing, empathetic and smooth."),
        GeminiVoice("Fenrir", "Strong & Resonant", "Male", 0.80f, 0.92f, "Bold, deep cinematic tone."),
        GeminiVoice("Leda", "Sophisticated & Polished", "Female", 1.10f, 1.00f, "Articulate and professional."),
        GeminiVoice("Orus", "Warm & Grounded", "Male", 0.90f, 1.00f, "Trustworthy and conversational."),
        GeminiVoice("Aoede", "Melodic & Elegant", "Female", 1.25f, 0.98f, "Expressive and poetic cadences."),
        GeminiVoice("Callirrhoe", "Cheerful & Friendly", "Female", 1.30f, 1.08f, "Sunny and engaging."),
        GeminiVoice("Autonoe", "Empathetic & Soothing", "Female", 1.12f, 0.94f, "Calming therapeutic cadence."),
        GeminiVoice("Enceladus", "Technical & Precise", "Male", 0.92f, 1.05f, "Analytical, crisp diction."),
        GeminiVoice("Iapetus", "Mature & Wise", "Male", 0.85f, 0.90f, "Gravelly and reassuring."),
        GeminiVoice("Umbriel", "Subtle & Mysterious", "Neutral", 0.95f, 0.95f, "Reserved, smooth and quiet."),
        GeminiVoice("Algieba", "Bright & Direct", "Female", 1.18f, 1.02f, "Clear and proactive."),
        GeminiVoice("Despina", "Playful & Vibrant", "Female", 1.35f, 1.12f, "Spirited and expressive."),
        GeminiVoice("Erinome", "Composed & Soft", "Female", 1.08f, 0.96f, "Tender, quiet warmth."),
        GeminiVoice("Algenib", "Direct & Urgent", "Male", 0.95f, 1.10f, "Action-oriented and fast."),
        GeminiVoice("Rasalgethi", "Rich & Resonant", "Male", 0.78f, 0.94f, "Deep baritone resonance."),
        GeminiVoice("Laomedeia", "Airy & Pleasant", "Female", 1.28f, 1.00f, "Lighthearted and casual."),
        GeminiVoice("Achernar", "Steadfast & Confident", "Male", 0.88f, 1.02f, "Assertive and capable."),
        GeminiVoice("Alnilam", "Astral & Futuristic", "Neutral", 1.00f, 1.00f, "Balanced AI cadence."),
        GeminiVoice("Schedar", "Royal & Elegant", "Female", 1.15f, 0.98f, "Noble and articulate."),
        GeminiVoice("Gacrux", "Grounded & Reliable", "Male", 0.86f, 0.96f, "Sturdy and reassuring."),
        GeminiVoice("Pulcherrima", "Vibrant & Sweet", "Female", 1.32f, 1.04f, "Enthusiastic and joyful."),
        GeminiVoice("Achird", "Gentle & Attentive", "Male", 0.94f, 0.98f, "Kind-hearted and focused."),
        GeminiVoice("Zubenelgenubi", "Deep & Classic", "Male", 0.82f, 0.90f, "Classic narrator texture."),
        GeminiVoice("Vindemiatrix", "Expressive & Warm", "Female", 1.22f, 1.02f, "Friendly storytelling style."),
        GeminiVoice("Sadachbia", "Mellow & Relaxed", "Male", 0.90f, 0.94f, "Laid-back and easygoing."),
        GeminiVoice("Sadaltager", "Crisp & Professional", "Neutral", 1.02f, 1.04f, "Modern assistant tone.")
    )

    fun findVoice(name: String): GeminiVoice {
        return ALL_VOICES.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: ALL_VOICES[0]
    }
}

class VoicePreviewPlayer(context: Context) {
    private var tts: TextToSpeech? = null
    private var isReady = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.ENGLISH
                isReady = true
            }
        }
    }

    fun previewVoice(voice: GeminiVoice, sampleText: String = "Hello! I am Maya, your personal AI assistant.") {
        if (!isReady || tts == null) return
        tts?.stop()
        tts?.setPitch(voice.pitch)
        tts?.setSpeechRate(voice.speechRate)
        tts?.speak(sampleText, TextToSpeech.QUEUE_FLUSH, null, "voice_preview_${voice.name}")
    }

    fun stop() {
        tts?.stop()
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }
}
